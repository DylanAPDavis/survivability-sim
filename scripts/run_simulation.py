from launch import launch_simulator, kill
import sys, ast

default_len = 30


def run_sim(args):
    num_args = len(args)
    if num_args != default_len:
        print(sys.argv)
        print_usage_message()
        sys.exit(-1)

    # Build the dictionary for the request
    param_dict = build_param_dict(args)
    
    # Launch the simulator
    process = launch_simulator(True, param_dict, "0", "false")

    # Kill the simulator (in case it hasn't died)
    # kill(process)
    print("Done")


def build_param_dict(args):
    return {
        "seed": ast.literal_eval(args[0]),
        "topologyId": args[1],
        "numRequests": ast.literal_eval(args[2]),
        "algorithm": args[3],
        "problemClass": args[4],
        "objective": args[5],
        "numSources": ast.literal_eval(args[6]),
        "numDestinations": ast.literal_eval(args[7]),
        "failureSetSize": ast.literal_eval(args[8]),
        "minMaxFailures": ast.literal_eval(args[9]),
        "failureClass": args[10],
        "failureProb": ast.literal_eval(args[11]),
        "minMaxFailureProb": ast.literal_eval(args[12]),
        "numConnections": ast.literal_eval(args[13]),
        "minConnectionsRange": ast.literal_eval(args[14]),
        "maxConnectionsRange": ast.literal_eval(args[15]),
        "minSrcConnectionsRange": ast.literal_eval(args[16]),
        "maxSrcConnectionsRange": ast.literal_eval(args[17]),
        "minDstConnectionsRange": ast.literal_eval(args[18]),
        "maxDstConnectionsRange": ast.literal_eval(args[19]),
        "numFailsAllowed": ast.literal_eval(args[20]),
        "minMaxFailsAllowed": ast.literal_eval(args[21]),
        "processingType": args[22],
        "percentSrcAlsoDest": ast.literal_eval(args[23]),
        "percentSrcFail": ast.literal_eval(args[24]),
        "percentDestFail": ast.literal_eval(args[25]),
        "sdn": ast.literal_eval(args[26]),
        "useAws": ast.literal_eval(args[27]),
        "ignoreFailures": ast.literal_eval(args[28]),
        "numThreads": ast.literal_eval(args[29])
    }


def print_usage_message():
    message = "Usage: seed topologyId numRequests algorithm problemClass objective numSources numDestinations"
    message += " failureSetSize minMaxFailures[min, max] failureClass failureProb minMaxFailureProb[min, max]"
    message += " numConnections minConnectionsRange[min, max] maxConnectionsRange[min, max]"
    message += " minSrcConnectionsRange[min, max] maxSrcConnectionsRange[min, max]"
    message += " minDstConnectionsRange[min, max] maxDstConnectionsRange[min, max]"
    message += " numFailsAllowed minMaxFailsAllowed[min, max] processingType percentSrcAlsoDest"
    message += " percentSrcFail percentDstFail sdn useAWS ignoreFailures numThreads"
    print(message)


if __name__ == "__main__":
    if len(sys.argv) != default_len + 1:
        print(str(len(sys.argv)) + " args submitted")
        print_usage_message()
        sys.exit(-1)
    run_sim(sys.argv[1:])
