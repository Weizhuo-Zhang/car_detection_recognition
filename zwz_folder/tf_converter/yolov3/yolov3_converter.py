import tensorflow as tf


graph_def_file = "/home/weizhuozhang/dataset/model/yolov3_coco.pb"
input_arrays   = ["input/input_data"]
output_arrays  = ["pred_sbbox/concat_2", \
                  "pred_mbbox/concat_2", \
                  "pred_lbbox/concat_2",]

# tensorflow 1.12+
converter = tf.lite.TFLiteConverter.from_frozen_graph(
          graph_def_file, input_arrays, output_arrays)

# tensorflow 1.9-1.11
# converter = tf.contrib.lite.TocoConverter.from_frozen_graph(
#          graph_def_file, input_arrays, output_arrays)

#saved_model_dir = "/home/weizhuozhang/workspace/tensorflow-yolov3/checkpoint"
#converter = tf.contrib.lite.TocoConverter.from_saved_model(saved_model_dir)

tflite_model = converter.convert()
model_name = "converted_yolov3_model"
open("{0}.tflite".format(model_name), "wb").write(tflite_model)

#converter.optimizations = [tf.lite.Optimize.OPTIMIZE_FOR_SIZE]
converter.post_training_quantize=True
tflite_quantized_model = converter.convert()
open("{0}_quantized.tflite".format(model_name), "wb").write(tflite_quantized_model)
