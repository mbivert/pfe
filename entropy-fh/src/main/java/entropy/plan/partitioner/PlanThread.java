/*
 * Copyright (c) Fabien Hermenier
 *
 *        This file is part of Entropy.
 *
 *        Entropy is free software: you can redistribute it and/or modify
 *        it under the terms of the GNU Lesser General Public License as published by
 *        the Free Software Foundation, either version 3 of the License, or
 *        (at your option) any later version.
 *
 *        Entropy is distributed in the hope that it will be useful,
 *        but WITHOUT ANY WARRANTY; without even the implied warranty of
 *        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *        GNU Lesser General Public License for more details.
 *
 *        You should have received a copy of the GNU Lesser General Public License
 *        along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package entropy.plan.partitioner;

import entropy.configuration.*;
import entropy.plan.*;
import entropy.vjob.DefaultVJob;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.VJob;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrap a solving process into a Thread.
 *
 * @author Fabien Hermenier
 */
public class PlanThread extends Thread {

    private CustomizablePlannerModule m;

    private ManagedElementSet<VirtualMachine> run;
    private ManagedElementSet<VirtualMachine> wait;
    private ManagedElementSet<VirtualMachine> sleep;
    private ManagedElementSet<VirtualMachine> stop;
    private ManagedElementSet<Node> on;
    private ManagedElementSet<Node> off;
    private List<VJob> queue;
    private Configuration cfg;

    private TimedReconfigurationPlan plan;

    private PlanException ex = null;

    private boolean repair;

    private int timeout;

    private long generationTime = -1L;

    private SolvingStatistics solvingStats;

    private List<SolutionStatistics> solutions;

    public void setRepairMode(boolean mode) {
        repair = mode;
    }

    public void setTimeout(int t) {
        this.timeout = t;
    }

    /**
     * Compute a new DefaultTimedReconfigurationPlan that satisfy all the constraints applied to the model.
     *
     * @param part     The partition to use
     * @param src      The source configuration. It must be viable.
     * @param plan     the plan module to use
     * @param allRun   The set of virtual machines that must be running at the end of the process
     * @param allWait  The set of virtual machines that must be waiting at the end of the process
     * @param allSleep The set of virtual machines that must be sleeping at the end of the process
     * @param allStop  The set of virtual machines that must be terminated at the end of the process
     * @param allOn    The set of nodes that must be online at the end of the process
     * @param allOff   The set of nodes that must be offline at the end of the process
     */
    public PlanThread(Partition part,
                      Configuration src,
                      CustomizablePlannerModule plan,
                      ManagedElementSet<VirtualMachine> allRun,
                      ManagedElementSet<VirtualMachine> allWait,
                      ManagedElementSet<VirtualMachine> allSleep,
                      ManagedElementSet<VirtualMachine> allStop,
                      ManagedElementSet<Node> allOn,
                      ManagedElementSet<Node> allOff) throws ConfigurationsException, PlanException {

        //Divide the original configuration
        //System.exit(1);
        cfg = Configurations.subConfiguration(src, part.getVirtualMachines(), part.getNodes());
        solutions = new ArrayList<SolutionStatistics>();
        //Divide the vms
        this.run = new SimpleManagedElementSet<VirtualMachine>();
        this.wait = new SimpleManagedElementSet<VirtualMachine>();
        this.sleep = new SimpleManagedElementSet<VirtualMachine>();
        this.stop = new SimpleManagedElementSet<VirtualMachine>();

        for (VirtualMachine vm : part.getVirtualMachines()) {
            if (allRun.contains(vm)) {
                this.run.add(vm);
            } else if (allWait.contains(vm)) {
                this.wait.add(vm);
            } else if (allSleep.contains(vm)) {
                this.sleep.add(vm);
            } else if (allStop.contains(vm)) {
                this.stop.add(vm);
            }
        }
        this.on = new SimpleManagedElementSet<Node>();
        this.off = new SimpleManagedElementSet<Node>();
        for (Node n : part.getNodes()) {
            if (allOn.contains(n)) {
                this.on.add(n);
            } else if (allOff.contains(n)) {
                this.off.add(n);
            }
        }

        m = plan;
        VJob vj = new DefaultVJob(getRunID());
        for (PlacementConstraint x : part.getConstraints()) {
            vj.addConstraint(x);
        }
        queue = new ArrayList<VJob>();
        queue.add(vj);
    }

    /**
     * Get statistics of the computed solutions
     *
     * @return a list of statistics thatmay be empty
     */
    public List<SolutionStatistics> getSolutionsStatistics() {
        if (m != null) {
            return m.getSolutionsStatistics();
        }
        return solutions;
    }

    /**
     * Get the statistics about the solving process
     *
     * @return some statistics
     */
    public SolvingStatistics getSolvingStatistics() {
        if (m != null) {
            return m.getSolvingStatistics();
        }
        return solvingStats;
    }

    @Override
    public void run() {
        //ChocoLogging.setVerbosity(Verbosity.SOLUTION);
        try {
            /*m.setRepairMode(repair);
            m.setTimeLimit(timeout);*/
            plan = m.compute(cfg, run, wait, sleep, stop, on, off, queue);

            //Copy statistics to liberate memory more easily (no more references to the problem)
            //solvingStats = m.getSolvingStatistics().clone();
            solutions.clear();
            for (SolutionStatistics st : m.getSolutionsStatistics()) {
                solutions.add(st.clone());
            }
        } catch (PlanException e) {
            this.ex = e;
        } finally {
            solvingStats = m.getSolvingStatistics().clone();
            generationTime = m.getGenerationTime();
            m = null;
        }
    }

    /**
     * Get the exception that may occurred during the solving process
     *
     * @return an exception or {@code null} if no exception occurred
     */
    public PlanException getException() {
        return ex;
    }

    /**
     * Get the computed plan
     *
     * @return a plan or {@code null} if no plan was computed
     */
    public TimedReconfigurationPlan getResultingPlan() {
        return plan;
    }

    /**
     * Get the id of the solving process.
     *
     * @return a String
     */
    public String getRunID() {
        return new StringBuilder("Partition").toString();
    }

    public String toString() {
        return getRunID();
    }

    public long getGenerationTime() {
        return generationTime;
    }
}
