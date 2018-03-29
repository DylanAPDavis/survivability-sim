import os
import subprocess
import sys
import logging
import traceback
import json


def launch_simulator(loop, sim_params, analysis_params, port_num="9867", use_web_server="true", aggregate="false",
                     mass_analyze_params=None, mass_run=None):
    process = None

    try:
        top_dir = os.path.join(os.path.dirname(__file__), "..")
        os.chdir(top_dir)

        # Launch simulator
        port = "--server.port=" + port_num
        sim = '--sim=' + convert_params_to_string(sim_params)
        web = "--web=" + use_web_server
        analysis = "--analyze=" + convert_params_to_string(analysis_params)
        aggregate_arg = "--defaultAggregate=" + aggregate
        command_input = ['java', "-jar", os.path.join("target", "survivability-sim-0.0.1-SNAPSHOT.jar"), port, web]
        if len(sim_params) > 0:
            command_input.append(sim)
        if len(analysis_params) > 0:
            command_input.append(analysis)
        command_input.append(aggregate_arg)
        if mass_analyze_params is not None:
            mass_analyze_arg = "--massAnalyze=" + convert_params_to_string(mass_analyze_params)
            command_input.append(mass_analyze_arg)
        if mass_run is not None:
            sim_set = "--massRun=" + mass_run
            command_input.append(sim_set)

        process = subprocess.Popen(command_input)

        if loop:
            process.wait()
        return process

    except Exception as e:
        logging.error(traceback.format_exc())
        kill(process)
        sys.exit()


def convert_params_to_string(sim_params):
    return json.dumps(sim_params)


def kill(process):
    print(process)
    if process is not None:
        process.terminate()

if __name__ == "__main__":
    if len(sys.argv) < 1 or len(sys.argv) > 3:
        print("Usage: <Loop (0 or 1, Default 1)> <Port Num (Default 9867)>")
        sys.exit(-1)
    loop_val = sys.argv[1] if len(sys.argv) > 1 else 1
    port_val = sys.argv[2] if len(sys.argv) == 3 else "9867"
    launch_simulator(loop_val, {}, {}, port_val, "true", "false")
