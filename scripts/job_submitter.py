import subprocess
import jobs

analysis_job = "ANALYSIS_JOB"
analysis_after_sim = "ANALYSIS_AFTER_SIM"
analysis_none = "ANALYSIS_NONE"


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
        command_input = ["bsub", "-q", "short", "-W", run_time, "-R", "rusage[mem=" + memory + "] span[hosts=1]", "-n",
                         str(job.num_threads), "-o", output_file_path, "python", "scripts/run_simulation.py"]
        command_input += job.ordered_params
        command_input.append(job.request_id)
        if analysis_type == analysis_after_sim:
            command_input.append("true")
        else:
            command_input.append("false")
    process = subprocess.Popen(command_input, stdout=subprocess.PIPE, universal_newlines=True)


# seeds = range(1, 31)
seeds = [1]
for seed in seeds:
    jobs = jobs.create_jobs(seed)
    for job in jobs:
        process_job(job, analysis_after_sim)
