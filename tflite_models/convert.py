import tensorflow as tf
import keras
import os
import sys

h5_model_path =  os.path.join('h5models', 'checkpoint') 
tflite_model_path = "tflitemodels"
model_name = sys.argv[1]

h5_model_file = os.path.join(h5_model_path, model_name + ".h5")
converter = tf.lite.TFLiteConverter.from_keras_model_file(h5_model_file)
tflite_model = converter.convert()
tflite_model_file = os.path.join(tflite_model_path, model_name + ".tflite")
open(tflite_model_file, "wb").write(tflite_model)