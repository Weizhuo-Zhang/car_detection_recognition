import tensorflow as tf
import keras

import sys
import os
import logging
import json
import numpy as np
import matplotlib.pyplot as plt

BATCH_SIZE = 64
transfer_epochs = 100
fine_tune_epochs = 10
fine_tune_start_layer = 100
model_info = "model_info.json"
image_data_path = "train"
# val_data_path = "validation"
h5_model_path = "h5models"
tflite_model_path = "tflitemodels"
temp_path = "temp"

def transfer_learning(model_name, transfer_epochs = transfer_epochs, fine_tune_epochs = fine_tune_epochs):
    # for handler in logging.root.handlers[:]:
    #     logging.root.removeHandler(handler)
    # logname = os.path.join(os.path.dirname(os.path.realpath(__file__)), model_name + "_transfer.log")
    # logging.basicConfig(filename = logname, 
    #                     format = '%(asctime)s - %(levelname)s - %(message)s', 
    #                     level = logging.INFO)
    with open(model_info) as info:
        data = json.load(info)
        # print(data[model_name])
        IMAGE_SIZE = int(data[model_name])
    # logging.info("Using "+ model_name)
    # logging.info("Input image size: " + str(IMAGE_SIZE))
    IMG_SHAPE = (IMAGE_SIZE, IMAGE_SIZE, 3)
    
    datagen = tf.keras.preprocessing.image.ImageDataGenerator(rescale = 1./255, validation_split = 0.1)

    train_generator = datagen.flow_from_directory(
        image_data_path,
        target_size = (IMAGE_SIZE, IMAGE_SIZE),
        batch_size = BATCH_SIZE,
        subset = "training")
    # logging.info("Training data loaded")
    
    val_generator = datagen.flow_from_directory(
        image_data_path,
        target_size = (IMAGE_SIZE, IMAGE_SIZE),
        batch_size = BATCH_SIZE,
        subset = "validation")
    # logging.info("Validation data loaded")
    
    for image_batch, label_batch in train_generator:
        break
    # logging.info("Image batch shape: " + str(image_batch.shape))
    # logging.info("Label batch shape: " + str(label_batch.shape))
    
    labels = '\n'.join(sorted(train_generator.class_indices.keys()))

    with open('labels.txt', 'w') as label_file:
        label_file.write(labels)
    
    base_model = eval("tf.keras.applications." + model_name + "(input_shape=IMG_SHAPE,include_top=False, weights='imagenet')")
    # logging.info("Base model created")
    base_model.trainable = False
    
    model = tf.keras.Sequential([
                base_model,
                tf.keras.layers.Conv2D(32, 3, activation = 'relu'),
                tf.keras.layers.Dropout(0.2),
                tf.keras.layers.GlobalAveragePooling2D(),
                tf.keras.layers.Dense(label_batch.shape[1], activation = 'softmax')
                ])
    
    model.compile(optimizer = tf.keras.optimizers.Adam(), 
              loss = 'categorical_crossentropy', 
              metrics = ['accuracy'])
    model.summary()
    # logging.info('Number of trainable variables = {}'.format(len(model.trainable_variables)))
    csv_logger = tf.keras.callbacks.CSVLogger(model_name + 'log.csv', append=True, separator=';')
    transfer_history = model.fit_generator(train_generator, 
                        epochs = transfer_epochs, 
                        validation_data = val_generator,
                        callbacks = [csv_logger])
    
    # logging.info("Transfer training complete, " + str(transfer_epochs) + " epochs trained")
    
    # plot_history(transfer_history)
    
    # Fine tuning
    # logging.info("Number of layers in the base model: ", len(base_model.layers))
    fine_tune_at = fine_tune_start_layer
    # Freeze all the layers before the `fine_tune_at` layer
    for layer in base_model.layers[:fine_tune_at]:
        layer.trainable =  False
    
    model.compile(loss ='categorical_crossentropy',
              optimizer = tf.keras.optimizers.Adam(1e-5),
              metrics = ['accuracy'])
    model.summary()
    # logging.info('Number of trainable variables = {}'.format(len(model.trainable_variables)))
    
    history_fine = model.fit_generator(train_generator, 
                         epochs = fine_tune_epochs,
                         validation_data = val_generator,
                         callbacks = [csv_logger])
    
    # plot_history(history_fine)
    
    # save models
    h5_model_file = h5_model_path+"\\" + model_name + ".h5"
    model.save(h5_model_file)
    # logging.info("H5 model saved")
    print("H5 model saved")
    
    # Convert to TensorFlow Lite model.
    converter = tf.lite.TFLiteConverter.from_keras_model_file(h5_model_file)
    tflite_model = converter.convert()
    tflite_model_file = tflite_model_path +"\\" + model_name + ".tflite"
    open(tflite_model_file, "wb").write(tflite_model)
    
if __name__ == '__main__':
    transfer_learning("MobileNetV2")