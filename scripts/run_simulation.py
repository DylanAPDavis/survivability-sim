from launch import launch_simulator
import sys, ast
import traceback


default_len = 36


def run_sim(args):
    print(args)
    num_args = len(args)
    if num_args != default_len:
        print(sys.argv)
        print_usage_message()
        sys.exit(-1)

    # Build the dictionary for the request
    param_dict = build_param_dict(args)

    analyze_params = {}
    # If user passes in arg to analyze after request runs
    run_analysis = args[-1].lower() == "true"
    if run_analysis:
        analyze_params = {
            "requestSetId": param_dict["requestSetId"],
            "useAws": param_dict["useAws"]
        }
    
    # Launch the simulator
    process = launch_simulator(loop=True, sim_params=param_dict, analysis_params=analyze_params, port_num="0", use_web_server="false")

    # Kill the simulator (in case it hasn't died)
    # kill(process)
    print("Done")


def build_param_dict(args):
    try:
        return {
            "seed": ast.literal_eval(args[0]),
            "topologyId": args[1],
            "problemClass": args[2],
            "objective": args[3],
            "algorithm": args[4],
            "numRequests": ast.literal_eval(args[5]),
            "numSources": ast.literal_eval(args[6]),
            "numDestinations": ast.literal_eval(args[7]),
            "numConnections": ast.literal_eval(args[8]),
            "minConnectionsRange": ast.literal_eval(args[9]),
            "maxConnectionsRange": ast.literal_eval(args[10]),
            "minSrcConnectionsRange": ast.literal_eval(args[11]),
            "maxSrcConnectionsRange": ast.literal_eval(args[12]),
            "minDstConnectionsRange": ast.literal_eval(args[13]),
            "maxDstConnectionsRange": ast.literal_eval(args[14]),
            "reachMinS": ast.literal_eval(args[15]),
            "reachMaxS": ast.literal_eval(args[16]),
            "reachMinD": ast.literal_eval(args[17]),
            "reachMaxD": ast.literal_eval(args[18]),
            "failureSetSize": ast.literal_eval(args[19]),
            "minMaxFailures": ast.literal_eval(args[20]),
            "failureClass": args[21],
            "failureProb": ast.literal_eval(args[22]),
            "minMaxFailureProb": ast.literal_eval(args[23]),
            "numFailsAllowed": ast.literal_eval(args[24]),
            "minMaxFailsAllowed": ast.literal_eval(args[25]),
            "processingType": args[26],
            "percentSrcAlsoDest": ast.literal_eval(args[27]),
            "percentSrcFail": ast.literal_eval(args[28]),
            "percentDestFail": ast.literal_eval(args[29]),
            "sdn": args[30].lower() == "true",
            "useAws": args[31].lower() == "true",
            "ignoreFailures": args[32].lower() == "true",
            "numThreads": ast.literal_eval(args[33]),
            "requestSetId": args[34]
        }
    except Exception as e:
        print(traceback.format_exc())
        sys.exit()



def print_usage_message():
    message = "Usage: seed topologyId problemClass objective algorithm numRequests numSources numDestinations"
    message += " numConnections minConnectionsRange[min, max] maxConnectionsRange[min, max]"
    message += " minSrcConnectionsRange[min, max] maxSrcConnectionsRange[min, max]"
    message += " minDstConnectionsRange[min, max] maxDstConnectionsRange[min, max]"
    message += " reachMinSources, reachMaxSources, reachMinDestinations, reachMaxDestinations"
    message += " failureSetSize minMaxFailures[min, max] failureClass failureProb minMaxFailureProb[min, max]"
    message += " numFailsAllowed minMaxFailsAllowed[min, max] processingType percentSrcAlsoDest"
    message += " percentSrcFail percentDstFail sdn useAWS ignoreFailures numThreads requestSetId analyzeAfterRun"
    print(message)


if __name__ == "__main__":
    if len(sys.argv) != default_len + 1:
        print(str(len(sys.argv)) + " args submitted")
        print_usage_message()
        sys.exit(-1)
    run_sim(sys.argv[1:])
