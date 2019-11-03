import tensorflow as tf
import keras
import cv2
import os
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
class_list_path = os.path.join(base_path,"labels.txt")
accept_confidence = 0.5
max_video_duration = 15 # second

def vieo_process(video_path, output_path, 
                model_name = "nasnetlarge", 
                accept_confidence = 0.5,
                max_video_duration = 15):
    print("Creating YOLO object", flush=True)
    yolo = YOLO()
    print("Creating video capture", flush=True)
    video = cv2.VideoCapture(os.path.join(video_path))
    if model_name == "inceptionresnetv2":
        img_size = 299,299
    elif model_name == "nasnetlarge":
        img_size = 331,331
    elif model_name == "densenet201":
        img_size = 224,224
    elif model_name == "densenet169":
        img_size = 224,224
    model_path = os.path.join(base_path,"check_point", model_name+".h5")
    print("model path: " + str(model_path) ,flush=True)
    model = tf.keras.models.load_model(model_path)
    print("Model loaded",flush=True)
    class_list = []
    with open(class_list_path,'r') as label_list:
        for label in label_list:
            print(label.strip(),flush=True)
            class_list.append(label.strip())
    begin_time = time.time()
    frame_array = []
    try:
        _, frame = video.read()
        video_resolution = (frame.shape[1],frame.shape[0])
        print("Video resolution: "+ str(video_resolution),flush=True)
        frame_array.append(frame)
        fps = int(video.get(cv2.CAP_PROP_FPS))
        print("FPS:" +str(fps),flush=True)
        duration = int(video.get(cv2.CAP_PROP_FRAME_COUNT)/video.get(cv2.CAP_PROP_FPS))
        print("Duration:" + str(duration)+"s")
        if duration > max_video_duration:
            print("Video duration exceeded limit, process only first "+ str(max_video_duration) + " seconds",flush=True)
            duration = max_video_duration
    except:
        print("Input Error: Invalid video",flush=True)
        sys.exit()
        
    # convert video to a list of frame
    print("Reading video file",flush=True)
    frame_count = 0
    while True:
        return_value, frame = video.read()
        if return_value and frame_count <= max_video_duration*fps:
        # if return_value:
            frame_array.append(frame)
            frame_count += 1
        else:
            break
    print("Reading complete, total frame number: " + str(frame_count),flush=True)
    
    pbar = tqdm(total=len(frame_array))
    counter = 0
    total_start_time = time.time()
    for frame in frame_array:
        start_time = time.time()
        # yolo.get_car_images returns a tuple of all vehicle images and their corresponding bbox
        frame = Image.fromarray(frame,'RGB')
        vehicle_images, bboxes = yolo.get_car_images(frame)
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
        print("Drawing boxes on frame "+str(counter),flush=True)
        print("Acceptable bbox number: "+str(len(bboxes)),flush=True)
        frame = yolo.drawalllable(frame,accept_bboxes,labels)
        frame_array[counter] = frame
        end_time = time.time()
        pbar.update(1)
        print("Time cost for processing frame " + str(counter) + " : "+str(end_time - start_time),flush=True)
        counter += 1
    
    total_end_time = time.time()
    avg_time_per_frame = (total_end_time - total_start_time) / len(frame_array)
    print("Average time cost for processing a frame: " + str(avg_time_per_frame))
    print("All frames processed",flush=True)
    writer = cv2.VideoWriter(output_path,cv2.VideoWriter_fourcc(*'mp4v'),fps,video_resolution)    
    print("Writing output video",flush=True)
    for frame in frame_array:
        frame = np.array(frame)
        writer.write(frame)
    writer.release()
    print("Done writing output video",flush=True)
    total_time = time.time() - begin_time
    print("Total time cost: " + str(total_time),flush=True)
    print("Time cost per video second: "+ str(total_time/duration),flush=True)
    
    print("Writing log file")
    output_log_path = os.path.join(output_path,video_name + "_yolov3_"+model_name+".txt")
    with open(output_log_path,'w') as output_log:
        output_log.write("Video name: "+ str(video_name)+"\n")
        output_log.write("Resolution: "+str(video_resolution)+"\n")
        output_log.write("FPS: "+str(fps)+"\n")
        output_log.write("Duration: "+str(duration)+"s"+"\n")
        output_log.write("Total time cost: "+str(total_time)+"\n")
        output_log.write("Time cost per video second: " + str(total_time/duration)+"\n")
    print("Done writing log file",flush=True)
    
if __name__ == '__main__':
    video_path = Path(sys.argv[1])
    video_name = video_path.stem
    print("Video to process:" + str(video_path.name))
    video_format = video_path.suffix
    print("Name of video:" + str(video_name))
    print("Format of video:" + str(video_format))
    directory = video_path.parent
    print("Directory:" + str(directory))
    if len(sys.argv) > 1:
        model_name = sys.argv[2]
    else:
        model_name = 'nasnetlarge'
    print("Using model:" + str(model_name),flush=True)
    output_filename = video_name + "_processed_" + model_name + '.mp4'
    output_path = os.path.join(directory,output_filename)
    print("Start processing",flush=True)
    vieo_process(video_path, output_path, 
                model_name, 
                accept_confidence = 0.1,
                max_video_duration = 15)