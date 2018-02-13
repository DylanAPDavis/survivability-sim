import job
algorithm_dict = {
    "unicast": ["flexbhandari", "minimumcost", "minimumrisk", "bhandari", "hamiltonian", "yens"],  # ["ilp", "flexbhandari", "minimumcost", "minimumrisk", "bhandari", "hamiltonian", "yens"],
    "anycast": ["flexbhandari", "minimumcost", "minimumrisk", "bhandari", "hamiltonian", "yens"],  # ["ilp", "flexbhandari", "minimumcost", "minimumrisk", "bhandari", "hamiltonian", "yens"],
    "multicast": ["ilp", "flexbhandari", "minimumcost", "minimumrisk", "bhandari", "hamiltonian", "yens", "overlappingtrees", "memberforwarding", "cyclefortwo", "tabu"],
    "manycast": ["ilp", "flexbhandari", "minimumcost", "minimumrisk", "bhandari", "hamiltonian", "yens", "overlappingtrees", "memberforwarding", "cyclefortwo", "tabu"],
    "manytoone": ["ilp", "flexbhandari", "minimumcost", "minimumrisk", "bhandari", "hamiltonian", "yens", "overlappingtrees", "memberforwarding", "cyclefortwo", "tabu"],
    "manytomany": ["ilp", "flexbhandari", "minimumcost", "minimumrisk", "bhandari", "hamiltonian", "yens", "overlappingtrees", "memberforwarding", "cyclefortwo", "tabu"],
    "broadcast": ["ilp", "flexbhandari", "minimumcost", "minimumrisk", "bhandari", "hamiltonian", "yens", "overlappingtrees", "memberforwarding", "cyclefortwo", "tabu"],
}
s_d_value_dict = {
    "unicast":
    [
        {"num_s": 1, "num_d": 1, "use_min_s": 1, "use_max_s": 1, "use_min_d": 1, "use_max_d": 1}
    ],
    "anycast":
    [
        {"num_s": 1, "num_d": 2, "use_min_s": 1, "use_max_s": 1, "use_min_d": 1, "use_max_d": 1},
        {"num_s": 1, "num_d": 3, "use_min_s": 1, "use_max_s": 1, "use_min_d": 1, "use_max_d": 1},
    ],
    "multicast":
    [
        {"num_s": 1, "num_d": 2, "use_min_s": 1, "use_max_s": 1, "use_min_d": 2, "use_max_d": 2},
        {"num_s": 1, "num_d": 3, "use_min_s": 1, "use_max_s": 1, "use_min_d": 3, "use_max_d": 3},
    ],
    "manycast":
    [
        {"num_s": 1, "num_d": 3, "use_min_s": 1, "use_max_s": 1, "use_min_d": 2, "use_max_d": 2},
        {"num_s": 1, "num_d": 4, "use_min_s": 1, "use_max_s": 1, "use_min_d": 3, "use_max_d": 3},
    ],
    "manytoone":
    [
        # {"num_s": 2, "num_d": 1, "use_min_s": 1, "use_max_s": 1, "use_min_d": 1, "use_max_d": 1},
        {"num_s": 2, "num_d": 1, "use_min_s": 2, "use_max_s": 2, "use_min_d": 1, "use_max_d": 1},
        # {"num_s": 3, "num_d": 1, "use_min_s": 1, "use_max_s": 1, "use_min_d": 1, "use_max_d": 1},
        # {"num_s": 3, "num_d": 1, "use_min_s": 2, "use_max_s": 2, "use_min_d": 1, "use_max_d": 1},
        {"num_s": 3, "num_d": 1, "use_min_s": 3, "use_max_s": 3, "use_min_d": 1, "use_max_d": 1},
    ],
    "manytomany":
    [
        # {"num_s": 2, "num_d": 2, "use_min_s": 2, "use_max_s": 2, "use_min_d": 1, "use_max_d": 1},
        {"num_s": 2, "num_d": 2, "use_min_s": 2, "use_max_s": 2, "use_min_d": 1, "use_max_d": 2},
        {"num_s": 2, "num_d": 2, "use_min_s": 2, "use_max_s": 2, "use_min_d": 2, "use_max_d": 2},
        # {"num_s": 3, "num_d": 3, "use_min_s": 3, "use_max_s": 3, "use_min_d": 1, "use_max_d": 1},
        # {"num_s": 3, "num_d": 3, "use_min_s": 3, "use_max_s": 3, "use_min_d": 1, "use_max_d": 2},
        # {"num_s": 3, "num_d": 3, "use_min_s": 3, "use_max_s": 3, "use_min_d": 2, "use_max_d": 2},
        {"num_s": 3, "num_d": 3, "use_min_s": 3, "use_max_s": 3, "use_min_d": 1, "use_max_d": 3},
        {"num_s": 3, "num_d": 3, "use_min_s": 3, "use_max_s": 3, "use_min_d": 2, "use_max_d": 3},
        {"num_s": 3, "num_d": 3, "use_min_s": 3, "use_max_s": 3, "use_min_d": 3, "use_max_d": 3},
    ],
    "broadcast":
    [
        {"num_s": 2, "num_d": 2, "use_min_s": 2, "use_max_s": 2, "use_min_d": 2, "use_max_d": 2},
        {"num_s": 3, "num_d": 3, "use_min_s": 3, "use_max_s": 3, "use_min_d": 3, "use_max_d": 3},
        {"num_s": 4, "num_d": 4, "use_min_s": 4, "use_max_s": 4, "use_min_d": 4, "use_max_d": 4},
    ],
}
traffic_combo_dict = {
    "unicast": ["both"],  # ["none"],
    "anycast": ["both"],  # ["none"],
    "multicast": ["both"],  # ["none", "source", "dest", "both"],
    "manycast": ["both"],  # ["none", "source", "dest", "both"],
    "manytoone": ["both"],  # ["none", "source", "dest", "both"],
    "manytomany": ["both"],  # ["none", "source", "dest", "both"],
    "broadcast": ["both"],  # ["none", "source", "dest", "both"],
}

# routing_types = ["unicast", "anycast", "manycast", "multicast", "manytoone", "manytomany", "broadcast"]
routing_types = ["unicast","anycast"]
failure_scenarios = ["default", "alllinks", "allnodes", "quake1", "quake2", "quake3"]
# failure_scenarios = ["default", "alllinks", "allnodes", "quake1", "quake2", "quake3", "quake12", "quake13", "quake23", "quake123"]
nfe_values = [0, 1, 2, 3, 9999]
topologies = ["nsfnet", "tw"]


def create_jobs(seed):
    jobs = []
    for topology in topologies:
        for routing_type in routing_types:
            algorithms = algorithm_dict[routing_type]
            sd_values = s_d_value_dict[routing_type]
            traffic_combos = traffic_combo_dict[routing_type]
            for traffic in traffic_combos:
                for algorithm in algorithms:
                    for sd_value in sd_values:
                        num_s = sd_value["num_s"]
                        num_d = sd_value["num_d"]
                        use_min_s = sd_value["use_min_s"]
                        use_max_s = sd_value["use_max_s"]
                        use_min_d = sd_value["use_min_d"]
                        use_max_d = sd_value["use_max_d"]
                        ignore_values = ["false"]
                        if algorithm == "ilp":
                            ignore_values.append("true")
                        for scenario in failure_scenarios:
                            for nfe in nfe_values:
                                # Only use a nfe of 0 if you're doing default scenario
                                if nfe == 0 and scenario != "default":
                                    continue
                                if scenario == "default" and nfe != 0:
                                    continue
                                for ignore in ignore_values:
                                    if nfe == 0 and ignore == "true":
                                        continue
                                    j1 = job.build_request(seed=seed, topo=topology, routing=routing_type, algorithm=algorithm,
                                                           num_s=num_s, num_d=num_d, use_min_s=use_min_s, use_max_s=use_max_s,
                                                           use_min_d=use_min_d, use_max_d=use_max_d, traffic_combo=traffic,
                                                           f_scenario=scenario, fail_type="Both", nfe=nfe, overlap="none",
                                                           s_fail="allow", d_fail="allow", ignore=ignore, num_threads=8,
                                                           use_aws="true")
                                    jobs.append(j1)
    return jobs

