import subprocess
import jobs


def process_job(job, analyze):
    '''output_file_path = "results/output/" + "_".join(
        [str(job.seed), job.topo, job.problem, job.objective, job.algorithm, str(job.num_r), str(job.num_s),
         str(job.num_d), str(job.num_c), str(job.min_range), str(job.max_range), str(job.min_src_range),
         str(job.max_src_range), str(job.min_dst_range), str(job.max_dst_range),
         str(job.reach_min_s), str(job.reach_max_s), str(job.reach_min_d), str(job.reach_max_d), str(job.f_size),
         str(job.fail_range), job.fail_type, str(job.fail_prob), str(job.prob_range), str(job.nfa),
         str(job.nfa_range), job.processing, str(job.overlap), str(job.s_fail), str(job.d_fail), str(job.sdn).lower(),
         str(job.use_aws).lower(), str(job.ignore).lower(), str(job.num_threads)]).replace(" ", "") '''
    output_file_path = "results/output/" + job.request_set_id
    run_time = "3:59" if (job.f_size >= 14 and not job.ignore) else "0:30"
    memory = "700"
    if job.f_size >= 14 and not job.ignore:
        if job.nfa >= 2:
            memory = "3500"
            if job.problem == "Endpoint":
                memory = "4000"
        elif job.num_s >= 14 or job.num_d >= 14:
            memory = "2700"
        else:
            memory = "1500"
    command_input = ["bsub", "-q", "short", "-W", run_time, "-R", "rusage[mem=" + memory + "] span[hosts=1]", "-n",
                     str(job.num_threads), "-o", output_file_path, "python", "scripts/run_simulation.py"]
    command_input += job.ordered_params
    command_input.append(job.request_set_id)
    if analyze:
        command_input.append("true")
    else:
        command_input.append("false")
    process = subprocess.Popen(command_input, stdout=subprocess.PIPE, universal_newlines=True)


# seeds = range(1, 31)
seeds = [1]
for seed in seeds:
    jobs = jobs.create_orig_run_jobs(seed)
    process_job(jobs[0], True)
    #for job in jobs:
    #    process_job(job)
