import tensorflow as tf
import keras
import sys
import os
import json
import numpy as np
import argparse

from tensorflow.compat.v1 import ConfigProto
from tensorflow.compat.v1 import InteractiveSession

config = ConfigProto()
config.gpu_options.allow_growth = True
session = InteractiveSession(config=config)

model_info = "model_info.json"
h5_model_path = "h5models"
tflite_model_path = "tflitemodels"
image_data_path = "train"

def train(model_name,
          train_epochs = 10,
          batch_size = 8,
          learning_rate = 1e-7,
          rotation_range = 20,
          zoom_range = 0.3,
          shift_factor = 0.2,
          brightness_range = [0.5,1.0],
          horizontal_flip = True,
          vertical_flip = False,
          fine_tune_at = 0
          ):
    
    print("Training model: " + model_name)
    print("Epochs: " + str(train_epochs))
    print("Batch size: " + str(batch_size))
    print("Learning rate: " + str(learning_rate))
    print("Rotation range: " + str(rotation_range))
    print("Zoom range: " + str(zoom_range))
    print("Shift factor: " + str(shift_factor))
    print("brightness range: " + str(brightness_range))
    print("Horizontal flip: " + str(horizontal_flip))
    with open(model_info) as info:
        data = json.load(info)
        # print(data[model_name])
        IMAGE_SIZE = int(data[model_name])
    IMG_SHAPE = (IMAGE_SIZE, IMAGE_SIZE, 3)
    datagen = tf.keras.preprocessing.image.ImageDataGenerator(rescale = 1./255, 
                                                              rotation_range = rotation_range,
                                                              width_shift_range = [-shift_factor, shift_factor],
                                                              height_shift_range = [-shift_factor, shift_factor],
                                                              brightness_range = brightness_range,
                                                              zoom_range = zoom_range,
                                                              horizontal_flip = horizontal_flip,
                                                              vertical_flip = vertical_flip,
                                                              validation_split = 0.05)

    train_generator = datagen.flow_from_directory(
                                        image_data_path,
                                        target_size = (IMAGE_SIZE, IMAGE_SIZE),
                                        batch_size = batch_size,
                                        subset = "training")

    val_generator = datagen.flow_from_directory(
                                        image_data_path,
                                        target_size = (IMAGE_SIZE, IMAGE_SIZE),
                                        batch_size = batch_size,
                                        subset = "validation")
    
    model_path = os.path.join(h5_model_path, model_name + ".h5")
    model = tf.keras.models.load_model(model_path, compile = False)
    model.summary()
    
    print(len(model.layers[0].layers))
    for layer in model.layers[0].layers[0:fine_tune_at]:
        layer.trainable =  False
    for layer in model.layers[0].layers[fine_tune_at:]:
        layer.trainable =  True
        
    model.summary()
    
    model.compile(loss ='categorical_crossentropy',
              optimizer = tf.keras.optimizers.Adam(learning_rate),
              metrics = ['accuracy'])
    
    csv_logger = tf.keras.callbacks.CSVLogger(model_name + 'log.csv', append=True, separator=';')
    checkpoint_path = os.path.join(h5_model_path, "checkpoint", model_name + ".h5")
    checkpoint = tf.keras.callbacks.ModelCheckpoint(checkpoint_path, monitor = 'val_acc', verbose = 1, save_best_only = True, mode = 'max')
    callbacks_list = [csv_logger, checkpoint]
    
    history_fine = model.fit_generator(train_generator, 
                         epochs = train_epochs,
                         validation_data = val_generator,
                         callbacks = callbacks_list)
    
    # save models
    # h5_model_file = h5_model_path+"\\" + model_name + ".h5"
    h5_model_file = os.path.join(h5_model_path, model_name + ".h5")
    model.save(h5_model_file)
    print("H5 model saved")
    
    # Convert to TensorFlow Lite model.
    converter = tf.lite.TFLiteConverter.from_keras_model_file(h5_model_file)
    # converter.optimizations = [tf.lite.Optimize.DEFAULT]
    # converter.target_spec.supported_types = [tf.lite.constants.FLOAT16]
    tflite_model = converter.convert()
    tflite_model_file = os.path.join(tflite_model_path, model_name + ".tflite")
    # tflite_model_file = tflite_model_path +"\\" + model_name + ".tflite"
    open(tflite_model_file, "wb").write(tflite_model)
    
if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-m', '--model', action='store', dest='model',help='model selection', default='densenet169', required=True)
    parser.add_argument('-lr', '--learning_rate', action='store', type=float, dest='lr',help='learning rate', default=1e-5)
    parser.add_argument('-ep', '--epochs', action='store', type=int, dest='epochs',help='number of epochs to train', default=10)
    parser.add_argument('-batch', '--batch_size', action='store', type=int, dest='batch_size',help='batch size', default=8)
    parser.add_argument('-rotation','--rotation_range', action='store', type=int, dest='rotation',help='rotation range for data augmentation', default=20)
    parser.add_argument('-zoom','--zoom_range', action='store', type=float, dest='zoom',help='zoom range for data augmentation', default=0.3)
    parser.add_argument('-shift','--shift_factor', action='store', type=float, dest='shift',help='shift ratio for data augmentation', default=0.2)
    parser.add_argument('-brightness','--brightness_range', action='store', nargs='+', type=float, dest='brightness',help='brightness range for data augmentation, need two float numbers', default=[0.5, 1.0])
    parser.add_argument('-no_hori_flip','--no_horizontal_flip', action='store_false', dest='hori_flip',help='horizontal flip for data augmentation', default=True)
    args = parser.parse_args()
    train(args.model, train_epochs = args.epochs, batch_size = args.batch_size, learning_rate = args.lr, rotation_range = args.rotation,\
        zoom_range = args.zoom, shift_factor = args.shift, brightness_range = args.brightness, horizontal_flip = args.hori_flip)