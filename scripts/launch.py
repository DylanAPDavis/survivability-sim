import os
import subprocess
import time
import sys
import logging
import traceback

processes = []

try:
    orig_dir = os.getcwd()

    top_dir = os.path.join(os.path.dirname(__file__), "..")

    os.chdir(top_dir)
    top_dir = os.getcwd()

    # Launch core
    core_proc = subprocess.Popen(['java', "-jar", os.path.join("target", "survivability-sim-0.0.1-SNAPSHOT.jar")])
    processes.append(core_proc)

    while 1:
        time.sleep(1)

except Exception as e:
    logging.error(traceback.format_exc())
    for proc in processes:
        proc.kill()
    sys.exit()

