import tensorflow as tf
import keras
import cv2
import os
# import pickle
import numpy as np
import sys
import getopt
import time
from PIL import Image
from tqdm import tqdm
from pathlib import Path
from yolo import YOLO

print("keras version:" + keras.__version__)
print("tensorflow version:" + tf.__version__)
base_path = os.path.abspath(os.path.dirname(__file__))
print("base path:" + str(base_path))
yolo_img_size = 608,608
# default_img_size = 224,224
# test_path = os.path.join(base_path,"input/Car_data/test")
# ALLOWED_EXTENSIONS = set(['png','jpg','JPG','PNG','bmp','jpeg'])
class_list_path = os.path.join(base_path,"labels.txt")
# video_path = "C:\\Users\\Kyle\\Desktop\\StudyS4\\Project\\video_dataset\\"
# video_name = "test_video1"
# output_path = "C:\\Users\\Kyle\\Desktop\\StudyS4\\Project\\video_dataset\\"
accept_confidence = 0.5
max_video_duration = 15 # second

def image_process(image_path, output_path, 
                model_name = "nasnetlarge", 
                accept_confidence = 0.5):
    print("Creating YOLO object", flush=True)
    yolo = YOLO()
    print("Creating image capture", flush=True)
    print(image_path)
    img = cv2.imread(os.path.join(image_path))
    # try:
    #     opts,args = getopt.getopt(sys.argv[1:],"m:",['model='])
    # except getopt.GetoptError:
    #     print('test.py -m <model_name>')
    # for opt, arg in opts:
    #     if opt in ('-m', '--model'):
    #         model_name = arg
    # print("Using model: "+model_name , flush=True)
    if model_name == "inceptionresnetv2":
        img_size = 299,299
    elif model_name == "nasnetlarge":
        img_size = 331,331
    elif model_name == "densenet201":
        img_size = 224,224
    elif model_name == "densenet169":
        img_size = 224,224
    # model_path = os.path.join(base_path,"check_point\\"+model_name+"\\"+model_name+".hdf5")
    model_path = os.path.join(base_path,"check_point\\"+model_name+"\\"+model_name+".h5")
    print("model path: " + str(model_path) ,flush=True)
    # output_video_path = os.path.join(output_path,video_name + "_yolov3_"+model_name+".mp4")
    model = tf.keras.models.load_model(model_path)
    print("Model loaded",flush=True)
    class_list = []
    with open(class_list_path,'r') as label_list:
        for label in label_list:
            print(label.strip(),flush=True)
            class_list.append(label.strip())
           
    img = Image.fromarray(img,'RGB') 
    vehicle_images, bboxes = yolo.get_car_images(img)
    print("vehicle found: "+str(len(vehicle_images)),flush=True)
    labels = []
    accept_bboxes = []
    for i in range(len(vehicle_images)):
        resized_image = vehicle_images[i].resize(tuple(reversed(img_size)), Image.BICUBIC)
        image_data = np.array(resized_image, dtype='float32')
        image_data /= 255.
        image_data = np.expand_dims(image_data, 0)
        result = model.predict(image_data).flatten()
        index = np.argmax(result)
        prob = result[index]
        # Remove low confidence results
        if prob < accept_confidence: continue
        class_name = class_list[index]
        label = str(class_name) + " : " + str(prob)
        labels.append(label)
        accept_bboxes.append(bboxes[i])
    print("Acceptable bbox number: "+str(len(bboxes)),flush=True)
    img = yolo.drawalllable(img,accept_bboxes,labels)
    img = np.array(img)
    cv2.imwrite(os.path.join(output_path), img)
    
if __name__ == '__main__':
    image_path = Path(sys.argv[1])
    image_name = image_path.stem
    print("Image to process:" + str(image_path.name))
    image_format = image_path.suffix
    print("Name of Image:" + str(image_name))
    print("Format of Image:" + str(image_format))
    directory = image_path.parent
    print("Directory:" + str(directory))
    model_name = sys.argv[2]
    print("Using model:" + str(model_name),flush=True)
    output_filename = image_name + "_processed_" + model_name + '.png'
    output_path = os.path.join(directory,output_filename)
    print("Start processing",flush=True)
    image_process(image_path, output_path, 
                model_name, 
                accept_confidence = 0.1)