from launch import launch_simulator
import sys, ast
import traceback


default_len = 21


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
            "requestId": param_dict["requestId"],
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
            "routingType": args[2],
            "algorithm": args[3],
            "numSources": ast.literal_eval(args[4]),
            "numDestinations": ast.literal_eval(args[5]),
            "useMinS": ast.literal_eval(args[6]),
            "useMaxS": ast.literal_eval(args[7]),
            "useMinD": ast.literal_eval(args[8]),
            "useMaxD": ast.literal_eval(args[9]),
            "trafficCombinationType": args[10],
            "failureScenario": args[11],
            "failureClass": args[12],
            "numFailureEvents": ast.literal_eval(args[13]),
            "sourceSubsetDestType": args[14],
            "sourceFailureType": args[15],
            "destFailureType": args[16],
            "ignoreFailures": args[17].lower() == "true",
            "numThreads": ast.literal_eval(args[18]),
            "useAws": args[19].lower() == "true",
            "requestId": args[20]
        }
    except Exception as e:
        print(traceback.format_exc())
        sys.exit()


def print_usage_message():
    message = "Usage: seed topologyId routingType algorithm numSources numDestinations"
    message += " useMinS useMaxS useMinD useMaxD trafficCombinationType"
    message += " failureScenario failureClass numFailureEvents"
    message += " sourceSubsetDestType sourceFailureType destFailureType ignoreFailures numThreads"
    message += " useAws requestId"
    print(message)


if __name__ == "__main__":
    if len(sys.argv) != default_len + 1:
        print(str(len(sys.argv)) + " args submitted")
        print_usage_message()
        sys.exit(-1)
    run_sim(sys.argv[1:])
