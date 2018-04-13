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
        run_time = "36:00"
        memory = "6000"
        queue = "long"
        if job.algorithm == "ilp" and job.f_scenario != "default" and job.ignore == "false":
            memory = "12000"
            if job.f_scenario == "alllinks" and job.topo == "tw" and job.nfe == 2:
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
    #print("Running job list. Ids stored in scripts/input/first_id_mass.txt")
    first_job = job_list[0]
    output_file_path = "results/output/mass_process_" + first_job.request_id
    run_time = "3:59"
    memory = "4000"
    queue = "short"
    param_dicts = []
    for job_details in job_list:
        args = [arg for arg in job_details.ordered_params]
        args.append(job_details.use_aws)
        args.append(job_details.request_id)
        param_dicts.append(run_simulation.build_param_dict(args))
    params_string = json.dumps(param_dicts)
    filepath = 'scripts/input/' + first_job.request_id + '_mass.txt'
    with open(filepath, "w+") as fp:
        fp.write(params_string)
    command_input = ["bsub", "-q", queue, "-W", run_time, "-R", "rusage[mem=" + memory + "] span[hosts=1]", "-n",
                     str(job_list[0].num_threads), "-o", output_file_path, "python", "scripts/run_mass_simulation.py",
                     first_job.request_id]
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
    print("Rerunning jobs in scripts/Rerun.txt")
    with open(filepath) as fp:
        for line in fp:
            line = line.strip("\n").strip("\r")
            params = line.split("_")
            if len(params) > 2:
                output_file_path = "results/output/" + line
                run_time = "36:00"
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


def analyze_jobs(analysis_seeds, topologies, routings):
    for topology in topologies:
        for routing in routings:
            output_file_path = "results/output/" + "mass_analyze" + str(analysis_seeds).replace(" ", "") + "_" + topology + "_" + routing
            run_time = "3:59"
            memory = "8000"
            command_input = ["bsub", "-q", "short", "-W", run_time, "-R",
                             "rusage[mem=" + memory + "] span[hosts=1]", "-n", str(12), "-o", output_file_path,
                             "python", "scripts/run_mass_analysis.py", json.dumps(analysis_seeds), topology, routing]
            process = subprocess.Popen(command_input, stdout=subprocess.PIPE, universal_newlines=True)

if aggregate_analysis:
    process_aggregate_job()
elif rerun:
    rerun_jobs()
elif mass_analysis:
    seeds = range(1, 31)
    topologies = ["tw"]
    routings = ["anycast"]
    analyze_jobs(seeds, topologies, routings)
elif mass_sim:
    seeds = range(1, 31)
    #topology = "tw"
    #routing = "manytomany"
    #algorithm = "cyclefortwo"
    #nfe = 1
    routing_types = ["anycast", "manytomany"]
    failure_scenarios = ["alllinks", "quake2"]
    nfe_values = [1, 2]
    topologies = ["nsfnet", "tw"]
    algorithm_dict = {
        "anycast": ["minimumcost", "bhandari"],#["flexbhandari", "minimumcost", "minimumrisk", "bhandari", "tabu", "survivablehub"],
        "manytomany": ["minimumcost"]#["flexbhandari", "minimumcost", "memberforwarding", "cyclefortwo", "tabu", "survivablehub"],
    }
    regular_filter = True
    all_seeds = True
    if all_seeds:
        for routing in routing_types:
            for nfe in nfe_values:
                for topology in topologies:
                    for algorithm in algorithm_dict[routing]:
                        for f_scenario in failure_scenarios:
                            job_list = []
                            for seed in seeds:
                                jobs_for_seed = jobs.create_jobs(seed)
                                job_list += [job for job in jobs_for_seed if job.topo == topology and job.routing == routing
                                             and job.algorithm == algorithm and job.nfe == nfe and job.f_scenario == f_scenario]
                            print(routing + "_" + f_scenario + "_" + str(nfe) + "_" + topology + "_" + algorithm + ": " + str(len(job_list)))
                            mass_process_jobs(job_list)
    else:
        for seed in seeds:
            if regular_filter:
                jobs_for_seed = jobs.create_jobs(seed)
                job_list = [job for job in jobs_for_seed if job.topo == topology and job.routing == routing
                            and job.algorithm == algorithm and nfe == nfe]
                mass_process_jobs(job_list)
                print(len(job_list))
            else:
                job_list = [job for job in jobs_for_seed if job.topo == topology and job.routing == routing
                            and job.algorithm == algorithm and job.nfe == nfe]
                print(len(job_list))
                mass_process_jobs(job_list)
                single_job_list = [job for job in jobs_for_seed if job.topo == topology and job.routing == routing
                                   and job.algorithm == algorithm and job.nfe != nfe]
                print(len(single_job_list))
                for j in single_job_list:
                    print(str(j.__dict__))
                    process_job(j, analysis_after_sim)
                time.sleep(2)
else:
    seeds = range(1, 31)
    for seed in seeds:
        job_list = jobs.create_jobs(seed)
        for j in job_list:
            print(str(j.__dict__))
            process_job(j, analysis_after_sim)
            time.sleep(2)
