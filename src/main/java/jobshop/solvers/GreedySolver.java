package jobshop.solvers;

import java.util.ArrayList;
import java.util.Arrays;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

public class GreedySolver implements Solver {

    private ArrayList<Task> feasible_tasks = new ArrayList<Task>();
    private ArrayList<Task> done_tasks = new ArrayList<Task>();

    private Instance instance = null;

    private int[] date_per_machine = null;
    // Array to sotck each task LRPT
    private int[] tasks_LRPT = null;

    @Override
    public Result solve(Instance instance, long deadline) {

        this.instance = instance;
        ResourceOrder sol = new ResourceOrder(instance);

        this.date_per_machine = new int[instance.numMachines];
        this.tasks_LRPT = new int[instance.numJobs*instance.numTasks];
        Arrays.fill(tasks_LRPT, -1);

        // Init
        // Getting first task of each job
        for(int j = 0 ; j < instance.numJobs ; j++) {
            feasible_tasks.add(new Task(j, 0));
        }

        // Loop
        while (feasible_tasks.size() != 0) {
            // Get task and put it on the needed resource
            Task chosen_task = get_LRPT();
            // Get the needed resource
            int machine = instance.machine(chosen_task.job, chosen_task.task);
            // Adding task to resouce order
            sol.tasksByMachine[machine][sol.nextFreeSlot[machine]] = chosen_task;
            // Increment date per machine
            date_per_machine[machine] += instance.duration(chosen_task);
            // Increment nextFreeSlot pointer
            sol.nextFreeSlot[machine]++;
            // Adding task to done tasks list
            done_tasks.add(chosen_task);
            // Update feasible task list
            update_feasible_tasks();
        }

        return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
    }

    public Task get_SPT() {

        Task shortest_task = feasible_tasks.get(0);
        for (int i = 1 ; i < feasible_tasks.size() ; i++) {

            Task current_task = feasible_tasks.get(i);
            int current_task_duration = instance.duration(current_task.job, current_task.task);
            int shortest_task_duration = instance.duration(shortest_task.job, shortest_task.task);

            if ( current_task_duration <= shortest_task_duration ) {
                shortest_task = current_task;
            }
        }
        return shortest_task;

    }

    public Task get_LRPT() {

        Task task = null;
        int current_job_duration = 0;
        int longest_job_duration = -1;
        int longest_job_index = -1;

        for (int i = 0 ; i < instance.numJobs ; i++) {
            for (int j = 0 ; j < instance.numTasks ; j++) {
                if ( !(done_tasks.contains(new Task(i, j))) ) {
                    current_job_duration += instance.duration(i, j);
                }
            }
            if ( current_job_duration > longest_job_duration ) {
                longest_job_duration = current_job_duration;
                longest_job_index = i;
            }
            current_job_duration = 0;
        }

        for (int i = 0 ; i < feasible_tasks.size() ; i++) {
            if (feasible_tasks.get(i).job == longest_job_index) {
                task = feasible_tasks.get(i);
            }
        }
        return task;

    }

    public Task get_EST_LRPT() {
        
        Task shortest_task = feasible_tasks.get(0);
        Task current_task = null;

        for (int i = 1 ; i < feasible_tasks.size() ; i++) {
            current_task = feasible_tasks.get(i);

            // getting LRPT of current_task
            if ( tasks_LRPT[current_task.job*instance.numTasks+current_task.task] == -1 ) {
                tasks_LRPT[current_task.job*instance.numTasks+current_task.task] = 0;
                for (int k = current_task.task ; k < instance.numTasks ; k++) {
                    tasks_LRPT[current_task.job*instance.numTasks+current_task.task] += instance.duration(current_task.job, k);
                } 
            }

            if ( date_per_machine[instance.machine(shortest_task)] == date_per_machine[instance.machine(current_task)] 
                    && tasks_LRPT[shortest_task.job*instance.numTasks] < tasks_LRPT[current_task.job*instance.numTasks] ) {
                shortest_task = current_task;
            }
            else if ( date_per_machine[instance.machine(shortest_task)] < date_per_machine[instance.machine(current_task)] ) {
                shortest_task = current_task;
            }
        }
        return shortest_task;
    }

    public void update_feasible_tasks() {

        ArrayList<Task> result = new ArrayList<Task>();

        for (int i = 0 ; i < instance.numTasks ; i++) {
            for (int j = 0 ; j < instance.numJobs ; j++) {
                if ( !(done_tasks.contains(new Task(j, i))) ) {
                    // Task is not realised yet
                    if ( (i == 0) || (done_tasks.contains(new Task(j, (i-1)))) ) {
                        // Predecessor task is already realised or there is no predecessor task
                        result.add(new Task(j,i));
                    }
                }
            }
        } 

        this.feasible_tasks = result;

    }

}