from os import listdir, remove
from os.path import isfile, join
directories = ["results/raw/", "results/analyzed/", "results/output/"]
for directory in directories:
    files = [f for f in listdir(directory) if isfile(join(directory, f))]
    for f in files:
        #print(join(directory, f))
        remove(join(directory, f))
    print("Done with " +  directory)