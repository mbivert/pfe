/*
 * Copyright (c) 2010 Ecole des Mines de Nantes.
 *
 *      This file is part of Entropy.
 *
 *      Entropy is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      Entropy is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package entropy.plan.choco;

import choco.Choco;
import choco.kernel.solver.Solver;
import choco.kernel.solver.variables.integer.IntDomainVar;
import choco.kernel.solver.variables.set.SetVar;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.SolutionStatistics;
import entropy.plan.SolvingStatistics;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.choco.actionModel.NodeActionModel;
import entropy.plan.choco.actionModel.VirtualMachineActionModel;
import entropy.plan.choco.actionModel.slice.ConsumingSlice;
import entropy.plan.choco.actionModel.slice.DemandingSlice;
import entropy.plan.durationEvaluator.DurationEvaluator;
import gnu.trove.TIntArrayList;

import java.util.List;
import java.util.Set;

/**
 * Specification of a reconfiguration problem.
 *
 * @author Fabien Hermenier
 */
public interface ReconfigurationProblem extends Solver {

    /**
     * The maximum number of group of nodes.
     */
    Integer MAX_NB_GRP = 100;

    /**
     * The maximum duration of a plan.
     */
    Integer MAX_TIME = Choco.MAX_UPPER_BOUND / 10;

    /**
     * Get the current location of a running or a sleeping VM.
     *
     * @param vmIdx the index of the virtual machine
     * @return the node index if exists or -1 if the VM is not already placed
     */
    int getCurrentLocation(int vmIdx);

    /**
     * Get all the nodes in the model. Indexed by their identifier.
     *
     * @return an array of node.
     */
    Node[] getNodes();

    /**
     * Get all the virtual machines in the model. Indexed by their identifier.
     *
     * @return an array of virtual machines.
     */
    VirtualMachine[] getVirtualMachines();

    /**
     * Get the source configuration.
     *
     * @return a configuration
     */
    Configuration getSourceConfiguration();

    /**
     * Get the virtual machines that will be in the running state at the
     * end of the reconfiguration process.
     *
     * @return a set, may be empty
     */
    ManagedElementSet<VirtualMachine> getFutureRunnings();

    /**
     * Get the virtual machines that will be in the waiting state at the
     * end of the reconfiguration process.
     *
     * @return a set, may be empty
     */
    ManagedElementSet<VirtualMachine> getFutureWaitings();

    /**
     * Get the virtual machines that will be in the sleeping state at the
     * end of the reconfiguration process.
     *
     * @return a set, may be empty
     */
    ManagedElementSet<VirtualMachine> getFutureSleepings();

    /**
     * Get the virtual machines that will be in the terminated state at the
     * end of the reconfiguration process.
     *
     * @return a set, may be empty
     */
    ManagedElementSet<VirtualMachine> getFutureTerminated();

    /**
     * Get the nodes that will be in the online state at the
     * end of the reconfiguration process.
     *
     * @return a set, may be empty
     */
    ManagedElementSet<Node> getFutureOnlines();

    /**
     * Get the nodes that will be in the offline state at the
     * end of the reconfiguration process.
     *
     * @return a set, may be empty
     */
    ManagedElementSet<Node> getFutureOfflines();

    /**
     * Get the starting moment of the reconfiguration.
     *
     * @return a variable equals to 0
     */
    IntDomainVar getStart();

    /**
     * Get the end  moment of the reconfiguration
     *
     * @return a variable, should be equals to the last end moment of actions
     */
    IntDomainVar getEnd();

    /**
     * Get the index of a virtual machine
     *
     * @param vm the virtual machine
     * @return its index or -1 in case of failure
     */
    int getVirtualMachine(VirtualMachine vm);

    /**
     * Get the virtual machine with a specified index
     *
     * @param idx the index of the virtual machine
     * @return the virtual machine or null in case of failure
     */
    VirtualMachine getVirtualMachine(int idx);

    /**
     * Get the index of a node
     *
     * @param n the node
     * @return its index or -1 in case of failure
     */
    int getNode(Node n);

    /**
     * Get the node with a specified index
     *
     * @param idx the index of the node
     * @return the node or null in case of failure
     */
    Node getNode(int idx);

    /**
     * Get all the actions related to virtual machines.
     *
     * @return a list of actions.
     */
    List<VirtualMachineActionModel> getVirtualMachineActions();

    /**
     * Get the action associated to a virtual machine.
     *
     * @param vm the virtual machine
     * @return the action associated to the virtual machine.
     */
    VirtualMachineActionModel getAssociatedAction(VirtualMachine vm);

    /**
     * Get the action associated to a virtual machine.
     *
     * @param vmIdx the index of the virtual machine
     * @return the action associated to the virtual machine.
     */
    VirtualMachineActionModel getAssociatedVirtualMachineAction(int vmIdx);

    /**
     * Get all the actions related to nodes.
     *
     * @return a list of actions.
     */
    List<NodeActionModel> getNodeMachineActions();

    /**
     * Get the action associated to a node.
     *
     * @param n the node
     * @return the associated action, may be null
     */
    NodeActionModel getAssociatedAction(Node n);

    /**
     * Get the used CPU capacity of a node.
     *
     * @param n the node
     * @return the used CPU capacity for this node
     */
    IntDomainVar getUsedCPU(Node n);

    /**
     * Get the used memory capacity of a node.
     *
     * @param n the node
     * @return the used memory capacity for this node
     */
    IntDomainVar getUsedMem(Node n);

    /**
     * Get the variable associated to a group of VMs.
     * If the group was not defined, it is created. All the VMs must only belong to one group
     *
     * @param vms the group of virtual machines.
     * @return the variable associated to the group or null if at least one VM of the proposed new group already belong to a group
     */
    IntDomainVar getVMGroup(ManagedElementSet<VirtualMachine> vms);

    /**
     * Make a group variable.
     *
     * @param vms   the VMs involved in the group
     * @param nodes the possible hosting group
     * @return a variable denoting the assignment of the VMs group to one of the group of nodes
     */
    IntDomainVar makeGroup(ManagedElementSet<VirtualMachine> vms, Set<ManagedElementSet<Node>> nodes);

    /**
     * Get the group variable associated to a virtual machine.
     *
     * @param vm the virtual machine
     * @return the group variable if it exists, null otherwise
     */
    IntDomainVar getAssociatedGroup(VirtualMachine vm);

    /**
     * Get all the defined groups of virtual machines.
     *
     * @return a set of group of VMs, may be empty
     */
    Set<ManagedElementSet<VirtualMachine>> getVMGroups();

    /**
     * Get identifier associated to a group of nodes.
     * If the group was not defined, it is created.
     *
     * @param nodes the group to define
     * @return the value associated to the group. -1 if the maximum number of group of nodes has been reached.
     */
    int getGroup(ManagedElementSet<Node> nodes);

    /**
     * Get all the defined groups of nodes.
     *
     * @return a set of group of nodes, may be empty
     */
    Set<ManagedElementSet<Node>> getNodesGroups();

    /**
     * Get the different groups associated to a node.
     *
     * @param n the node
     * @return a list of groups, may be empty
     */
    TIntArrayList getAssociatedGroups(Node n);

    /**
     * Get the group of nodes associated to an identifier.
     *
     * @param idx the identifier
     * @return the group of nodes if it exists, null otherwise
     */
    ManagedElementSet<Node> getNodeGroup(int idx);

    int[] getNodesGroupId();

    /**
     * Get the evaluator to estimate the duration of the actions.
     *
     * @return a DurationEvaluator
     */
    DurationEvaluator getDurationEvaluator();

    /**
     * Get  all the demanding slices in the model.
     *
     * @return a list of slice. May be empty
     */
    List<DemandingSlice> getDemandingSlices();

    /**
     * Get all the consuming slices in the model.
     *
     * @return a list of slice. May be empty
     */
    List<ConsumingSlice> getConsumingSlice();

    /**
     * Get all the actions associated to a list of virtual machines.
     *
     * @param vms the virtual machines
     * @return a list of actions. The order is the same than the order of the VMs.
     */
    List<VirtualMachineActionModel> getAssociatedActions(ManagedElementSet<VirtualMachine> vms);

    /**
     * Get the set model of the nodes.
     * One set per nodes
     *
     * @return an array of set
     */
    SetVar[] getSetModels();

    /**
     * Get the set associated to a node.
     *
     * @param n the node
     * @return the associated set if exists, {@code null} otherwise
     */
    SetVar getSetModel(Node n);

    /**
     * Get the moment the virtual machine is ready to be running.
     *
     * @param vm the virtual machine
     * @return {@code null} if the VM is already ready, a variable otherwise
     */
    IntDomainVar getTimeVMReady(VirtualMachine vm);

    /**
     * Extract the resulting reconfiguration plan if the
     * solving process succeeded.
     *
     * @return a plan if the solving process succeeded or {@code null}
     */
    TimedReconfigurationPlan extractSolution();

    /**
     * Get statistics about computed solutions.
     *
     * @return a list of statistics that may me empty.
     */
    List<SolutionStatistics> getSolutionsStatistics();

    /**
     * Get statistics about the solving process
     *
     * @return some statistics
     */
    SolvingStatistics getSolvingStatistics();

    IntDomainVar getNbHosted(Node n);

    IntDomainVar[] getNbHosted();

    IntDomainVar mult(IntDomainVar left, int right);

    IntDomainVar div(IntDomainVar var, int i);

    IntDomainVar mult(IntDomainVar left, IntDomainVar right);

    IntDomainVar getHostMaxCPU(VirtualMachine vm);

    IntDomainVar getHostUSedCPU(VirtualMachine vm);

    IntDomainVar[] getUsedCPUs();

    /**
     * A variable to indicate whether a node can be online and hosting VMs or not.
     *
     * @param nIdx the node index to check
     * @return a boolean variable. Instantiate to {@code 1} to indicate that the node host VMs
     */
    IntDomainVar isIdle(int nIdx);

}
