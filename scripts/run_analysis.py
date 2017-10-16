from launch import launch_simulator, kill

default_len = 1
def run_analysis(request_set_id, use_aws=False):
    params = {
        "requestSetId" : request_set_id,
        "useAws" : use_aws
    }
    launch_simulator(loop=True, sim_params={}, analysis_params=params, port_num="0", use_web_server="false")
    print("Done")

def print_usage_message():
    message = "Usage: requestSetId useAws"
    print(message)


if __name__ == "__main__":
    if len(sys.argv) != default_len + 1:
        print(str(len(sys.argv)) + " args submitted")
        print_usage_message()
        sys.exit(-1)
    run_sim(sys.argv[1:])