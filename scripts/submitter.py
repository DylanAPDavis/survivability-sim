import subprocess
import jobs
import time

analysis_job = "ANALYSIS_JOB"
analysis_after_sim = "ANALYSIS_AFTER_SIM"
analysis_none = "ANALYSIS_NONE"
aggregate_analysis = True


def process_job(job, analysis_type):
    if analysis_type == analysis_job:
        output_file_path = "results/output/" + "analyze_" + job.request_id
        run_time = "0:30"
        memory = "700"
        command_input = ["bsub", "-q", "short", "-W", run_time, "-R", "rusage[mem=" + memory + "] span[hosts=1]", "-n",
                         str(8), "-o", output_file_path, "python", "scripts/run_analysis.py", job.request_id,
                         str(job.use_aws)]
    else:
        output_file_path = "results/output/" + job.request_id
        run_time = "3:59"
        memory = "6000"
        queue = "short"
        if job.algorithm == "ilp" and job.f_scenario != "default" and job.ignore == "false":
            memory = "8000"
            if job.f_scenario == "alllinks" and job.topo == "tw" and job.nfe == 3:
                memory = "16000"

        command_input = ["bsub", "-q", queue, "-W", run_time, "-R", "rusage[mem=" + memory + "] span[hosts=1]", "-n",
                         str(job.num_threads), "-o", output_file_path, "python", "scripts/run_simulation.py"]
        command_input += job.ordered_params
        command_input.append(job.use_aws)
        command_input.append(job.request_id)
        if analysis_type == analysis_after_sim:
            command_input.append("true")
        else:
            command_input.append("false")
    process = subprocess.Popen(command_input, stdout=subprocess.PIPE, universal_newlines=True)


def process_aggregate_job():
    output_file_path = "results/output/" + "aggregate"
    run_time = "3:59"
    memory = "16000"
    command_input = ["bsub", "-q", "short", "-W", run_time, "-R", "rusage[mem=" + memory + "] span[hosts=1]", "-n",
                     str(8), "-o", output_file_path, "python", "scripts/run_aggregate.py"]

if aggregate_analysis:
    process_aggregate_job()
else:
    seeds = range(1, 31)
    for seed in seeds:
        job_list = jobs.create_jobs(seed)
        for j in job_list:
            print(str(j.__dict__))
            process_job(j, analysis_after_sim)
        time.sleep(2)
