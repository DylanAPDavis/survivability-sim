import subprocess
import os

seeds = [1]
run_time = "2:00"
memory = "8000"
web = "--web=" + str(False)

for seed in seeds:
    rerun = "--rerun_incomplete=" + str(seed)
    output_file_path = "results/output/" + str(seed) + "_rerun_incomplete"
    command_input = ["bsub", "-q", "short", "-W", run_time, "-R", "rusage[mem=" + memory + "] span[hosts=1]", "-n", "6", "-o",
                     output_file_path, 'java', "-jar", os.path.join("target", "survivability-sim-0.0.1-SNAPSHOT.jar"), rerun, web]
    process = subprocess.Popen(command_input, stdout=subprocess.PIPE, universal_newlines=True)