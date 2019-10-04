/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.CarClassifier;
import org.tensorflow.lite.examples.detection.tflite.ObjectDetectionClassifier;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;
import org.tensorflow.lite.examples.detection.tflite.CarClassifier.Device;
import org.tensorflow.lite.examples.detection.tflite.CarClassifier.Model;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();

  // Configuration values for the prepackaged SSD model.
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED = true;
  private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
  private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
  //private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco.names";
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
  private static final boolean MAINTAIN_ASPECT = false;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;
  private static final int MAXIMUM_NUM_CAR_DETECT = 2;
  OverlayView trackingOverlay;
  private Integer sensorOrientation;

  private ObjectDetectionClassifier detector;
  private CarClassifier carClassifier;

  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private BorderedText borderedText;

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    tracker = new MultiBoxTracker(this);

    int cropSize = TF_OD_API_INPUT_SIZE;

    try {
      detector =
          TFLiteObjectDetectionAPIModel.create(
              getAssets(),
              TF_OD_API_MODEL_FILE,
              TF_OD_API_LABELS_FILE,
              TF_OD_API_INPUT_SIZE,
              TF_OD_API_IS_QUANTIZED);
      cropSize = TF_OD_API_INPUT_SIZE;
    } catch (final IOException e) {
      e.printStackTrace();
      LOGGER.e(e, "Exception initializing classifier!");
      Toast toast =
          Toast.makeText(
              getApplicationContext(), "ObjectDetectionClassifier could not be initialized", Toast.LENGTH_SHORT);
      toast.show();
      finish();
    }

    recreateClassifier(getModel(), getDevice(), getNumThreads());
    if (carClassifier == null) {
      LOGGER.e("No carClassifier on preview!");
      return;
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            tracker.draw(canvas);
            if (isDebug()) {
              tracker.drawDebug(canvas);
            }
          }
        });

    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
  }

  private int getPositiveValue(float value) {
    if (value < 0) {
      return 0;
    } else {
      return (int)value;
    }
  }

  private int getBoundedValue(float value, int bound) {
    if (value > bound) {
      return bound;
    } else {
      return (int)value;
    }
  }

  @Override
  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }

    runInBackground(
        new Runnable() {
          @Override
          public void run() {
            LOGGER.i("Running detection on image " + currTimestamp);
            final long startTime = SystemClock.uptimeMillis();
            List<ObjectDetectionClassifier.Recognition> results = detector.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas canvas = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(2.0f);

            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            switch (MODE) {
              case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
            }


            //PriorityQueue<ObjectDetectionClassifier.Recognition> sorted_results = new PriorityQueue<>();
            PriorityQueue<ObjectDetectionClassifier.Recognition> sorted_results =
                    new PriorityQueue<ObjectDetectionClassifier.Recognition>(20, new Comparator<ObjectDetectionClassifier.Recognition>(){
              public int compare(ObjectDetectionClassifier.Recognition o1, ObjectDetectionClassifier.Recognition o2){
                if (o2.getConfidence() > o1.getConfidence()) {
                  return 1;
                } else {
                  return -1;
                }
              }
            });

            for (ObjectDetectionClassifier.Recognition result : new ArrayList<>(results)) {
              if (result.getLocation() != null &&
                      result.getConfidence() >= minimumConfidence &&
                      result.getTitle().equals("car")) {
                sorted_results.add(result);
              } else {
                results.remove(result);
              }
            }

            //for (ObjectDetectionClassifier.Recognition result : sorted_results) {
            //  LOGGER.i("confidence:" + result.getConfidence());
            //}

            if (results.size() > MAXIMUM_NUM_CAR_DETECT) {
              for (int i = 0; i < MAXIMUM_NUM_CAR_DETECT; i++) {
                ObjectDetectionClassifier.Recognition recognition = sorted_results.poll();
              }
              while (!sorted_results.isEmpty()) {
                ObjectDetectionClassifier.Recognition recognition = sorted_results.poll();
                results.remove(recognition);
              }
            }

            //Iterator<ObjectDetectionClassifier.Recognition> cleaned_results = results.iterator();
            //while (cleaned_results.hasNext()){
            //  ObjectDetectionClassifier.Recognition result = cleaned_results.next();
            //  if (result.getConfidence() >= minimumConfidence && result.getTitle().equals("car")) {
            //      // Do nothing
            //  } else {
            //    cleaned_results.remove(result);
            //  }
            //}

            // Car Classifiction
            final long recognizeStartTime = SystemClock.uptimeMillis();
            for (ObjectDetectionClassifier.Recognition result : results) {
              final RectF location = result.getLocation();
              int x = getPositiveValue(location.left);
              int y = getPositiveValue(location.top);
              int width = getBoundedValue(location.right - x, cropCopyBitmap.getWidth() - x);
              int height = getBoundedValue(location.bottom - location.top, cropCopyBitmap.getHeight() - y);
              //LOGGER.i("zwz log left: " + location.left);
              //LOGGER.i("zwz log right: " + location.right);
              //LOGGER.i("zwz log top: " + location.top);
              //LOGGER.i("zwz log bottom: " + location.bottom);
              //LOGGER.i("zwz log x: " + x);
              //LOGGER.i("zwz log y: " + y);
              //LOGGER.i("zwz log width: " + width);
              //LOGGER.i("zwz log height: " + height);
              //LOGGER.i("zwz log bitmap width: " + croppedBitmap.getWidth());
              //LOGGER.i("zwz log bitmap height: " + croppedBitmap.getHeight());
              final Bitmap resultBitmap = Bitmap.createBitmap(croppedBitmap, x, y, width, height);
              final Bitmap scaledResultBitmap =
                      resultBitmap.createScaledBitmap(resultBitmap,
                                                      carClassifier.getImageSizeX(),
                                                      carClassifier.getImageSizeY(),
                                                      false);
              final CarClassifier.Recognition recognitionResult = carClassifier.recognizeImage(scaledResultBitmap).get(0);
              result.setTitle(recognitionResult.getTitle());
              result.setConfidence(recognitionResult.getConfidence());
            }
            long recognizeProcessingTimeMs = SystemClock.uptimeMillis() - recognizeStartTime;

            final List<ObjectDetectionClassifier.Recognition> mappedRecognitions =
                new LinkedList<ObjectDetectionClassifier.Recognition>();

            //for (;cleaned_results.hasNext();) {
            //  final ObjectDetectionClassifier.Recognition result = cleaned_results.next();
            //  LOGGER.i("get result: " + result.getTitle());

            for (final ObjectDetectionClassifier.Recognition result : results) {
              final RectF location = result.getLocation();
              if (location != null && result.getConfidence() >= minimumConfidence) {
                canvas.drawRect(location, paint);

                cropToFrameTransform.mapRect(location);

                result.setLocation(location);
                mappedRecognitions.add(result);
              }
            }

            tracker.trackResults(mappedRecognitions, currTimestamp);
            trackingOverlay.postInvalidate();

            computingDetection = false;

            runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    showFrameInfo(previewWidth + "x" + previewHeight);
                    showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                    showInference(lastProcessingTimeMs + "ms");
                    showRecognitionInference(recognizeProcessingTimeMs + "ms");
                  }
                });
          }
        });
  }

  @Override
  protected int getLayoutId() {
    return R.layout.camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }

  @Override
  protected void setUseNNAPI(final boolean isChecked) {
    runInBackground(() -> detector.setUseNNAPI(isChecked));
  }

  @Override
  protected void onInferenceConfigurationChanged() {
    if (croppedBitmap == null) {
      // Defer creation until we're getting camera frames.
      return;
    }
    final Device device = getDevice();
    final Model model = getModel();
    final int numThreads = getNumThreads();
    runInBackground(() -> recreateClassifier(model, device, numThreads));
    runInBackground(() -> detector.setNumThreads(numThreads));
  }

  private void recreateClassifier(Model model, Device device, int numThreads) {
    if (carClassifier != null) {
      LOGGER.d("Closing carClassifier.");
      carClassifier.close();
      carClassifier = null;
    }
    if (device == Device.GPU && model == Model.QUANTIZED) {
      LOGGER.d("Not creating carClassifier: GPU doesn't support quantized models.");
      runOnUiThread(
              () -> {
                Toast.makeText(this, "GPU does not yet supported quantized models.", Toast.LENGTH_LONG)
                        .show();
              });
      return;
    }
    try {
      LOGGER.d(
              "Creating carClassifier (model=%s, device=%s, numThreads=%d)", model, device, numThreads);
      carClassifier = CarClassifier.create(this, model, device, numThreads);
    } catch (IOException e) {
      LOGGER.e(e, "Failed to create carClassifier.");
    }
  }
}
