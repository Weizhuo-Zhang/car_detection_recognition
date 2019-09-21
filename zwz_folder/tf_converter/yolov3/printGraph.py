import tensorflow as tf
sess = tf.Session();
f = open("/home/weizhuozhang/dataset/model/yolov3_coco.pb", "rb")
graph_def = tf.GraphDef()
graph_def.ParseFromString(f.read())
print(graph_def)
