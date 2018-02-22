from launch import launch_simulator, kill
import sys
default_len = 2


def run_analysis(request_id, use_aws):
    params = {
        "requestId": request_id,
        "useAws": use_aws
    }
    launch_simulator(loop=True, sim_params={}, analysis_params=params, use_web_server="false")
    print("Done")


def print_usage_message():
    message = "Usage: requestId useAws"
    print(message)


if __name__ == "__main__":
    if len(sys.argv) != default_len + 1:
        print(str(len(sys.argv)) + " args submitted")
        print_usage_message()
        sys.exit(-1)
    run_analysis(sys.argv[1], sys.argv[2])
