from launch import launch_simulator, kill
import sys
import json

default_len = 3


def run_mass_analyze(seeds, topology, routing_type):
    seeds_list = json.loads(seeds)
    mass_analyze_params = {
        "seeds": seeds_list,
        "topology": topology,
        "routingType": routing_type
    }
    launch_simulator(loop=True, sim_params={}, analysis_params={}, port_num="0", use_web_server="false", aggregate="false",
                     mass_analyze_params=mass_analyze_params)
    print("Done")


def print_usage_message():
    print("Usage: <seed> <topology> <routing type>")

if __name__ == "__main__":
    if len(sys.argv) != default_len + 1:
        print(str(len(sys.argv)) + " args submitted")
        print_usage_message()
        sys.exit(-1)
    run_mass_analyze(sys.argv[1], sys.argv[2], sys.argv[3])

