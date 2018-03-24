import subprocess
import jobs
import time
import run_simulation
import json
from launch import launch_simulator

analysis_job = "ANALYSIS_JOB"
analysis_after_sim = "ANALYSIS_AFTER_SIM"
analysis_none = "ANALYSIS_NONE"
aggregate_analysis = False
rerun = False
mass_analysis = False
mass_sim = True


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


def mass_process_jobs(job_list):
    output_file_path = "results/output/mass_process_" + job_list[0].request_id
    run_time = "3:59"
    memory = "6000"
    queue = "short"
    param_dicts = []
    for job in job_list:
        args = [arg for arg in job.ordered_params]
        args.append(job.use_aws)
        args.append(job.request_id)
        param_dicts.append(run_simulation.build_param_dict(args))
    params_string = json.dumps(param_dicts)
    filepath = 'scripts/mass.txt'
    with open(filepath, "w+") as fp:
        fp.write(params_string)
    command_input = ["bsub", "-q", queue, "-W", run_time, "-R", "rusage[mem=" + memory + "] span[hosts=1]", "-n",
                     str(job_list[0].num_threads), "-o", output_file_path, "python", "scripts/run_mass_simulation.py"]
    process = subprocess.Popen(command_input, stdout=subprocess.PIPE, universal_newlines=True)


def process_aggregate_job():
    output_file_path = "results/output/" + "aggregate"
    run_time = "3:59"
    memory = "8000"
    command_input = ["bsub", "-q", "short", "-W", run_time, "-R", "rusage[mem=" + memory + "] span[hosts=1]", "-n",
                     str(12), "-o", output_file_path, "python", "scripts/run_aggregate.py"]
    process = subprocess.Popen(command_input, stdout=subprocess.PIPE, universal_newlines=True)


# 12_tw_manytomany_ilp_5_3_5_5_1_3_none_alllinks_both_2_none_allow_allow_false_8
def rerun_jobs():
    filepath = 'scripts/Rerun.txt'
    with open(filepath) as fp:
        for line in fp:
            line = line.strip("\n").strip("\r")
            params = line.split("_")
            if len(params) > 2:
                output_file_path = "results/output/" + line
                run_time = "6:00"
                memory = "12000"
                queue = "long"
                command_input = ["bsub", "-q", queue, "-W", run_time, "-R", "rusage[mem=" + memory + "] span[hosts=1]", "-n",
                                 str(params[-1]), "-o", output_file_path, "python", "scripts/run_simulation.py"]
                command_input += params
                command_input.append("true")
                command_input.append(line)
                command_input.append("true")
                #print(command_input)
                process = subprocess.Popen(command_input, stdout=subprocess.PIPE, universal_newlines=True)


def analyze_jobs(seeds, topologies, routings):
    for seed in seeds:
        for topology in topologies:
            for routing in routings:
                output_file_path = "results/output/" + "mass_analyze" + seed + "_" + topology + "_" + routing
                run_time = "3:59"
                memory = "8000"
                command_input = ["bsub", "-q", "short", "-W", run_time, "-R",
                                 "rusage[mem=" + memory + "] span[hosts=1]", "-n", str(12), "-o", output_file_path,
                                 "python", "scripts/run_mass_analysis.py", seed, topology, routing]
                process = subprocess.Popen(command_input, stdout=subprocess.PIPE, universal_newlines=True)

if aggregate_analysis:
    process_aggregate_job()
elif rerun:
    rerun_jobs()
elif mass_analysis:
    seeds = range(1,31)
    topologies = ["tw"]
    routings = ["manytomany"]
    analyze_jobs(seeds, topologies, routings)
elif mass_sim:
    seeds = range(1,31)
    topology = "tw"
    routing = "manytomany"
    algorithm = "tabu"
    job_list = []
    for seed in seeds:
        jobs_for_seed = jobs.create_jobs(seed)
        job_list += [job for job in jobs_for_seed if job.topo == topology and job.routing == routing and job.algorithm == algorithm]
    mass_process_jobs(job_list)
else:
    seeds = range(1, 31)
    for seed in seeds:
        job_list = jobs.create_jobs(seed)
        for j in job_list:
            print(str(j.__dict__))
            process_job(j, analysis_job)
        time.sleep(2)
