package jobshop;

import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.solvers.DescentSolver;

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.nio.file.Paths;

public class DebuggingMain {

    public static void main(String[] args) {
        try {
            // load the aaa1 instance
            Instance instance = Instance.fromFile(Paths.get("instances/aaa1"));

            // construit une solution dans la représentation par
            // numéro de jobs : [0 1 1 0 0 1]
            // Note : cette solution a aussi été vue dans les exercices (section 3.3)
            //        mais on commençait à compter à 1 ce qui donnait [1 2 2 1 1 2]
            JobNumbers enc = new JobNumbers(instance);
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 1;
            enc.jobs[enc.nextToSet++] = 1;
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 1;

            System.out.println("\nENCODING: " + enc);

            Schedule sched = enc.toSchedule();
            // TODO: make it print something meaningful
            // by implementing the toString() method
            System.out.println("SCHEDULE: " + sched);
            System.out.println("VALID: " + sched.isValid());
            System.out.println("MAKESPAN: " + sched.makespan());

            // Testing descent methods
            ResourceOrder rOrder = new ResourceOrder(sched);
            //System.out.println(rOrder);
            //System.out.println("---------------------------------------");
            DescentSolver solver = new DescentSolver();
            List<DescentSolver.Block> path = solver.blocksOfCriticalPath(rOrder);
            List<DescentSolver.Swap> neighbors;
            for ( DescentSolver.Block b : path) {
                neighbors = solver.neighbors(b);
                for ( DescentSolver.Swap s : neighbors ) {
                    s.applyOn(rOrder);
                }
            }
            //System.out.println(rOrder);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
}
