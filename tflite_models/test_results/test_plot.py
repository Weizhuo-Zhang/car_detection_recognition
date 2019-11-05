import pandas
import sys
import matplotlib.pyplot as plt

if __name__ == "__main__":
    model_name = sys.argv[1]
    model_data = pandas.read_csv(model_name + "_test_result.csv", sep = ',', index_col = None)
    print(model_data.columns)
    # model_data = model_data.drop("loss",axis=1)
    # model_data.drop(columns="val_loss")
    print(model_data)
    model_data.plot.bar(x='class',y=['top_1','top_5'])
    print("Average Top 1 Accuracy: " + str(model_data['top_1'].mean()))
    print("Average Top 5 Accuracy: " + str(model_data['top_5'].mean())) 
    plt.title(model_name + ' results on test set')
    plt.xlabel('Class')
    plt.ylabel('Accuracy')
    plt.show()