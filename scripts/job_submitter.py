import os
import subprocess
import math
# seeds = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30]
seeds = [8]
topology_ids = ["NSFnet"]
problem_classes = ["Flex", "Flow", "FlowSharedF", "EndpointSharedF", "Endpoint"]
objectives = ["LinksUsed", "Connections", "TotalCost"]
algorithms = ["ServiceILP"]
num_requests = [10]
num_sources = [1, 2, 7, 14]
num_dests = [1, 2, 7, 14]
failure_set_dict = {
    "Link": [[0, 0, 0.0, 0.0], [1, 1, 0.0, 0.0], [2, 1, 0.0, 0.0], [2, 2, 0.0, 0.0], [21, 1, 0.0, 0.0],
             [21, 2, 0.0, 0.0], [21, 21, 0.0, 0.0]
             ],
    "Node": [[1, 1, 0.0, 0.0], [1, 1, 0.0714, 0.0], [1, 1, 0.0, 0.0714],  # One node - not src or dest, src, dest
             [2, 1, 0.0, 0.0], [2, 1, 0.0714, 0.0], [2, 1, 0.0, 0.0714], [2, 1, 0.0714, 0.0714],  # Two nodes, one fails
             [2, 2, 0.0, 0.0], [2, 2, 0.0714, 0.0], [2, 2, 0.0, 0.0714], [2, 2, 0.0714, 0.0714],  # Two nodes, two fail
             [2, 2, 0.142, 0.0], [2, 2, 0.0, 0.142],  # Two nodes, two fail, - both src or both dest
             [14, 1, 1.0, 1.0], [14, 2, 1.0, 1.0], [14, 14, 1.0, 1.0]
             ],
    "Both": [[35, 1, 1.0, 1.0], [35, 2, 1.0, 1.0], [35, 35, 1.0, 1.0]]
}  # Includes failure class, num fails, num fails allowed, percent src fail, percent dst fail
num_conns = [1, 2, 7, 14]
min_connection_ranges = [[0, 0], [1, 1], [2, 2]]
max_connection_ranges = [[1, 1], [2, 2], [14, 14]]
percent_src_also_dests = [0.0, 0.5, 1.0]
ignore_failures = [True, False]


def create_params(seed, topology, problem, objective, algorithm, num_r, num_c, min_c_range, max_c_range, num_s, num_d,
                  percent_src_dest, ignore, fail_type, fail_params):
    if 2 in min_c_range and 1 in max_c_range:
        return None
    num_s_in_d = math.ceil(percent_src_dest * num_s)
    exclusive_s = num_s - num_s_in_d
    num_fails = fail_params[0]
    num_fails_allowed = fail_params[1]
    src_fail_percent = fail_params[2]
    dst_fail_percent = fail_params[3]
    complete_overlap = num_s_in_d == num_d and exclusive_s == 0
    if num_s_in_d > num_d or (complete_overlap and num_d == 1) or (node_count(topology) - exclusive_s < num_d) or \
            (ignore_failures and num_fails != 0):
        return None
    if fail_type == "Node":
        num_s_fail = math.ceil(src_fail_percent * num_s)
        num_d_fail = math.ceil(dst_fail_percent * num_d)
        if num_s_fail > num_fails or num_d_fail > num_fails or (num_s_fail - num_s_in_d + num_d_fail > num_fails):
            return None
    min_range = min_c_range if problem != "Flex" else []
    max_range = max_c_range if problem != "Flex" else []
    return [seed, topology, num_r, algorithm, problem, objective, num_s, num_d, num_fails, [], fail_type, 1.0, [],
            num_c, min_range, max_range, num_fails_allowed, [], "Solo", percent_src_dest, src_fail_percent, dst_fail_percent,
            False, True, ignore]
    # "Usage: seed topologyId numRequests algorithm problemClass objective numSources numDestinations"
    # " failureSetSize minMaxFailures[min, max] failureClass failureProb minMaxFailureProb[min, max]"
    # " numConnections minConnectionsRange[min, max] maxConnectionsRange[min, max]"
    # " numFailsAllowed minMaxFailsAllowed[min, max] processingType percentSrcAlsoDest"
    # " percentSrcFail percentDstFail sdn useAWS ignoreFailures"


def node_count(topology_name):
    if topology == "NSFnet":
        return 14


def create_job(seed, topology, problem, objective, algorithm, num_r, num_c, min_c_range, max_c_range, num_s, num_d,
               percent_src_dest, ignore, fail_type, fail_params):

    output_file_path = "results/output/" + "_".join([str(seed), topology, problem, objective, algorithm,
                                 str(num_r), str(num_c), str(min_c_range),
                                 str(max_c_range), str(num_s), str(num_d),
                                 str(percent_src_dest), str(ignore), fail_type,
                                 str(fail_params)])
    parameters = create_params(seed, topology, problem, objective, algorithm,
                               num_r, num_c, min_c_range, max_c_range,
                               num_s, num_d, percent_src_dest, ignore,
                               fail_type, fail_params)
    if parameters is not None:
        command_input = ["bsub", "-q", "short", "-W", "3:59", "-R", "rusage[mem=1500] span[hosts=1]", "-n", "8", "-o", output_file_path, "python", "scripts/run_simulation.py"]
        for param in parameters:
            command_input.append(str(param))
        process = subprocess.Popen(command_input, stdout=subprocess.PIPE, universal_newlines=True)


# create_job(1, "NSFNet", "Endpoint", "TotalCost", "ServiceILP", 5, 1, [0, 0], [4, 4], 14, 14, 1.0, False, "Link", [14, 1, 0.0, 0.0])
# exit(1)

for seed in seeds:
    for topology in topology_ids:
        for problem in problem_classes:
            for objective in objectives:
                print("Starting: " + problem + " " + objective + " " + str(seed))
                for algorithm in algorithms:
                    for num_r in num_requests:
                        for num_c in num_conns:
                            for min_c_range in min_connection_ranges:
                                for max_c_range in max_connection_ranges:
                                    for num_s in num_sources:
                                        for num_d in num_dests:
                                            for percent_src_dest in percent_src_also_dests:
                                                for ignore in ignore_failures:
                                                    for fail_type in failure_set_dict.keys():
                                                        for fail_params in failure_set_dict[fail_type]:
                                                            pass
                                                            create_job(seed, topology, problem, objective, algorithm,
                                                                       num_r, num_c, min_c_range, max_c_range, num_s,
                                                                       num_d,percent_src_dest, ignore, fail_type,
                                                                       fail_params)
                print("Done with: " + problem + " " + objective + " " + str(seed))
