from os import listdir
from os.path import isfile, join

onlyfiles = [f for f in listdir("../results/output") if isfile(join("../results/output", f))]
files_without_string = []
for file in onlyfiles:
    with open(join("../results/output", file)) as f:
        content = f.readlines()
        contains_string = False
        for line in content:
            if "Stored params" in line:
                contains_string = True
                break
        if not contains_string:
            files_without_string.append(file)
print files_without_string
print len(files_without_string)