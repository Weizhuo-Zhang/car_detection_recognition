import os, random, shutil
 
def copyFile(datasetDir):

    for clss in os.listdir(datasetDir):
        print("clss" + str(clss))
        clsDir = os.path.join(datasetDir, clss)
        pathDir = os.listdir(clsDir)
        print("pathDir" + str(pathDir))
        # pathDir = os.path.join(datasetDir, clsDir, pathDir)
        sample = random.sample(pathDir, 10)
        clsTarDir = os.path.join(tarDir, clss)
        print("clsTarDir" + clsTarDir)
        for name in sample:
            shutil.copyfile(os.path.join(clsDir,name), os.path.join(clsTarDir,name))
            os.remove(os.path.join(clsDir, name))
  

if __name__ == '__main__':
	datasetDir = "train"
	tarDir = 'validation'
	copyFile(datasetDir)