# Car Detection, Tracking and Model Recognition

This project is supervised by [Prof. Richard Sinnott](https://cis.unimelb.edu.au/people/rsinnott) for Computing Project ([COMP90055 2019](https://handbook.unimelb.edu.au/2019/subjects/comp90055), [University of Melbourne](https://www.unimelb.edu.au/)).

## Description

This project implements an Android application for Car detection, tracking, and Model recognition based on deep learning model. For example, if one video is uploaded to the application, we need to recognise all the cars in the video.

**Car Detection -> Tracking -> Car Model Recognition**

**Assessment**

| Grading Parts | Percentage |
|  ----  | ----  |
| Source Code  | **45%** |
| Final Report | **45%** |
| Presentation | **10%** |.

## Team Members
[Xiaokang Zhang](https://github.com/zhkyle0903)

[Weizhuo Zhang](https://github.com/Weizhuo-Zhang)

[Zisen Zhou](https://github.com/toponeson)

## Dataset
###[Stanford Cars Dataset](https://www.kaggle.com/jessicali9530/stanford-cars-dataset)
#### cars_annos.mat -> annotations
| relative_im_path     | bbox_x1 | bbox_y1 | bbox_x2 | bbox_y2 | class | test  |
| -------------------- | ------- | ------- | ------- | ------- | ----- | ----- |
| 'car_ims/000001.jpg' |  112    |  7	     |  853    |	717    |	1    | false |
| 'car_ims/000163.jpg' |  131	   |  124	   |  537	   |  329	   |  3    | false |
| 'car_ims/000690.jpg' |  8	     |  33	   |  195	   |  119	   |  9    | false |
| ...                  |  ...	   |  ...	   |  ...	   |  ...	   |  ...  | ...   |

#### cars_annos.mat -> class_names (1x196)
| 'AM General Hummer SUV 2000' | 'Acura RL Sedan 2012' | 'Acura TL Sedan 2012' | ... |
| ---------------------------- | --------------------- | --------------------- | --- |

## Stages and TODO check list
### First Stage
#### Android Application 
- [ ] **Application Prototype**: the Android application should have a button for uploading the videos and a window show the output video.

#### Deep Learning Model
- [ ] **Car Detection Model (one car)**: detect one car in a input image.
- [ ] **Car Model Recognition Model**: recognise the model of the car, the input should be the image croped by **Car Detection Model (one car)**

#### Data Collection
- [ ] **Crawler**: collect the data continuesly

### Second Stage
#### Android Application
- [ ] **Beautify the app**: try to make the app more beautiful.
- [ ] **Use C++ to speed up**: try to use OpenCV C++ to speed up image processing or video processing. Running in the JNI(Java Native Interface).
- [ ] **Camera Process**: Use camera to capture the video stream and process real-time.

#### Deep Learning Model
- [ ] **Car Detection Model(multi-cars)**: detect all the cars in a input image.
- [ ] **Overlap cars**: detect the car hiden behind a obstacle.
- [ ] **Car tracking**: record the ID and position of previous frame for tracking the car, smooth the output.
- [ ] **Run model locally**: Real-time processing. At least 30 frames = 33ms, ideally 60 frames = 16ms.
- [ ] **Model Quantization**: try to shrink the model size and not hurt the performance. (SNPE SDK)
- [ ] **Snapgragon GPU or NPU**: try to use SNPE to run the model in GPU or NPU


