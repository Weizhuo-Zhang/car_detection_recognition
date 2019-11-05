import pandas
import sys
import matplotlib.pyplot as plt


if __name__ == "__main__":
    model_name = sys.argv[1]
    model_data = pandas.read_csv(model_name + "log.csv", sep = ';')
    print(model_data.columns)
    # model_data = model_data.drop("loss",axis=1)
    # model_data.drop(columns="val_loss")
    model_data.rename(columns={'acc': 'Training Accuracy',
                            'val_acc': 'Validation Accuracy'}, 
                inplace=True)
    ax = model_data.plot(use_index=True, y=['Training Accuracy', 'Validation Accuracy'], title='Accuracy growth on training dataset - ' + model_name)
    # ax = model_data.plot(use_index=True, y=, title='Accuracy growth on training dataset - model_dataobile')
    ax.set_xlabel('Epochs')
    ax.set_ylabel('Accuracy')
    plt.show()
