from launch import launch_simulator, kill
import sys


def run_aggregate():
    launch_simulator(loop=True, sim_params={}, analysis_params={}, port_num="0", use_web_server="false", aggregate="true")
    print("Done")

if __name__ == "__main__":
    run_aggregate()
