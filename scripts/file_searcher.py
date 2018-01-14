from os import listdir
from os.path import isfile, join

onlyfiles = [f for f in listdir("results/output") if isfile(join("results/output", f))]
files_not_stored = []
files_hit_mem_limit = []
files_hit_time_limit = []
stored_string = "Stored params"
already_completed_string = "Already completed, exiting..."
time_limit_string = "time limit"
memory_limit_string = "memory usage limit"
for file in onlyfiles:
    with open(join("results/output", file), encoding="latin-1") as f:
        content = f.readlines()
        was_stored = False
        hit_time_limit = False
        hit_memory_limit = False
        for line in content:
            if stored_string in line or already_completed_string in line:
                was_stored = True
            if time_limit_string in line:
                hit_time_limit = True
                break
            if memory_limit_string in line:
                hit_memory_limit = True
                break
        if not was_stored:
            files_not_stored.append(file)
        if hit_time_limit:
            files_hit_time_limit.append(file)
        if hit_memory_limit:
            files_hit_mem_limit.append(file)
print(files_not_stored)
print(len(files_not_stored))
print(files_hit_time_limit)
print(len(files_hit_time_limit))
print(files_hit_mem_limit)
print(len(files_hit_mem_limit))