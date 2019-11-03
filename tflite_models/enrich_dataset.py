import os
class_list = []
with open('labels.txt') as class_file:
    for line in class_file:
        class_list.append(line.strip()) 
for clss in class_list:
    clss = clss.strip()
    clss = '\"' + clss + '\"'
    os.system("googleimagesdownload -k "+clss+" -l 10 -o test")