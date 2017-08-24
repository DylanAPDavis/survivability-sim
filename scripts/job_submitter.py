import subprocess
import math
import time
# seeds = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30]
seeds = [9,10,11]
topology_ids = ["NSFnet"]
problem_classes = ["Flex", "Flow", "FlowSharedF", "EndpointSharedF", "Endpoint", "Combined"]
objectives = ["TotalCost"]
algorithms = ["ServiceILP"]
num_requests = [1]
num_sources = [1, 7, 14]
num_dests = [1, 7, 14]
failure_set_dict = {
    "Link": [[0, 0, 0.0, 0.0], [1, 1, 0.0, 0.0], [21, 1, 0.0, 0.0], [21, 2, 0.0, 0.0]],
    "Node": [[1, 1, 0.0, 0.0], [1, 1, 0.0714, 0.0], [1, 1, 0.0, 0.0714], [14, 1, 1.0, 1.0], [14, 2, 1.0, 1.0]],
    "Both": [[35, 1, 1.0, 1.0]]
}  # Includes failure class, num fails, num fails allowed, percent src fail, percent dst fail
num_conns = [1, 7, 14]
min_connection_ranges = [[0, 0], [1, 1]]
max_connection_ranges = [[1, 1], [2, 2]]
min_src_connection_ranges = [[0, 0], [1, 1]]
max_src_connection_ranges = [[1, 1], [2, 2]]
min_dst_connection_ranges = [[0, 0], [[1, 1]]]
max_dst_connection_ranges = [[1, 1], [2, 2]]
percent_src_also_dests = [0.0, 1.0]
ignore_failures = [True, False]
threads = [6]


num_params = 0
num_node_params = 0
num_both_params = 0


def create_params(seed, topology, problem, objective, algorithm, num_r, num_c, min_c_range, max_c_range,
                  min_src_c_range, max_src_c_range, min_dst_c_range, max_dst_c_range, num_s, num_d,
                  percent_src_dest, ignore, fail_type, fail_params, threads):
    num_fails = fail_params[0]
    num_fails_allowed = fail_params[1]
    src_fail_percent = fail_params[2]
    dst_fail_percent = fail_params[3]
    num_s_in_d = math.ceil(percent_src_dest * num_s)
    exclusive_s = num_s - num_s_in_d
    complete_overlap = num_s_in_d == num_d and exclusive_s == 0
    if num_s_in_d > num_d or (complete_overlap and num_d == 1) or (node_count(topology) - exclusive_s < num_d):
        return None
    if fail_type == "Node":
        num_s_fail = math.ceil(src_fail_percent * num_s)
        num_d_fail = math.ceil(dst_fail_percent * num_d)
        if num_s_fail > num_fails or num_d_fail > num_fails or (num_s_fail - num_s_in_d + num_d_fail > num_fails):
            return None
    min_range = min_c_range if problem == "Flow" or problem == "FlowSharedF" or problem == "Combined" else []
    max_range = max_c_range if problem == "Flow" or problem == "FlowSharedF" or problem == "Combined" else []
    min_src_range = min_src_c_range if problem == "Endpoint" or problem == "EndpointSharedF" or problem == "Combined" else []
    max_src_range = max_src_c_range if problem == "Endpoint" or problem == "EndpointSharedF" or problem == "Combined" else []
    min_dst_range = min_dst_c_range if problem == "Endpoint" or problem == "EndpointSharedF" or problem == "Combined" else []
    max_dst_range = max_dst_c_range if problem == "Endpoint" or problem == "EndpointSharedF" or problem == "Combined" else []
    return [seed, topology, num_r, algorithm, problem, objective, num_s, num_d, num_fails, [], fail_type, 1.0, [],
            num_c, min_range, max_range, min_src_range, max_src_range, min_dst_range, max_dst_range,
            num_fails_allowed, [], "Solo", percent_src_dest, src_fail_percent, dst_fail_percent,
            False, True, ignore, threads]
    # "Usage: seed topologyId numRequests algorithm problemClass objective numSources numDestinations"
    # " failureSetSize minMaxFailures[min, max] failureClass failureProb minMaxFailureProb[min, max]"
    # " numConnections minConnectionsRange[min, max] maxConnectionsRange[min, max]"
    # " numFailsAllowed minMaxFailsAllowed[min, max] processingType percentSrcAlsoDest"
    # " percentSrcFail percentDstFail sdn useAWS ignoreFailures"


def node_count(topology_name):
    if topology_name == "NSFnet":
        return 14


def create_job(seed, topology, problem, objective, algorithm, num_r, num_c, min_c_range, max_c_range,
               min_src_c_range, max_src_c_range, min_dst_c_range, max_dst_c_range, num_s, num_d, percent_src_dest,
               ignore, fail_type, fail_params, numThreads):
    # 1_NSFnet_Flex_TotalCost_ServiceILP_1_1_1_1_[0,0]_[1,1]_0_[]_Link_1.0_[]_0_[]_Solo_0.0_0.0_0.0_false_true_true
    output_file_path = "results/output/" + "_".join(
        [str(seed), topology, problem, objective, algorithm, str(num_r), str(num_s), str(num_d), str(num_c),
         str(min_c_range), str(max_c_range), str(min_src_c_range), str(max_src_c_range), str(min_dst_c_range), str(max_dst_c_range),
         str(fail_params[0]), str([]), fail_type, str(1.0), str([]), str(fail_params[1]),
         str([]), "Solo", str(percent_src_dest), str(fail_params[2]), str(fail_params[3]), "false", "true", str(ignore).lower(),
         str(numThreads)
        ]
    ).replace(" ", "")
    parameters = create_params(seed, topology, problem, objective, algorithm,
                               num_r, num_c, min_c_range, max_c_range,
                               num_s, num_d, percent_src_dest, ignore,
                               fail_type, fail_params)
    if parameters is not None:
        global num_params, num_node_params, num_both_params
        num_params += 1
        if fail_type == "Node":
            num_node_params += 1
        if fail_type == "Both":
            num_both_params += 1

        run_time = "3:59" if (fail_params[0] >= 14 and not ignore) else "0:30"
        memory = "700"
        if fail_params[0] >= 14 and not ignore:
            if fail_params[1] >= 2:
                memory = "3500"
                if problem == "Endpoint":
                    memory = "4000"
            elif num_s >= 14 or num_d >= 14:
                memory = "2700"
            else:
                memory = "1500"

        command_input = ["bsub", "-q", "short", "-W", run_time, "-R", "rusage[mem=" + memory + "] span[hosts=1]", "-n",
                         str(numThreads), "-o", output_file_path, "python", "scripts/run_simulation.py"]
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
                                    for min_src_c_range in min_src_connection_ranges:
                                        for max_src_c_range in max_src_connection_ranges:
                                            for min_dst_c_range in min_dst_connection_ranges:
                                                for max_dst_c_range in max_dst_connection_ranges:
                                                    for num_s in num_sources:
                                                        time.sleep(10)
                                                        for num_d in num_dests:
                                                            for percent_src_dest in percent_src_also_dests:
                                                                for ignore in ignore_failures:
                                                                    for fail_type in failure_set_dict.keys():
                                                                        for fail_params in failure_set_dict[fail_type]:
                                                                            for num_threads in threads:
                                                                                create_job(seed, topology, problem, objective, algorithm,
                                                                                           num_r, num_c, min_c_range, max_c_range,
                                                                                           min_src_c_range, max_src_c_range,
                                                                                           min_dst_c_range, max_dst_c_range, num_s,
                                                                                           num_d, percent_src_dest, ignore, fail_type,
                                                                                           fail_params, num_threads)
                print("Done with: " + problem + " " + objective + " " + str(seed))

print(num_params)
print(num_node_params)
print(num_both_params)