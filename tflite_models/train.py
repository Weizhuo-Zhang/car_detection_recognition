import tensorflow as tf
import keras
import sys
import os
import json
import numpy as np

BATCH_SIZE = 128
epochs = 50
model_info = "model_info.json"
h5_model_path = "h5models"
tflite_model_path = "tflitemodels"
image_data_path = "train"

def train(model_name,
          rotation_range = 30,
          zoom_range = 0.2,
          shift_factor = 0.2,
          brightness_range = [0.5,1.0],
          horizontal_flip = True,
          vertical_flip = False,
          fine_tune_at = 100):
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
                                                              validation_split = 0.1)

    train_generator = datagen.flow_from_directory(
    image_data_path,
    target_size = (IMAGE_SIZE, IMAGE_SIZE),
    batch_size = BATCH_SIZE,
    subset = "training")

    val_generator = datagen.flow_from_directory(
    image_data_path,
    target_size = (IMAGE_SIZE, IMAGE_SIZE),
    batch_size = BATCH_SIZE,
    subset = "validation")
    
    model = tf.keras.models.load_model(h5_model_path + "\\" + model_name + ".h5", compile = False)
    model.summary()
    
    for layer in model.layers[0].layers[fine_tune_at:]:
        layer.trainable =  True
        
    model.summary()
    
    model.compile(loss ='categorical_crossentropy',
              optimizer = tf.keras.optimizers.Adam(1e-5),
              metrics = ['accuracy'])
    model.summary()
    
    csv_logger = tf.keras.callbacks.CSVLogger(model_name + 'log.csv', append=True, separator=';')
    checkpoint = tf.keras.callbacks.ModelCheckpoint(h5_model_path + "\\checkpoint\\" + model_name + ".h5", monitor = 'acc', verbose = 1, save_best_only = False, mode = 'max')
    callbacks_list = [csv_logger, checkpoint]
    
    history_fine = model.fit_generator(train_generator, 
                         epochs = epochs,
                         validation_data = val_generator,
                         callbacks = callbacks_list)
    
    # save models
    h5_model_file = h5_model_path+"\\" + model_name + ".h5"
    model.save(h5_model_file)
    print("H5 model saved")
    
    # Convert to TensorFlow Lite model.
    converter = tf.lite.TFLiteConverter.from_keras_model_file(h5_model_file)
    tflite_model = converter.convert()
    tflite_model_file = tflite_model_path +"\\" + model_name + ".tflite"
    open(tflite_model_file, "wb").write(tflite_model)
    
if __name__ == '__main__':
    train("MobileNetV2")