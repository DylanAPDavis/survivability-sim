import os
import subprocess
import time
import sys
import logging
import traceback


def launch_simulator(loop, port_num="9867"):
    process = None

    try:
        orig_dir = os.getcwd()

        top_dir = os.path.join(os.path.dirname(__file__), "..")

        os.chdir(top_dir)
        top_dir = os.getcwd()

        # Launch simulator
        port_string = "--server.port=" + port_num
        process = subprocess.Popen(['java', "-jar", os.path.join("target", "survivability-sim-0.0.1-SNAPSHOT.jar"), port_string])

        while loop:
            time.sleep(1)
        return process

    except Exception as e:
        logging.error(traceback.format_exc())
        kill(process)
        sys.exit()


def kill(process):
    print(process)
    if process is not None:
        process.kill()

if __name__ == "__main__":
    if len(sys.argv) < 1 or len(sys.argv) > 3:
        print("Usage: <Loop (0 or 1, Default 1)> <Port Num (Default 9867)>")
        sys.exit(-1)
    loop = sys.argv[1] if len(sys.argv) > 1 else 1
    port = sys.argv[2] if len(sys.argv) == 3 else "9867"
    launch_simulator(loop, port)
