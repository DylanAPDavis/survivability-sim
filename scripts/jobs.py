import job


def create_jobs(seed):
    jobs = []
    j1 = job.build_request(seed=seed, topo="NSFnet", routing="Unicast", algorithm="ILP",  num_s=1, num_d=1,
                       use_min_s=1, use_max_s=1, use_min_d=1, use_max_d=1, traffic_combo="none", f_scenario="default",
                       fail_type="Both", nfe=0, overlap="none", s_fail="prevent", d_fail="prevent", ignore="true",
                       num_threads=8, use_aws="true")
    jobs.append(j1)
    return jobs

