from launch import launch_simulator, kill
from submit_request import submit
import sys, time

default_len = 24
len_with_port = default_len + 1


def run_sim(args):
    num_args = len(args)
    if num_args != default_len and num_args != len_with_port:
        print(sys.argv)
        print_usage_message()
        sys.exit(-1)

    port_num = args[-1] if num_args == len_with_port else "9867"

    # Launch the simulator
    process = launch_simulator(0, port_num)
    time.sleep(10)

    # Submit the request
    submit(args)

    # Kill the simulator
    kill(process)


def print_usage_message():
    message = "Usage: seed topologyId numRequests algorithm problemClass objective numSources numDestinations"
    message += " failureSetSize failureSetSizeRange[min, max] failureClass failureProb failureProbRange[min, max]"
    message += " numConnections minConnectionsRange[min, max] maxConnectionsRange[min, max]"
    message += " numFailsAllowed numFailsAllowedRange[min, max] processingType percentSrcAlsoDest"
    message += " percentSrcFail percentDstFail sdn useAWS  portNum(optional)"
    print(message)

if __name__ == "__main__":
    if len(sys.argv) != default_len + 1 and len(sys.argv) != len_with_port + 1:
        print(len(sys.argv))
        print_usage_message()
        sys.exit(-1)
    run_sim(sys.argv[1:])
