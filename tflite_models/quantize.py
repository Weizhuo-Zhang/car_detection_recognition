import tensorflow as tf


h5_model_path = "h5models"
tflite_model_path = "tflitemodels"
model_name = "MobileNetV2"
h5_model_file = h5_model_path+"\\" + model_name + ".h5"

converter = tf.lite.TFLiteConverter.from_keras_model_file(h5_model_file)
# converter.optimizations = [tf.lite.Optimize.DEFAULT]
# converter.target_spec.supported_types = [tf.lite.constants.FLOAT16]
converter.post_training_quantize = True
tflite_model = converter.convert()
tflite_model_file = tflite_model_path +"\\" + model_name + "_quantized.tflite"
open(tflite_model_file, "wb").write(tflite_model)