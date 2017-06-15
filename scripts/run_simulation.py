from launch import launch_simulator, kill
import sys, ast

default_len = 25


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
    kill(process)
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
        "numFailsAllowed": ast.literal_eval(args[16]),
        "minMaxFailsAllowed": ast.literal_eval(args[17]),
        "processingType": args[18],
        "sdn": ast.literal_eval(args[19]),
        "useAws": ast.literal_eval(args[20]),
        "percentSrcAlsoDest": ast.literal_eval(args[21]),
        "percentSrcFail": ast.literal_eval(args[22]),
        "percentDestFail": ast.literal_eval(args[23]),
        "ignoreFailures": ast.literal_eval(args[24])
    }


def print_usage_message():
    message = "Usage: seed topologyId numRequests algorithm problemClass objective numSources numDestinations"
    message += " failureSetSize minMaxFailures[min, max] failureClass failureProb minMaxFailureProb[min, max]"
    message += " numConnections minConnectionsRange[min, max] maxConnectionsRange[min, max]"
    message += " numFailsAllowed minMaxFailsAllowed[min, max] processingType percentSrcAlsoDest"
    message += " percentSrcFail percentDstFail sdn useAWS ignoreFailures"
    print(message)


if __name__ == "__main__":
    if len(sys.argv) != default_len + 1:
        print(len(sys.argv))
        print_usage_message()
        sys.exit(-1)
    run_sim(sys.argv[1:])
