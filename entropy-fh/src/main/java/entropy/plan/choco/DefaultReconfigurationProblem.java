/*
 * Copyright (c) Fabien Hermenier
 *
 * This file is part of Entropy.
 *
 * Entropy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Entropy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package entropy.plan.choco;

import choco.cp.solver.CPSolver;
import choco.cp.solver.constraints.global.BoundGccVar;
import choco.cp.solver.constraints.integer.Element;
import choco.cp.solver.constraints.integer.ElementV;
import choco.cp.solver.constraints.integer.EuclideanDivisionXYZ;
import choco.cp.solver.constraints.integer.TimesXYZ;
import choco.cp.solver.constraints.reified.ReifiedFactory;
import choco.cp.solver.constraints.set.InverseSetInt;
import choco.kernel.common.util.tools.ArrayUtils;
import choco.kernel.solver.Solution;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.search.measure.IMeasures;
import choco.kernel.solver.variables.integer.IntDomainVar;
import choco.kernel.solver.variables.set.SetVar;
import entropy.configuration.*;
import entropy.plan.*;
import entropy.plan.action.Action;
import entropy.plan.choco.actionModel.*;
import entropy.plan.choco.actionModel.slice.ConsumingSlice;
import entropy.plan.choco.actionModel.slice.DemandingSlice;
import entropy.plan.choco.actionModel.slice.Slices;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSliceHeights;
import entropy.plan.durationEvaluator.DurationEvaluationException;
import entropy.plan.durationEvaluator.DurationEvaluator;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A CSP to model a reconfiguration plan composed of time bounded actions.
 * In this model, regarding to the current configuration and the sample destination configuration,
 * the model create the different actions that aims to perform the transition to the destination configuration. In addition,
 * several actions acting on the placement of the virtual machines can be added.
 *
 * @author Fabien Hermenier
 */
public class DefaultReconfigurationProblem extends CPSolver implements ReconfigurationProblem {

    private static final Logger logger = LoggerFactory
            .getLogger(DefaultReconfigurationProblem.class);

    private ManagedElementSet<VirtualMachine> manageable;

    /**
     * The maximum number of group of nodes.
     */
    public static final Integer MAX_NB_GRP = 1000;

    /**
     * The moment the reconfiguration starts. Equals to 0.
     */
    private IntDomainVar start;

    /**
     * The moment the reconfiguration ends. Variable.
     */
    private IntDomainVar end;

    /**
     * All the virtual machines' action to perform that implies regular actions.
     */
    private List<VirtualMachineActionModel> vmActions;

    /**
     * All the actions of the nodes that manage their state.
     */
    private List<NodeActionModel> nodesActions;

    /**
     * The source configuration.
     */
    private Configuration source;

    /**
     * The current location of the placed VMs.
     */
    private int[] currentLocation;

    /**
     * The future running VMs.
     */
    private ManagedElementSet<VirtualMachine> runnings;

    /**
     * The future waiting VMs.
     */
    private ManagedElementSet<VirtualMachine> waitings;

    /**
     * The future sleeping VMs.
     */
    private ManagedElementSet<VirtualMachine> sleepings;

    /**
     * The future terminated VMs.
     */
    private ManagedElementSet<VirtualMachine> terminated;

    /**
     * The future online nodes.
     */
    private ManagedElementSet<Node> onlines;

    /**
     * The future offline nodes.
     */
    private ManagedElementSet<Node> offlines;

    /**
     * All the nodes managed by the model.
     */
    private Node[] nodes;


    private TIntIntHashMap revNodes;

    /**
     * A set model for each node.
     */
    private SetVar[] sets;

    /**
     * Cpu usage indexed by the index of the node.
     */
    private IntDomainVar[] cpuUsages;

    /**
     * Mem usage indexed by the index of the node.
     */
    private IntDomainVar[] memCapacities;

    /**
     * All the virtual machines managed by the model.
     */
    private VirtualMachine[] vms;

    private TIntIntHashMap revVMs;
    /**
     * The duration evaluator.
     */
    private DurationEvaluator durationEval;

    /**
     * The group variable associated to each virtual machine.
     */
    private List<IntDomainVar> vmGrp;

    /**
     * The group variable associated to each group of VMs.
     */
    private Map<ManagedElementSet<VirtualMachine>, IntDomainVar> vmsGrp;

    /**
     * The value associated to each group of nodes.
     */
    private Map<ManagedElementSet<Node>, Integer> nodesGrp;

    /**
     * The groups associated to each node.
     */
    private List<TIntArrayList> nodeGrps;

    /**
     * The group of nodes associated to each identifier. To synchronize with nodesGrp.
     */
    private List<ManagedElementSet<Node>> revNodesGrp;

    /**
     * The next value to use when creating a nodeGrp.
     */
    private int nextNodeGroupVal = 0;

    /**
     * All the consuming slices in the model.
     */
    private List<ConsumingSlice> consumingSlices;

    /**
     * All the demanding slices in the model.
     */
    private List<DemandingSlice> demandingSlices;

    /**
     * The moments each VM is ready.
     */
    private IntDomainVar[] momentVMReady;

    private SatisfyDemandingSliceHeights packingConstraint;

    private int[] grpId; //The group ID of each node

    /**
     * Build a reconfiguration problem. All the VMs are candidate
     * for management
     *
     * @param src   the source configuration
     * @param run   the virtual machines to run
     * @param wait  the virtual machines that will stay waiting
     * @param sleep the virtual machines to turn into the sleeping state
     * @param stop  the virtual machines to stop
     * @param on    the nodes to turn on
     * @param off   the nodes to turn off
     * @param eval  the duration evaluator
     * @throws PlanException if an error occurs
     */
    public DefaultReconfigurationProblem(Configuration src,
                                         ManagedElementSet<VirtualMachine> run,
                                         ManagedElementSet<VirtualMachine> wait,
                                         ManagedElementSet<VirtualMachine> sleep,
                                         ManagedElementSet<VirtualMachine> stop,
                                         ManagedElementSet<Node> on,
                                         ManagedElementSet<Node> off,
                                         DurationEvaluator eval) throws PlanException {
        this(src, run, wait, sleep, stop, src.getAllVirtualMachines(), on, off, eval);

    }

    /**
     * Make a new model.
     *
     * @param src        The source configuration. It must be viable.
     * @param run        The set of virtual machines that must be running at the end of the process
     * @param wait       The set of virtual machines that must be waiting at the end of the process
     * @param sleep      The set of virtual machines that must be sleeping at the end of the process
     * @param stop       The set of virtual machines that must be terminated at the end of the process
     * @param manageable the set of virtual machines to consider as manageable in the problem
     * @param on         The set of nodes that must be online at the end of the process
     * @param off        The set of nodes that must be offline at the end of the process
     * @param eval       the evaluator to estimate the duration of an action.
     * @throws entropy.plan.PlanException if an error occurred while building the model
     */
    public DefaultReconfigurationProblem(Configuration src,
                                         ManagedElementSet<VirtualMachine> run,
                                         ManagedElementSet<VirtualMachine> wait,
                                         ManagedElementSet<VirtualMachine> sleep,
                                         ManagedElementSet<VirtualMachine> stop,
                                         ManagedElementSet<VirtualMachine> manageable,
                                         ManagedElementSet<Node> on,
                                         ManagedElementSet<Node> off,
                                         DurationEvaluator eval) throws PlanException {
        source = src;
        this.manageable = manageable;
        runnings = run;
        waitings = wait;
        sleepings = sleep;
        terminated = stop;
        onlines = on;
        offlines = off;
        durationEval = eval;

        checkDisjointSet();
        if (!Configurations.currentlyOverloadedNodes(source).isEmpty()) {
            for (Node n : Configurations.currentlyOverloadedNodes(source)) {
                System.err.println(n + ": " + source.getRunnings(n));
                for (VirtualMachine vm : source.getRunnings(n)) {
                    System.err.print(vm + " ");
                }
                System.err.println();
                System.err.println(n.getCPUCapacity() - ManagedElementSets.sum(source.getRunnings(n), ResourcePicker.VMRc.cpuConsumption)[0]);
                System.err.println(n.getMemoryCapacity() - ManagedElementSets.sum(source.getRunnings(n), ResourcePicker.VMRc.memoryConsumption)[0]);
            }
            System.err.println(source.getOfflines().size() + " offline(s); " + source.getWaitings().size() + "waitings");
            throw new NonViableSourceConfigurationException(source, Configurations.currentlyOverloadedNodes(source).get(0));
        }

        start = this.makeConstantIntVar(0);
        end = createBoundIntVar("end", 0, MAX_TIME);
        post(geq(end, start));

        ManagedElementSet<VirtualMachine> all = source.getAllVirtualMachines().clone();
        //We have to add the future waiting and running VMs as they have to be managed through an Instantiate action
        all.addAll(wait);
        all.addAll(runnings);
        vms = all.toArray(new VirtualMachine[all.size()]);
        revVMs = new TIntIntHashMap(vms.length);
        for (int i = 0; i < vms.length; i++) {
            revVMs.put(vms[i].hashCode(), i);
        }
        ManagedElementSet<Node> ns = source.getAllNodes();
        nodes = ns.toArray(new Node[ns.size()]);
        grpId = new int[ns.size()];
        revNodes = new TIntIntHashMap(ns.size());
        for (int i = 0; i < nodes.length; i++) {
            revNodes.put(nodes[i].hashCode(), i);
        }

        makeResourcesCapacities();

        try {
            makeBasicActions();
        } catch (DurationEvaluationException e) {
            throw new PlanException(e.getMessage(), e);
        }


        vmGrp = new ArrayList<IntDomainVar>(vms.length);
        for (int i = 0; i < vms.length; i++) {
            vmGrp.add(i, null);
        }
        vmsGrp = new HashMap<ManagedElementSet<VirtualMachine>, IntDomainVar>();
        nodeGrps = new ArrayList<TIntArrayList>(nodes.length);
        for (int i = 0; i < nodes.length; i++) {
            nodeGrps.add(i, new TIntArrayList());
        }
        nodesGrp = new HashMap<ManagedElementSet<Node>, Integer>();
        revNodesGrp = new ArrayList<ManagedElementSet<Node>>(MAX_NB_GRP);
    }

    @Override
    public int getCurrentLocation(int vmIdx) {
        if (vmIdx >= 0 && vmIdx < vms.length) {
            return currentLocation[vmIdx];
        }
        return -1;
    }

    /**
     * Make a set model. On set per node, that indicates the VMs it will run
     */
    private void makeSetModel() {
        if (sets == null) {
            //A set variable for each future online nodes
            sets = new SetVar[nodes.length];


            for (int i = 0; i < sets.length; i++) {
                Node n = nodes[i];
                SetVar s = createEnumSetVar("host(" + n.getName() + ")", 0, demandingSlices.size() - 1);
                sets[i] = s;
            }

            //Make the channeling with the assignment variable of all the d-slices
            IntDomainVar[] assigns = Slices.extractHosters(demandingSlices);
            post(new InverseSetInt(assigns, sets));
        }
    }


    @Override
    public IntDomainVar getTimeVMReady(VirtualMachine vm) {
        int idx = getVirtualMachine(vm);
        return momentVMReady[idx];
    }

    /**
     * Set the resources capacity of the nodes.
     */
    private void makeResourcesCapacities() {
        cpuUsages = new IntDomainVar[nodes.length];
        cpuMax = new int[nodes.length];
        memCapacities = new IntDomainVar[nodes.length];

        for (Node n : source.getAllNodes()) {
            int maxCPUCapa = n.getCPUCapacity();
            IntDomainVar capaCPU;
            IntDomainVar capaMem;
            if (offlines.contains(n)) { //The node will be online so all the resources will be used
                capaCPU = makeConstantIntVar(n.getCPUCapacity());
                capaMem = makeConstantIntVar(n.getMemoryCapacity());
            } else {
                capaCPU = createBoundIntVar(n.getName() + "#cpuCapacity", 0, n.getCPUCapacity());
                capaMem = createBoundIntVar(n.getName() + "#memCapacity", 0, n.getMemoryCapacity());
            }
            cpuUsages[getNode(n)] = capaCPU;
            cpuMax[getNode(n)] = maxCPUCapa;
            memCapacities[getNode(n)] = capaMem;
            if (maxCPUCapa > maxHostCPUCapacity) {
                maxHostCPUCapacity = maxCPUCapa;
            }
        }
    }

    int maxHostCPUCapacity = -1;

    /**
     * the array of max cpu capacity, indexed by the nodes
     */
    private int[] cpuMax;

    /**
     * @return the maximum CPU capacity of a registered host wanted to be online.
     */
    public int getMaxHostCPUCapacity() {
        return maxHostCPUCapacity;
    }

    /**
     * Check all the nodes belong to only on set.
     *
     * @throws entropy.plan.UnknownResultingStateException
     *          if the state of an element is not defined
     * @throws entropy.plan.MultipleResultingStateException
     *          if an element has two state
     */
    private void checkDisjointSet() throws UnknownResultingStateException, MultipleResultingStateException {
        for (Node n : source.getAllNodes()) {
            boolean inOnlines = onlines.contains(n);
            boolean inOfflines = offlines.contains(n);
            if (inOnlines && inOfflines) {
                throw new MultipleResultingStateException(n, onlines, offlines);
            }
            //NO it's fine now, let Entropy handle that for you
            /* else if (!inOnlines && !inOfflines) {
                throw new UnknownResultingStateException(n);
            } */
        }

        for (VirtualMachine vm : source.getAllVirtualMachines()) {
            int nbIn = runnings.contains(vm) ? 1 : 0;
            if (waitings.contains(vm)
                    || sleepings.contains(vm)
                    || terminated.contains(vm)) {
                nbIn++;
            }
            if (nbIn == 0) {
                throw new UnknownResultingStateException(vm);
            } else if (nbIn > 1) {
                throw new MultipleResultingStateException(vm,
                        runnings,
                        sleepings,
                        waitings,
                        terminated);
            }
        }
    }

    @Override
    public Node[] getNodes() {
        return nodes;
    }

    @Override
    public VirtualMachine[] getVirtualMachines() {
        return vms;
    }

    @Override
    public Configuration getSourceConfiguration() {
        return source;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getFutureRunnings() {
        return runnings;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getFutureWaitings() {
        return waitings;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getFutureSleepings() {
        return sleepings;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getFutureTerminated() {
        return terminated;
    }

    @Override
    public ManagedElementSet<Node> getFutureOnlines() {
        return onlines;
    }

    @Override
    public ManagedElementSet<Node> getFutureOfflines() {
        return offlines;
    }

    @Override
    public IntDomainVar getStart() {
        return start;
    }

    @Override
    public IntDomainVar getEnd() {
        return end;
    }

    @Override
    public int getVirtualMachine(VirtualMachine vm) {
        int h = vm.hashCode();
        int v = revVMs.get(h);
        if (v == 0 && !vms[0].equals(vm)) {
            return -1;
        }
        return v;
    }

    @Override
    public VirtualMachine getVirtualMachine(int idx) {
        if (idx < vms.length && idx >= 0) {
            return vms[idx];
        }
        return null;
    }

    @Override
    public int getNode(Node n) {
        int h = n.hashCode();
        int v = revNodes.get(h);
        if (v == 0 && !nodes[0].equals(n)) {
            return -1;
        }
        return v;
    }

    @Override
    public Node getNode(int idx) {
        if (idx < nodes.length && idx >= 0) {
            return nodes[idx];
        }
        return null;
    }

    /**
     * Create all the basic action that manipulate the state of the virtual machine and the nodes.
     *
     * @throws NoAvailableTransitionException if the VM can not be running regarding to its current state
     * @throws DurationEvaluationException    if an error occurred while evaluating the duration of the action
     */
    private void makeBasicActions() throws DurationEvaluationException, NoAvailableTransitionException {

        //make the actions for the VMs
        momentVMReady = new IntDomainVar[vms.length];
        vmActions = new ArrayList<VirtualMachineActionModel>(vms.length);
        for (int i = 0; i < vms.length; i++) {
            vmActions.add(i, null);
        }

        currentLocation = new int[vms.length];
        for (int i = 0; i < runnings.size(); i++) {
            VirtualMachine vm = runnings.get(i);
            boolean dyn = manageable.contains(vm);
            VirtualMachineActionModel a;
            if (source.isRunning(vm)) {
                int dM = durationEval.evaluateMigration(vm);
                if (vm.checkOption("clone")) {
                    int dF = durationEval.evaluateForge(vm);
                    int dR = durationEval.evaluateRun(vm);
                    if (dR + dF < dM) {
                        a = new ReInstantiateActionModel(this, vm, dF, dR, durationEval.evaluateStop(vm), dyn);
                    } else {
                        a = new MigratableActionModel(this, vm, dM, dyn);
                    }
                } else {
                    a = new MigratableActionModel(this, vm, dM, dyn);
                }
                currentLocation[getVirtualMachine(vm)] = getNode(source.getLocation(vm));
            } else if (source.isSleeping(vm)) {
                a = new ResumeActionModel(this, vm, durationEval.evaluateLocalResume(vm), durationEval.evaluateRemoteResume(vm));
                currentLocation[getVirtualMachine(vm)] = getNode(source.getLocation(vm));
            } else if (source.isWaiting(vm)) {
                int d = vm.getOption("boot") != null ? Integer.parseInt(vm.getOption("boot")) : durationEval.evaluateRun(vm);
                a = new RunActionModel(this, vm, d);
                currentLocation[getVirtualMachine(vm)] = -1;
            } else {
                //? -> running means  ? -> instantiate -> running
                momentVMReady[getVirtualMachine(vm)] = createBoundIntVar("", 0, MAX_TIME);
                int d = vm.getOption("boot") != null ? Integer.parseInt(vm.getOption("boot")) : durationEval.evaluateRun(vm);
                int f = durationEval.evaluateForge(vm);
                a = new RunActionModel(this, vm, d, f);
                currentLocation[getVirtualMachine(vm)] = -1;
            }
            vmActions.set(getVirtualMachine(vm), a);
        }
        for (VirtualMachine vm : waitings) {
            if (!source.contains(vm)) {
                momentVMReady[getVirtualMachine(vm)] = createBoundIntVar("", 0, MAX_TIME);
                addIntVar(momentVMReady[getVirtualMachine(vm)]);
                int f = durationEval.evaluateForge(vm);
                vmActions.set(getVirtualMachine(vm), new InstantiateActionModel(this, vm, f));

                //from ? -> waiting, just an instantiate action model
            } else if (!source.isWaiting(vm)) {
                if (source.isRunning(vm)) {
                    throw new NoAvailableTransitionException(vm, "running", "waiting");
                } else if (source.isSleeping(vm)) {
                    throw new NoAvailableTransitionException(vm, "sleeping", "waiting");
                } else {
                    throw new NoAvailableTransitionException(vm, "terminated", "waiting");
                }
            }
        }
        for (VirtualMachine vm : sleepings) {
            if (source.isRunning(vm)) {
                VirtualMachineActionModel a = new SuspendActionModel(this, vm, durationEval.evaluateLocalSuspend(vm));
                vmActions.set(getVirtualMachine(vm), a);
            } else if (source.isWaiting(vm)) {
                throw new NoAvailableTransitionException(vm, "waiting", "sleeping");
            } else if (!source.isSleeping(vm)) {
                throw new NoAvailableTransitionException(vm, "terminated", "sleeping");
            }
        }
        for (VirtualMachine vm : terminated) {
            if (source.isRunning(vm)) {
                int d = vm.getOption("halt") != null ? Integer.parseInt(vm.getOption("halt")) : durationEval.evaluateStop(vm);
                VirtualMachineActionModel a = new StopActionModel(this, vm, d);
                vmActions.set(getVirtualMachine(vm), a);
            } else if (source.isSleeping(vm)) {
                throw new NoAvailableTransitionException(vm, "sleeping", "terminated");
            } else if (source.isWaiting(vm)) {
                throw new NoAvailableTransitionException(vm, "sleeping", "waiting");
            }
        }

        //Make the actions for the nodes

        nodesActions = new ArrayList<NodeActionModel>(nodes.length);
        for (int i = 0; i < nodes.length; i++) {
            nodesActions.add(i, null);
        }

        for (Node n : source.getAllNodes()) {
            boolean on = source.isOnline(n);
            if (onlines.contains(n)) {
                if (!on) { //goes online necessarily
                    BootNodeActionModel a = new BootNodeActionModel(this, n, durationEval.evaluateStartup(n));
                    nodesActions.set(getNode(a.getNode()), a);
                }
            } else if (offlines.contains(n)) {
                if (on) {  //goes offline necessarily
                    ShutdownNodeActionModel a = new ShutdownNodeActionModel(this, n, durationEval.evaluateShutdown(n));
                    nodesActions.set(getNode(a.getNode()), a);
                } else { //Stay offline necessarily
                    StayOfflineNodeActionModel a = new StayOfflineNodeActionModel(this, n);
                    nodesActions.set(getNode(a.getNode()), a);
                }
            } else { //State management of the node relies on the plan module
                if (!on) { //It may be booted
                    BootableNodeActionModel2 a = new BootableNodeActionModel2(this, n, durationEval.evaluateStartup(n));
                    nodesActions.set(getNode(a.getNode()), a);
                } else { //It may be turned off
                    ShutdownableNodeActionModel a = new ShutdownableNodeActionModel(this, n, durationEval.evaluateShutdown(n));
                    nodesActions.set(getNode(a.getNode()), a);
                }
            }
        }

        //Get all the slices
        demandingSlices = new ArrayList<DemandingSlice>();
        demandingSlices.addAll(ActionModels.extractDemandingSlices(getVirtualMachineActions()));
        demandingSlices.addAll(ActionModels.extractDemandingSlices(getNodeMachineActions()));

        consumingSlices = new ArrayList<ConsumingSlice>();
        consumingSlices.addAll(ActionModels.extractConsumingSlices(getVirtualMachineActions()));
        consumingSlices.addAll(ActionModels.extractConsumingSlices(getNodeMachineActions()));
    }


    @Override
    public List<VirtualMachineActionModel> getVirtualMachineActions() {
        List<VirtualMachineActionModel> actions = new ArrayList<VirtualMachineActionModel>();
        for (VirtualMachineActionModel a : vmActions) {
            if (a != null) {
                actions.add(a);
            }
        }
        return actions;
    }

    @Override
    public VirtualMachineActionModel getAssociatedAction(VirtualMachine vm) {
        return vmActions.get(getVirtualMachine(vm));
    }

    @Override
    public VirtualMachineActionModel getAssociatedVirtualMachineAction(int vmIdx) {
        return vmActions.get(vmIdx);
    }


    @Override
    public List<NodeActionModel> getNodeMachineActions() {
        List<NodeActionModel> actions = new ArrayList<NodeActionModel>();
        for (NodeActionModel a : nodesActions) {
            if (a != null) {
                actions.add(a);
            }
        }
        return actions;
    }

    @Override
    public NodeActionModel getAssociatedAction(Node n) {
        return nodesActions.get(getNode(n));
    }

    @Override
    public IntDomainVar getUsedCPU(Node n) {
        return cpuUsages[getNode(n)];
    }

    @Override
    public IntDomainVar[] getUsedCPUs() {
        return cpuUsages;
    }

    @Override
    public IntDomainVar getUsedMem(Node n) {
        return memCapacities[getNode(n)];
    }

    @Override
    public IntDomainVar getVMGroup(ManagedElementSet<VirtualMachine> vms) {
        IntDomainVar v = vmsGrp.get(vms);
        if (v != null) {
            return v;
        }

        v = createEnumIntVar("vmset" + vms.toString(), 0, MAX_NB_GRP);
        for (VirtualMachine vm : vms) {
            vmGrp.set(getVirtualMachine(vm), v);
        }
        vmsGrp.put(vms, v);
        return v;
    }

    private Comparator nodeSetComparator = new java.util.Comparator<ManagedElementSet<Node>>() {

        public int compare(ManagedElementSet<Node> nodes, ManagedElementSet<Node> nodes1) {
            return getNode(nodes.get(0)) - getNode(nodes1.get(0));
        }
    };

    @Override
    public IntDomainVar makeGroup(ManagedElementSet<VirtualMachine> vms, Set<ManagedElementSet<Node>> nodes) {
        int[] values = new int[nodes.size()];
        int i = 0;
        //System.err.println(nodes);
        Set<ManagedElementSet<Node>> cpy = new TreeSet<ManagedElementSet<Node>>(nodeSetComparator);
        cpy.addAll(nodes);
        for (ManagedElementSet<Node> ns : cpy) {
            values[i] = getGroup(ns);
            i++;
        }
        IntDomainVar v = createEnumIntVar(""/*"vmset" + vms.toString()*/, values);
        vmsGrp.put(vms, v);
        return v;
    }

    @Override
    public IntDomainVar getAssociatedGroup(VirtualMachine vm) {
        return vmGrp.get(getVirtualMachine(vm));
    }

    @Override
    public Set<ManagedElementSet<VirtualMachine>> getVMGroups() {
        return vmsGrp.keySet();
    }

    @Override
    public int getGroup(ManagedElementSet<Node> nodes) {
        if (nodesGrp.get(nodes) != null) {
            return nodesGrp.get(nodes);
        } else {
            if (nextNodeGroupVal > MAX_NB_GRP) {
                return -1;
            }
            int v = nextNodeGroupVal++;
            nodesGrp.put(nodes, v);
            revNodesGrp.add(v, nodes);
            for (Node n : nodes) {
                int nIdx = getNode(n);
                TIntArrayList l = nodeGrps.get(nIdx);
                l.add(v);
                grpId[nIdx] = v;
            }
            //Set the group of the nodes
            return v;
        }
    }

    @Override
    public Set<ManagedElementSet<Node>> getNodesGroups() {
        return nodesGrp.keySet();
    }

    @Override
    public TIntArrayList getAssociatedGroups(Node n) {
        return nodeGrps.get(getNode(n));
    }

    @Override
    public int[] getNodesGroupId() {
        return grpId;
    }

    @Override
    public ManagedElementSet<Node> getNodeGroup(int idx) {
        return revNodesGrp.get(idx);
    }

    @Override
    public DurationEvaluator getDurationEvaluator() {
        return durationEval;
    }

    @Override
    public List<DemandingSlice> getDemandingSlices() {
        return demandingSlices;
    }

    @Override
    public List<ConsumingSlice> getConsumingSlice() {
        return consumingSlices;
    }

    @Override
    public List<VirtualMachineActionModel> getAssociatedActions(ManagedElementSet<VirtualMachine> vms) {
        List<VirtualMachineActionModel> l = new LinkedList<VirtualMachineActionModel>();
        for (VirtualMachine vm : vms) {
            VirtualMachineActionModel a = getAssociatedAction(vm);
            if (a != null) {
                l.add(a);
            }
        }
        return l;
    }

    @Override
    public SetVar[] getSetModels() {
        return new SetVar[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SetVar getSetModel(Node n) {
        if (sets == null) {
            makeSetModel();
        }
        int idx = getNode(n);
        if (idx < 0) {
            return null;
        }
        return sets[idx];
    }

    private IntDomainVar[] cards;

    private void makeCards() {
        if (cards == null) {
            cards = new IntDomainVar[nodes.length];
            for (int i = 0; i < cards.length; i++) {
                cards[i] = createBoundIntVar("nb#" + i, 0, vms.length);
            }
            IntDomainVar[] hs = Slices.extractHosters(demandingSlices);
            post(new BoundGccVar(hs, cards, 0, nodes.length - 1, getEnvironment()));

        }
    }

    @Override
    public IntDomainVar getNbHosted(Node n) {
        makeCards();
        return cards[getNode(n)];
    }

    @Override
    public IntDomainVar[] getNbHosted() {
        makeCards();
        return cards;
    }

    IntDomainVar[] vmsHostUsedCPUs = null;
    IntDomainVar[] vmsHostMaxCPUs = null;

    @Override
    public IntDomainVar getHostUSedCPU(VirtualMachine vm) {
        if (vmsHostUsedCPUs == null) {
            vmsHostUsedCPUs = new IntDomainVar[getVirtualMachines().length];
        }
        int vmIndex = getVirtualMachine(vm);
        if (vmIndex < 0) {
            logger.error("virtual machine " + vm.getName()
                    + " not found, returning null");
            return null;
        }
        IntDomainVar ret = vmsHostUsedCPUs[vmIndex];
        if (ret == null) {
            ret = createIntVar(vm.getName() + ".hosterUsedCPU", IntDomainVar.BOUNDS,
                    0, getMaxHostCPUCapacity());
            nth(getAssociatedAction(vm).getDemandingSlice().hoster(), cpuUsages, ret);
            vmsHostUsedCPUs[vmIndex] = ret;
        }
        return ret;
    }

    @Override
    public IntDomainVar getHostMaxCPU(VirtualMachine vm) {
        if (vmsHostMaxCPUs == null) {
            vmsHostMaxCPUs = new IntDomainVar[getVirtualMachines().length];
        }
        int vmIndex = getVirtualMachine(vm);
        if (vmIndex < 0) {
            logger.error("virtual machine " + vm.getName()
                    + " not found, returning null");
            return null;
        }
        IntDomainVar ret = vmsHostMaxCPUs[vmIndex];
        if (ret == null) {
            ret = createIntVar(vm.getName() + ".hosterMaxCPU", IntDomainVar.BOUNDS,
                    0, getMaxHostCPUCapacity());
            nth(getAssociatedAction(vm).getDemandingSlice().hoster(), cpuMax, ret);
            vmsHostMaxCPUs[vmIndex] = ret;
        }
        return ret;
    }

    /**
     * add a constraint such as array[index]=value
     */
    public void nth(IntDomainVar index, IntDomainVar[] array, IntDomainVar var) {
        post(new ElementV(ArrayUtils.append(array,
                new IntDomainVar[]{index, var}), 0, getEnvironment()));
    }

    /**
     * add a constraint such as array[index]=value
     */
    public void nth(IntDomainVar index, int[] array, IntDomainVar var) {
        post(new Element(index, array, var));
    }

    public IntDomainVar nth(IntDomainVar index, IntDomainVar[] array) {
        int[] minmax = getMinMax(array);
        IntDomainVar ret = createBoundIntVar(foldSetNames(array), minmax[0],
                minmax[1]);
        nth(index, array, ret);
        return ret;
    }

    /**
     * add a constraint, left*right==product
     */
    public void mult(IntDomainVar left, IntDomainVar right, IntDomainVar product) {
        post(new TimesXYZ(left, right, product));
    }

    @Override
    public IntDomainVar mult(IntDomainVar left, IntDomainVar right) {
        int min = left.getInf() * right.getInf(), max = min;
        for (int prod : new int[]{left.getInf() * right.getSup(),
                left.getSup() * right.getSup(), left.getInf() * right.getSup()}) {
            if (prod < min) {
                min = prod;
            }
            if (prod > max) {
                max = prod;
            }
        }
        IntDomainVar ret = createBoundIntVar(
                "(" + left.getName() + ")*(" + right.getName() + ")", min, max);
        mult(left, right, ret);
        return ret;
    }

    @Override
    public IntDomainVar mult(IntDomainVar left, int right) {
        int min = left.getInf() * right, max = min;
        int prod = left.getSup() * right;
        if (prod < min) {
            min = prod;
        }
        if (prod > max) {
            max = prod;
        }
        IntDomainVar ret = createBoundIntVar("(" + left.getName() + ")*" + right,
                min, max);
        mult(left, createIntegerConstant("" + right, right), ret);
        return ret;
    }

    @Override
    public IntDomainVar div(IntDomainVar var, int i) {
        int a = var.getInf() / i;
        int b = var.getSup() / i;
        int min = Math.min(a, b);
        int max = Math.max(a, b);
        IntDomainVar ret = createBoundIntVar("(" + var.getName() + ")/" + i, min,
                max);
        post(new EuclideanDivisionXYZ(var, createIntegerConstant("" + i, i), ret));
        return ret;
    }

    /**
     * Extract the result destination configuration.
     *
     * @return a configuration
     */
    public Configuration extractConfiguration() {
        Configuration cfg = new SimpleConfiguration();

        for (Node n : onlines) {
            cfg.addOnline(n);
        }

        for (Node n : offlines) {
            cfg.addOffline(n);
        }

        for (NodeActionModel action : getNodeMachineActions()) {
            if (!action.putResult(this, cfg)) {
                Plan.logger.error("Unable to update configuration with " + action.toString());
            }
        }

        for (VirtualMachineActionModel action : getVirtualMachineActions()) {
            if (!action.putResult(this, cfg)) {
                Plan.logger.error("Unable to update configuration with " + action.toString());
            }
        }

        for (VirtualMachine vm : waitings) {
            cfg.addWaiting(vm);
        }

        //VM that stay sleeping
        ManagedElementSet<VirtualMachine> sleepings = this.sleepings.clone();//new DefaultManagedElementSet<VirtualMachine>(getFutureSleepings());
        sleepings.retainAll(source.getSleepings());
        for (VirtualMachine vm : sleepings) {
            cfg.setSleepOn(vm, source.getLocation(vm));
        }
        return cfg;
    }

    @Override
    public TimedReconfigurationPlan extractSolution() {
        //TODO: check if solution is found
        //Configuration dst = extractConfiguration();
        DefaultTimedReconfigurationPlan plan = new DefaultTimedReconfigurationPlan(source);
        for (NodeActionModel action : getNodeMachineActions()) {
            //TODO: quite dirty approach
            if (action instanceof BootableNodeActionModel2
                    || action instanceof BootNodeActionModel) {
                for (Action a : action.getDefinedAction(this)) {
                    if (!plan.add(a)) {
                        Plan.logger.warn("Action " + a + " is not added into the plan");
                    }
                }
            }
        }
        for (VirtualMachineActionModel action : getVirtualMachineActions()) {
            for (Action a : action.getDefinedAction(this)) {
                if (!plan.add(a)) {
                    Plan.logger.warn("Action " + a + " is not added into the plan");
                }
            }
        }
        for (Action a : plan) {
            if (a.getStartMoment() == a.getFinishMoment()) {
                Plan.logger.error("Action " + a + " has a duration equals to 0");
                throw new RuntimeException();
            }
        }

        for (NodeActionModel action : getNodeMachineActions()) {
            if (action instanceof ShutdownNodeActionModel || action instanceof ShutdownableNodeActionModel) {
                for (Action a : action.getDefinedAction(this)) {
                    if (!plan.add(a)) {
                        Plan.logger.warn("Action " + a + " is not added into the plan");
                    }
                }
            }
        }
        if (plan.getDuration() != end.getVal()) {
            Plan.logger.error("Theoretical duration (" + end.getVal() + ") and plan duration (" + plan.getDuration() + ") mismatch");
            return null;
        }
        return plan;
    }

    @Override
    public List<SolutionStatistics> getSolutionsStatistics() {
        List<SolutionStatistics> stats = new LinkedList<SolutionStatistics>();
        for (Solution s : getSearchStrategy().getStoredSolutions()) {
            IMeasures m = s.getMeasures();
            SolutionStatistics st;
            if (m.getObjectiveValue() != null) {
                st = new SolutionStatistics(m.getNodeCount(),
                        m.getBackTrackCount(),
                        m.getTimeCount(),
                        m.getObjectiveValue().intValue());
            } else {
                st = new SolutionStatistics(m.getNodeCount(),
                        m.getBackTrackCount(),
                        m.getTimeCount());
            }
            stats.add(st);
        }
        Collections.sort(stats, SolutionStatisticsComparator);
        return stats;
    }

    @Override
    public SolvingStatistics getSolvingStatistics() {
        return new SolvingStatistics(
                getNodeCount(),
                getBackTrackCount(),
                getTimeCount(),
                isEncounteredLimit());
    }

    private static Comparator<SolutionStatistics> SolutionStatisticsComparator = new Comparator<SolutionStatistics>() {

        @Override
        public int compare(SolutionStatistics sol1, SolutionStatistics sol2) {
            if (sol1.getTimeCount() == sol2.getTimeCount()) {
                //Compare wrt. the number of nodes or backtracks
                if (sol1.getNbNodes() == sol2.getTimeCount()) {
                    return sol1.getNbBacktracks() - sol2.getNbBacktracks();
                }
                return sol1.getNbNodes() - sol2.getNbNodes();
            }
            return sol1.getTimeCount() - sol2.getTimeCount();
        }
    };

    /**
     * print an array of IntDomainVar as {var0, var1, var2, var3}
     */
    protected static String foldSetNames(IntDomainVar[] values) {
        StringBuilder sb = null;
        for (IntDomainVar idv : values) {
            if (sb == null) {
                sb = new StringBuilder("{");
            } else {
                sb.append(", ");
            }
            sb.append(idv.getName());
        }
        return sb == null ? "{}" : sb.append("}").toString();
    }

    /**
     * get the min and max values of the inf and sup ranges of an array of
     * IntDomainVar
     *
     * @param array the table of VarIntDomain
     * @return [min(inf(array)), max(sup(array))]
     */
    protected static int[] getMinMax(IntDomainVar[] array) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (IntDomainVar idv : array) {
            if (idv.getInf() < min) {
                min = idv.getInf();
            }
            if (idv.getSup() > max) {
                max = idv.getSup();
            }
        }
        return new int[]{min, max};
    }

    private IntDomainVar[] hostingStatus = null;

    /**
     * A variable to indicate whether a node can be online and hosting VMs or not.
     * In practice, a node is considered to be used when its memory is at least partially used.
     * This is based on the assumption that every running VM require at least 1 unit of memory to run.
     *
     * @param nIdx the node index to check
     * @return a boolean variable. Instantiate to {@code 1} to indicate that the node hosts VMs
     */
    @Override
    public IntDomainVar isIdle(int nIdx) {
        if (hostingStatus == null) {
            hostingStatus = new IntDomainVar[nodes.length];
        }
        if (hostingStatus[nIdx] == null) {
            Node n = getNode(nIdx);
            SConstraint free = eq(getUsedMem(n), 0);
            IntDomainVar bUsed = createBooleanVar("used(" + n.getName() + ")");
            post(ReifiedFactory.builder(bUsed, free, this));
            hostingStatus[nIdx] = bUsed;
        }
        return hostingStatus[nIdx];
    }
}
