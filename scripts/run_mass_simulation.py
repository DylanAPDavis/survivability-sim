from launch import launch_simulator
import sys, ast
import traceback


def run_sim(first_id):
    # Launch the simulator
    process = launch_simulator(loop=True, sim_params={}, analysis_params={}, port_num="0", use_web_server="false",
                               mass_run=first_id)

    # Kill the simulator (in case it hasn't died)
    # kill(process)
    print("Done")


def print_usage_message():
    message = "Usage: <First Request ID Number>"
    print(message)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Submitted: " + str(sys.argv))
        print_usage_message()
        sys.exit(-1)
    run_sim(sys.argv[1])
