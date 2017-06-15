import os
import subprocess
import math
seeds = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30]
topology_ids = ["NSFnet"]
problem_classes = ["Flex", "Flow", "FlowSharedF", "EndpointSharedF", "Endpoint"]
objectives = ["LinksUsed", "Connections", "TotalCost"]
algorithms = ["ServiceILP"]
num_requests = [1000]
num_sources = [1, 2, 3, 4, 5, 6, 7, 14]
num_dests = [1, 2, 3, 4, 5, 6, 7, 14]
failure_set_dict = {
    "Link": [[0, 0, 0.0, 0.0], [1, 1, 0.0, 0.0], [2, 1, 0.0, 0.0], [2, 2, 0.0, 0.0], [21, 1, 0.0, 0.0],
             [21, 2, 0.0, 0.0], [21, 21, 0.0, 0.0]
             ],
    "Node": [[0, 0, 0.0, 0.0],
             [1, 1, 0.0, 0.0], [1, 1, 0.0714, 0.0], [1, 1, 0.0, 0.0714],
             [2, 1, 0.0, 0.0], [2, 1, 0.0714, 0.0], [2, 1, 0.0, 0.0714], [2, 1, 0.0714, 0.0714],
             [2, 2, 0.0, 0.0], [2, 2, 0.0714, 0.0], [2, 2, 0.0, 0.0714], [2, 2, 0.0714, 0.0714],
             [2, 2, 0.142, 0.0], [2, 2, 0.0, 0.142],
             [14, 1, 1.0, 1.0], [14, 2, 1.0, 1.0], [14, 14, 1.0, 1.0]
             ],
    "Both": [[0, 0, 0.0, 0.0],
             [1, 1, 0.0, 0.0], [1, 1, 0.0714, 0.0], [1, 1, 0.0, 0.0714],
             [2, 1, 0.0, 0.0], [2, 1, 0.0714, 0.0], [2, 1, 0.0, 0.0714], [2, 1, 0.0714, 0.0714],
             [2, 2, 0.0, 0.0], [2, 2, 0.0714, 0.0], [2, 2, 0.0, 0.0714], [2, 2, 0.0714, 0.0714],
             [2, 2, 0.142, 0.0], [2, 2, 0.0, 0.142],
             [14, 1, 1.0, 1.0], [14, 2, 1.0, 1.0], [14, 14, 1.0, 1.0],
             [35, 1, 1.0, 1.0], [35, 2, 1.0, 1.0], [35, 35, 1.0, 1.0]
             ]
}  # Includes failure class, num fails, num fails allowed, percent src fail, percent dst fail
num_conns = [1, 2, 3, 4, 5, 6, 7, 14]
min_connection_ranges = [[0, 0], [1, 1], [2, 2]]
max_connection_ranges = [[1, 1], [2, 2], [14, 14]]
percent_src_also_dests = [0.0, 0.5, 1.0]
ignore_failures = [True, False]


def create_params(seed, topology, problem, objective, algorithm, num_r, num_c, min_c_range, max_c_range, num_s, num_d,
                  percent_src_dest, ignore, fail_type, fail_params):
    num_s_in_d = math.ceil(percent_src_dest * num_s)
    num_fails = fail_params[0]
    num_fails_allowed = fail_params[1]
    src_fail_percent = fail_params[2]
    dst_fail_percent = fail_params[3]
    if num_s_in_d > num_d:
        return []
    if fail_type != "Link":
        num_s_fail = math.ceil(src_fail_percent * num_s)
        num_d_fail = math.ceil(dst_fail_percent * num_d)
        if num_s_fail > num_fails or num_d_fail > num_fails:
            return []
    else:
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


for seed in seeds:
    for topology in topology_ids:
        for problem in problem_classes:
            for objective in objectives:
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
                                                            output_file_path = "_".join([seed, topology, problem, objective, algorithm,
                                                                                         num_r, num_c, min_c_range,
                                                                                         max_c_range, num_s, num_d,
                                                                                         percent_src_dest, ignore, fail_type,
                                                                                         fail_params])
                                                            parameters = create_params(seed, topology, problem, objective, algorithm,
                                                                                       num_r, num_c, min_c_range, max_c_range,
                                                                                       num_s, num_d, percent_src_dest, ignore,
                                                                                       fail_type, fail_params)
                                                            command_input = ["bsub", "-q", "long", "-W", "120:30", "-R",
                                                                             "rusage[mem=4000] span[hosts=1]", "-n", "10",
                                                                             "-o", output_file_path, "python", "run_simulation.py"]
                                                            command_input += parameters
                                                            process = subprocess.Popen(command_input, stdout=subprocess.PIPE, universal_newlines=True)
