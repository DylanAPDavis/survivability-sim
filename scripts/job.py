def create_ordered_params(*args):
    return [str(i).lower() for i in args]


def create_id(*args):
    "_".join(create_ordered_params(args)).replace(" ", "")


class Job:
    def __init__(self, seed, topo, routing, algorithm, num_s, num_d, use_min_s,
                 use_max_s, use_min_d, use_max_d, traffic_combo, f_scenario, fail_class, nfe, overlap, s_fail, d_fail,
                 ignore, num_threads, use_aws):
        self.seed = seed
        self.topo = topo
        self.routing = routing
        self.algorithm = algorithm
        self.num_s = num_s
        self.num_d = num_d
        self.use_min_s = use_min_s
        self.use_max_s = use_max_s
        self.use_min_d = use_min_d
        self.use_max_d = use_max_d
        self.traffic_combo = traffic_combo
        self.f_scenario = f_scenario
        self.fail_class = fail_class
        self.nfe = nfe
        self.overlap = overlap
        self.s_fail = s_fail
        self.d_fail = d_fail
        self.ignore = ignore
        self.num_threads = num_threads
        self.use_aws = use_aws
        self.ordered_params = create_ordered_params(seed, topo, routing, algorithm, num_s, num_d, use_min_s,
                                                    use_max_s, use_min_d, use_max_d, traffic_combo, f_scenario,
                                                    fail_class, nfe, overlap, s_fail, d_fail, ignore, num_threads)
        self.request_id = "_".join(self.ordered_params).replace(" ", "")


def build_request(seed=1, topo="NSFnet", routing="Unicast", algorithm="ILP",  num_s=1, num_d=1,
                  use_min_s=0, use_max_s=None, use_min_d=0, use_max_d=None, traffic_combo="none", f_scenario="default",
                  fail_type="Both", nfe=0, overlap="none", s_fail="prevent", d_fail="prevent", ignore=True,
                  num_threads=8, use_aws=True):
    if use_max_s is None:
        use_max_s = num_s
    if use_max_d is None:
        use_max_d = num_d
    return Job(seed, topo, routing, algorithm, num_s, num_d, use_min_s,
                   use_max_s, use_min_d, use_max_d, traffic_combo, f_scenario, fail_type, nfe, overlap, s_fail, d_fail,
                   ignore, num_threads, use_aws)