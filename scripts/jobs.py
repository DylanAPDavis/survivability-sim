def create_ordered_params(*args):
    return [str(i).lower() for i in args]



class Job:
    def __init__(self, seed, topo, problem, objective, algorithm, num_r, num_s, num_d, num_c, min_range, max_range, min_src_range,
                 max_src_range, min_dst_range, max_dst_range, reach_min_s, reach_max_s, reach_min_d, reach_max_d, f_size,
                 fail_range, fail_type, fail_prob, prob_range, nfa, nfa_range, processing, overlap, s_fail, d_fail, sdn,
                 use_aws, ignore, num_threads):
        self.problem = problem
        self.seed = seed
        self.num_s = num_s
        self.num_d = num_d
        self.num_c = num_c
        self.fail_type = fail_type
        self.f_size = f_size
        self.nfa = nfa
        self.overlap = overlap
        self.s_fail = s_fail
        self.d_fail = d_fail
        self.min_range = min_range
        self.max_range = max_range
        self.min_src_range = min_src_range
        self.max_src_range = max_src_range
        self.min_dst_range = min_dst_range
        self.max_dst_range = max_dst_range
        self.reach_min_s = reach_min_s
        self.reach_max_s = reach_max_s
        self.reach_min_d = reach_min_d
        self.reach_max_d = reach_max_d
        self.sdn = sdn
        self.use_aws = use_aws
        self.ignore = ignore
        self.fail_prob = fail_prob
        self.processing = processing
        self.topo = topo
        self.num_r = num_r
        self.algorithm = algorithm
        self.objective = objective
        self.fail_range = fail_range
        self.prob_range = prob_range
        self.nfa_range = nfa_range
        self.num_threads = num_threads
        self.ordered_params = create_ordered_params(seed, topo, problem, objective, algorithm, num_r, num_s, num_d,
                                                    num_c, min_range, max_range, min_src_range,
                                                    max_src_range, min_dst_range, max_dst_range, reach_min_s,
                                                    reach_max_s, reach_min_d, reach_max_d, f_size,
                                                    fail_range, fail_type, fail_prob, prob_range, nfa, nfa_range,
                                                    processing, overlap, s_fail, d_fail, sdn,
                                                    use_aws, ignore, num_threads)
        self.request_set_id = "_".join(self.ordered_params).replace(" ", "")


def build_request(seed=1, topo="NSFnet", problem="Combined", objective="TotalCost", algorithm="ServiceILP", num_r=1,
                  num_s=1, num_d=1, num_c=1, min_range=None, max_range=None, min_src_range=None, max_src_range=None,
                  min_dst_range=None, max_dst_range=None, reach_min_s=0, reach_max_s=None, reach_min_d=0,
                  reach_max_d=None, f_size=0, fail_range=None, fail_type="Both", fail_prob=1.0, prob_range=None,
                  nfa=0, nfa_range=None, processing="Solo", overlap=0.0, s_fail=0.0, d_fail=0.0, sdn=False,
                  use_aws=True, make_ignore_f=True, num_threads=8):
    if min_range is None:
        min_range = []
    if max_range is None:
        max_range = []
    if min_src_range is None:
        min_src_range = []
    if max_src_range is None:
        max_src_range = []
    if min_dst_range is None:
        min_dst_range = []
    if max_dst_range is None:
        max_dst_range = []
    if fail_range is None:
        fail_range = []
    if prob_range is None:
        prob_range = []
    if nfa_range is None:
        nfa_range = []
    if reach_max_s is None:
        reach_max_s = num_s
    if reach_max_d is None:
        reach_max_d = num_d
    job = Job(seed, topo, problem, objective, algorithm, num_r, num_s, num_d, num_c, min_range, max_range, min_src_range,
              max_src_range, min_dst_range, max_dst_range, reach_min_s, reach_max_s, reach_min_d, reach_max_d, f_size,
              fail_range, fail_type, fail_prob, prob_range, nfa, nfa_range, processing, overlap, s_fail, d_fail, sdn,
              use_aws, False, num_threads)
    job_ignore = None if not make_ignore_f else \
        Job(seed, topo, problem, objective, algorithm, num_r, num_s, num_d, num_c, min_range, max_range, min_src_range,
            max_src_range, min_dst_range, max_dst_range, reach_min_s, reach_max_s, reach_min_d, reach_max_d, f_size,
            fail_range, fail_type, fail_prob, prob_range, nfa, nfa_range, processing, overlap, s_fail, d_fail, sdn,
            use_aws, True, num_threads)
    jobs = [job]
    if job_ignore is not None:
        jobs.append(job_ignore)
    return jobs


def create_orig_run_jobs(seed):
    jobs = []
    # SD7
    '''jobs += build_request(problem="Flex", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Link", f_size=21, nfa=1)'''
    jobs += build_request(problem="Flex", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Link", f_size=21, nfa=2)
    '''jobs += build_request(problem="Flex", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Node", f_size=14, nfa=1,
                          s_fail=1.0, d_fail=1.0)
    jobs += build_request(problem="Flex", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Node", f_size=14, nfa=2,
                          s_fail=1.0, d_fail=1.0)
    jobs += build_request(problem="Flex", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Both", f_size=35, nfa=1,
                          s_fail=1.0, d_fail=1.0)'''
    # SD14
    '''jobs += build_request(problem="Flex", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Link", f_size=21, nfa=1,
                          overlap=1.0)
    jobs += build_request(problem="Flex", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Link", f_size=21, nfa=2,
                          overlap=1.0)
    jobs += build_request(problem="Flex", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Node", f_size=14, nfa=1,
                          s_fail=1.0, d_fail=1.0, overlap=1.0)
    jobs += build_request(problem="Flex", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Node", f_size=14, nfa=2,
                          s_fail=1.0, d_fail=1.0, overlap=1.0)
    jobs += build_request(problem="Flex", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Both", f_size=35, nfa=1,
                          s_fail=1.0, d_fail=1.0, overlap=1.0)'''

    # SD7
    '''jobs += build_request(problem="FlowSharedF", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Link", f_size=21, nfa=1,
                          min_range=[0,0], max_range=[2,2])'''
    jobs += build_request(problem="FlowSharedF", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Link", f_size=21, nfa=2,
                          min_range=[0,0], max_range=[2,2])
    '''jobs += build_request(problem="FlowSharedF", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Node", f_size=14, nfa=1,
                          s_fail=1.0, d_fail=1.0, min_range=[0,0], max_range=[2,2])
    jobs += build_request(problem="FlowSharedF", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Node", f_size=14, nfa=2,
                          s_fail=1.0, d_fail=1.0, min_range=[0,0], max_range=[2,2])
    jobs += build_request(problem="FlowSharedF", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Both", f_size=35, nfa=1,
                          s_fail=1.0, d_fail=1.0, min_range=[0,0], max_range=[2,2])
    # SD14
    jobs += build_request(problem="FlowSharedF", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Link", f_size=21, nfa=1,
                          overlap=1.0, min_range=[0,0], max_range=[2,2])
    jobs += build_request(problem="FlowSharedF", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Link", f_size=21, nfa=2,
                          overlap=1.0, min_range=[0,0], max_range=[2,2])
    jobs += build_request(problem="FlowSharedF", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Node", f_size=14, nfa=1,
                          s_fail=1.0, d_fail=1.0, overlap=1.0, min_range=[0,0], max_range=[2,2])
    jobs += build_request(problem="FlowSharedF", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Node", f_size=14, nfa=2,
                          s_fail=1.0, d_fail=1.0, overlap=1.0, min_range=[0,0], max_range=[2,2])
    jobs += build_request(problem="FlowSharedF", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Both", f_size=35, nfa=1,
                          s_fail=1.0, d_fail=1.0, overlap=1.0, min_range=[0,0], max_range=[2,2])'''

    # SD7
    '''jobs += build_request(problem="EndpointSharedF", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Link", f_size=21, nfa=1,
                          min_src_range=[0,0], max_src_range=[2,2], min_dst_range=[0,0], max_dst_range=[2,2])'''
    jobs += build_request(problem="EndpointSharedF", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Link", f_size=21, nfa=2,
                          min_src_range=[0,0], max_src_range=[2,2], min_dst_range=[0,0], max_dst_range=[2,2])
    '''jobs += build_request(problem="EndpointSharedF", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Node", f_size=14, nfa=1,
                          s_fail=1.0, d_fail=1.0, min_src_range=[0,0], max_src_range=[2,2], min_dst_range=[0,0], max_dst_range=[2,2])
    jobs += build_request(problem="EndpointSharedF", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Node", f_size=14, nfa=2,
                          s_fail=1.0, d_fail=1.0, min_src_range=[0,0], max_src_range=[2,2], min_dst_range=[0,0], max_dst_range=[2,2])
    jobs += build_request(problem="EndpointSharedF", seed=seed, num_s=7, num_d=7, num_c=14, fail_type="Both", f_size=35, nfa=1,
                          s_fail=1.0, d_fail=1.0, min_src_range=[0,0], max_src_range=[2,2], min_dst_range=[0,0], max_dst_range=[2,2])'''
    # SD14
    '''jobs += build_request(problem="EndpointSharedF", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Link", f_size=21, nfa=1,
                          overlap=1.0, min_src_range=[0,0], max_src_range=[2,2], min_dst_range=[0,0], max_dst_range=[2,2])'''
    jobs += build_request(problem="EndpointSharedF", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Link", f_size=21, nfa=2,
                          overlap=1.0, min_src_range=[0,0], max_src_range=[2,2], min_dst_range=[0,0], max_dst_range=[2,2])
    '''jobs += build_request(problem="EndpointSharedF", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Node", f_size=14, nfa=1,
                          s_fail=1.0, d_fail=1.0, overlap=1.0, min_src_range=[0,0], max_src_range=[2,2], min_dst_range=[0,0], max_dst_range=[2,2])
    jobs += build_request(problem="EndpointSharedF", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Node", f_size=14, nfa=2,
                          s_fail=1.0, d_fail=1.0, overlap=1.0, min_src_range=[0,0], max_src_range=[2,2], min_dst_range=[0,0], max_dst_range=[2,2])
    jobs += build_request(problem="EndpointSharedF", seed=seed, num_s=14, num_d=14, num_c=14, fail_type="Both", f_size=35, nfa=1,
                          s_fail=1.0, d_fail=1.0, overlap=1.0, min_src_range=[0,0], max_src_range=[2,2], min_dst_range=[0,0], max_dst_range=[2,2])'''

    return jobs
