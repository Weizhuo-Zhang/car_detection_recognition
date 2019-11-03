
import tensorflow as tf
import keras
import numpy as np
import pickle
from PIL import Image
import os
import sys
import json
import logging
import pandas as pd
import matplotlib.pyplot as plt
import cv2

class_list_path = 'labels.txt'
basepath = base_path = os.path.abspath(os.path.dirname(__file__))
model_info = 'model_info.json'

if __name__ == '__main__':
    model_name = sys.argv[1]
    k = sys.argv[2]
    k = int(k)
    for handler in logging.root.handlers[:]:
        logging.root.removeHandler(handler)
    logging.basicConfig(filename = model_name + '_top' + str(k) + '_test_log.txt', filemode = 'w', format = '%(message)s', level = logging.INFO)
    logging.info('Using model: ' + str(model_name))
    class_list = []
    with open(class_list_path,'r') as label_list:
        for label in label_list:
            print(label.strip(),flush=True)
            class_list.append(label.strip())
    print("Class list loaded")
    
    with open(model_info) as info:
        data = json.load(info)
        # print(data[model_name])
        IMAGE_SIZE = int(data[model_name])
    model_path = os.path.join('h5models', model_name+'.h5')
    print(model_path)
    model = tf.keras.models.load_model(model_path)
    print("Load complete")
    
    df = pd.DataFrame(columns=['class', 'top_1', 'top_'+str(k)])
    
    val_dir = 'test'
    total_top1_counter = 0
    total_topk_counter = 0
    img_counter = 0
    for clss in os.listdir(val_dir):
        clss = clss.strip()
        clss_top1_counter = 0
        clss_topk_counter = 0
        print("class: " + str(clss))
        logging.info("class: " + str(clss))
        clss_dir = os.path.join(val_dir, clss)
        for img_dir in os.listdir(clss_dir):
            img_dir = os.path.join(clss_dir,img_dir)
            img = cv2.imread(img_dir)
            img = cv2.cvtColor(img, cv2.COLOR_BGRA2BGR)
            resized_image = cv2.resize(img, dsize = (IMAGE_SIZE,IMAGE_SIZE), interpolation = cv2.INTER_LINEAR)
            image_data = np.array(resized_image, dtype='float32')
            image_data /= 255.
            # print(image_data.shape)
            if image_data.shape != (IMAGE_SIZE, IMAGE_SIZE, 3):
                print(image_data.shape)
                image_data = np.expand_dims(image_data,axis=2)
                image_data = np.concatenate((image_data, image_data, image_data), axis=-1)
                # print("Shape after converting: "+str(image_data.shape))
            image_data = np.expand_dims(image_data, 0)
            result = model.predict(image_data).flatten()
            sorted_index = np.argsort(-result)
            # print("sorted_index:" + str(sorted_index))
            top_k = sorted_index[:k]
            top_k_predictions = []
            for index in top_k:
                prediction = class_list[index]
                top_k_predictions.append(prediction)
            print("Top "+str(k)+" predictions: " + str(top_k_predictions))
            logging.info("Top "+str(k)+" predictions: " + str(top_k_predictions))
            if clss == top_k_predictions[0]: 
                total_top1_counter += 1
                clss_top1_counter += 1
            if clss in top_k_predictions: 
                total_topk_counter +=1
                clss_topk_counter += 1        
            img_counter += 1
        clss_top1_acc = clss_top1_counter/len(os.listdir(clss_dir))
        clss_topk_acc = clss_topk_counter/len(os.listdir(clss_dir))
        print("Top 1 Accuracy:" + str(clss_top1_acc))
        logging.info('Top 1 Accuracy:' + str(clss_top1_acc))
        print("Top " + str(k) + " Accuracy:" + str(clss_topk_acc))
        logging.info("Top " + str(k) + " Accuracy:" + str(clss_topk_acc))
        df = df.append({'class': clss, 'top_1': clss_top1_acc, 'top_'+str(k):clss_topk_acc}, ignore_index=True)
    print(model_name)
    print("Average Top 1 Accuracy: " + str(df['top_1'].mean()))
    logging.info("Average Top 1 Accuracy: " + str(df['top_1'].mean()))
    print("Average Top " + str(k) + " Accuracy: " + str(df['top_'+str(k)].mean()))
    logging.info("Average Top " + str(k) + " Accuracy" + str(df['top_'+str(k)].mean()))
    test_result = os.path.join('test_results', model_name + '_test_result.csv')
    df.to_csv(test_result)
    