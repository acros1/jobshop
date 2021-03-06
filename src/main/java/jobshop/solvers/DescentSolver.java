package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.List;
import java.util.ArrayList;

public class DescentSolver implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static public class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }

        public String toString() {
            String ret = "Machine : " + this.machine + " | First task : " + this.firstTask + " | Last task : " + this.lastTask;
            return ret;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static public class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order) {
            Task toSwap1 = order.tasksByMachine[machine][t1];
            Task toSwap2 = order.tasksByMachine[machine][t2];
            order.tasksByMachine[machine][t1] = toSwap2;
            order.tasksByMachine[machine][t2] = toSwap1;
            //throw new UnsupportedOperationException();
        }
    }

    private Instance instance = null;

    @Override
    public Result solve(Instance instance, long deadline) {
        this.instance = instance;
        // Init = Greedy solver
        GreedySolver greedy = new GreedySolver();
        Schedule bestSched = greedy.solve(instance, deadline).schedule;
        ResourceOrder bestROrder = new ResourceOrder(bestSched);
        
        int nbSol = 0;
        // Time check
        long time = System.currentTimeMillis();
        // Clear display
        // System.out.println();

        while ( System.currentTimeMillis() - time <= deadline ) {
            // For neighbors exploration
            List<Swap> neighbors = new ArrayList<Swap>();
            ResourceOrder bestNeighbor = null;
            List<Block> blockList = blocksOfCriticalPath(bestROrder);

            for ( Block b : blockList ) {
                neighbors = neighbors(b);
                for ( Swap s : neighbors ) {
                    ResourceOrder currentSol = bestROrder.copy();
                    // Creating neighbor
                    s.applyOn(currentSol);
                    // Compare current to best neighbor
                    if ( bestNeighbor != null ) {
                        if ( currentSol.toSchedule() != null ) {
                            if ( currentSol.toSchedule().makespan() < bestNeighbor.toSchedule().makespan() ) {
                                bestNeighbor = currentSol.copy();
                            }
                        }
                        else {
                            System.out.println("currentSol.toSchedule is null");
                        }
                    }
                    else {
                        bestNeighbor = currentSol.copy();
                    }
                }
            }
            // Best neighbor found, compare to the best solution
            if ( bestNeighbor.toSchedule().makespan() < bestSched.makespan() ) {
                bestROrder = bestNeighbor.copy();
                bestSched = bestROrder.toSchedule();
                nbSol++;
                // System.out.println("New best solution ! Iteration : " + nbSol + " | Makespan : " + bestSched.makespan());
            }
            else {
                // System.out.println("There is no better solution !!!!!");
                return new Result(instance, bestSched, Result.ExitCause.Blocked);
            }
        }
        // System.out.println("Time out !!!!! Stop !");
        return new Result(instance, bestSched, Result.ExitCause.Blocked);
    }

    /** Returns a list of all blocks of the critical path. */
    public List<Block> blocksOfCriticalPath(ResourceOrder order) {
        //throw new UnsupportedOperationException();
        List<Block> blockList = new ArrayList<Block>();

        List<Task> path = order.toSchedule().criticalPath();
        List<Task> tempTaskSequence = new ArrayList<Task>();
        
        int currentMachine = order.instance.machine(path.get(0));
        tempTaskSequence.add(path.get(0));

        for (int i = 1 ; i < path.size() ; i++) {
            if ( order.instance.machine(path.get(i)) == currentMachine ) {
                tempTaskSequence.add(path.get(i));
            }
            // else if there is at least two tasks (it's a block) add the block and empty the temp list, change current machine value, add current task to temp list
            else if ( tempTaskSequence.size() > 1 ) {
                int t1 = -1, t2 = -1;
                // find t1
                for (int iT1 = 0 ; iT1 < order.tasksByMachine[currentMachine].length ; iT1++) {
                    if ( tempTaskSequence.get(0).equals(order.tasksByMachine[currentMachine][iT1]) ) {
                        t1 = iT1;
                        break;
                    }
                }

                // find t2
                for (int iT2 = 0 ; iT2 < order.tasksByMachine[currentMachine].length ; iT2++) {
                    if ( tempTaskSequence.get(tempTaskSequence.size()-1).equals(order.tasksByMachine[currentMachine][iT2]) ) {
                        t2 = iT2;
                        break;
                    }
                }
                blockList.add(new Block(currentMachine, t1, t2));
                tempTaskSequence.clear();
                currentMachine = order.instance.machine(path.get(i));
                tempTaskSequence.add(path.get(i));
            }
            // else there is one task, it's not a block, empty the temp list
            else {
                tempTaskSequence.clear();
                currentMachine = order.instance.machine(path.get(i));
                tempTaskSequence.add(path.get(i));
            }

        }
        // Last block treatment
        if ( tempTaskSequence.size() > 1 ) {
            int t1 = -1, t2 = -1;
            // find t1
            for (int iT1 = 0 ; iT1 < order.tasksByMachine[currentMachine].length ; iT1++) {
                if ( tempTaskSequence.get(0).equals(order.tasksByMachine[currentMachine][iT1]) ) {
                    t1 = iT1;
                    break;
                }
            }

            // find t2
            for (int iT2 = 0 ; iT2 < order.tasksByMachine[currentMachine].length ; iT2++) {
                if ( tempTaskSequence.get(tempTaskSequence.size()-1).equals(order.tasksByMachine[currentMachine][iT2]) ) {
                    t2 = iT2;
                    break;
                }
            }
            blockList.add(new Block(currentMachine, t1, t2));
        }
        return blockList;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    public List<Swap> neighbors(Block block) {
        List<Swap> swapList = new ArrayList<Swap>();
        int blockSize = block.lastTask - block.firstTask;
        // there are 2 tasks, 1 neighbor
        if ( blockSize == 1 ) {
            swapList.add(new Swap(block.machine, block.firstTask, block.lastTask));
        }
        // there are 3 or more tasks, two neighbor
        if ( blockSize > 1 ) {
            swapList.add(new Swap(block.machine, block.firstTask, block.firstTask+1)); // swapping two first tasks
            swapList.add(new Swap(block.machine, block.lastTask, block.lastTask-1)); // swapping two last tasks
        }
        return swapList;
        //throw new UnsupportedOperationException();
    }

}
