import tensorflow as tf
import keras

h5_model_path = "h5models\\checkpoint"
tflite_model_path = "tflitemodels"
model_name = "MobileNetV2"

h5_model_file = h5_model_path+"\\" + model_name + ".h5"
converter = tf.lite.TFLiteConverter.from_keras_model_file(h5_model_file)
tflite_model = converter.convert()
tflite_model_file = tflite_model_path +"\\" + model_name + "11.tflite"
open(tflite_model_file, "wb").write(tflite_model)