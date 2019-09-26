import tensorflow as tf


h5_file_path = "MobileNetV2.h5"

# tensorflow 1.12+
converter = tf.lite.TFLiteConverter.from_keras_model_file(h5_file_path)

tflite_model = converter.convert()
model_name = "converted_mobilenetv2_model"
open("{0}.tflite".format(model_name), "wb").write(tflite_model)

#converter.optimizations = [tf.lite.Optimize.OPTIMIZE_FOR_SIZE]
converter.post_training_quantize=True
tflite_quantized_model = converter.convert()
open("{0}_quantized.tflite".format(model_name), "wb").write(tflite_quantized_model)
