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

import choco.cp.solver.search.integer.branching.AssignOrForbidIntVarVal;
import choco.cp.solver.search.integer.branching.AssignVar;
import choco.cp.solver.search.integer.valselector.MinVal;
import choco.cp.solver.search.integer.varselector.StaticVarOrder;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.choco.actionModel.*;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSliceHeights;
import entropy.plan.choco.search.*;
import gnu.trove.TIntHashSet;
import gnu.trove.TLongIntHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A placement heuristic focused on each VM.
 * First place the VMs, then plan the changes.
 *
 * @author Fabien Hermenier
 */
public class BasicPlacementHeuristic2 implements CorePlanHeuristic {

    private IntDomainVar totalDuration;

    private ManagedElementSet<VirtualMachine> managed;

    private SatisfyDemandingSliceHeights packing;

    /**
     * Make a new placement heuristic.
     *
     * @param globalCost the global cost of the plan
     */
    public BasicPlacementHeuristic2(IntDomainVar globalCost) {
        this(globalCost, null, new SimpleManagedElementSet<VirtualMachine>());

    }

    public BasicPlacementHeuristic2(IntDomainVar globalCost, SatisfyDemandingSliceHeights packing, ManagedElementSet<VirtualMachine> managed) {
        this.totalDuration = globalCost;
        this.managed = managed;
        this.packing = packing;
    }

    /**
     * To compare VMs in a descending order, wrt. their memory consumption.
     */
    private VirtualMachineComparator dsc = new VirtualMachineComparator(false, ResourcePicker.VMRc.memoryConsumption);

    @Override
    public void add(ChocoCustomRP plan) {
        ReconfigurationProblem rp = plan.getModel();
        //((CPSolver) rp).setGeometricRestart(14, 1.1d);
        Configuration src = rp.getSourceConfiguration();


        //Compute the nodes that will not leave resources. Awesome candidates to place VMs
        //on as they will be scheduled asap.
        TIntHashSet[] favorites = new TIntHashSet[2];
        favorites[0] = new TIntHashSet();
        favorites[1] = new TIntHashSet();
        if (!managed.isEmpty()) {

            //Composed with nodes that do not host misplaced VMs.
            ManagedElementSet<Node> involded = src.getAllNodes().clone();
            for (Node n : involded) {
                favorites[0].add(rp.getNode(n));
            }
            for (VirtualMachine vm : managed) {
                Node n = src.getLocation(vm);
                if (n != null && involded.remove(n)) {
                    int i = rp.getNode(n);
                    favorites[0].remove(i);
                    favorites[1].add(i);
                }
            }
            //Then remove nodes that have VMs that must be suspended or terminated
            for (VirtualMachine vm : plan.getModel().getFutureSleepings()) {
                if (src.isRunning(vm)) {
                    Node n = src.getLocation(vm);
                    int i = rp.getNode(n);
                    if (n != null && involded.remove(n)) {
                        favorites[1].add(i);
                        favorites[0].remove(i);
                    }
                }
                //Don't care about sleeping that stay sleeping
            }
            for (VirtualMachine vm : plan.getModel().getFutureTerminated()) {
                Node n = src.getLocation(vm);
                int i = rp.getNode(n);
                if (involded.remove(n)) {
                    favorites[1].add(i);
                    favorites[0].remove(i);
                }
            }
            //System.err.println(involded.size() + " (" + favorites[0].size() + ") idylic nodes over " + src.getAllNodes().size() + " (" + favorites[1].size() + ")");
        }


        //Get the VMs to move
        ManagedElementSet<VirtualMachine> onBadNodes = new SimpleManagedElementSet<VirtualMachine>();

        for (Node n : Configurations.futureOverloadedNodes(src)) {
            onBadNodes.addAll(src.getRunnings(n));
        }

        for (VirtualMachine vm : src.getSleepings()) {
            if (plan.getModel().getFutureRunnings().contains(vm)) {
                onBadNodes.add(vm);
            }
        }

        ManagedElementSet<VirtualMachine> onGoodNodes = src.getRunnings().minus(onBadNodes);

        Collections.sort(onGoodNodes, dsc);
        Collections.sort(onBadNodes, dsc);

        List<VirtualMachineActionModel> goodActions = rp.getAssociatedActions(onGoodNodes);
        List<VirtualMachineActionModel> badActions = rp.getAssociatedActions(onBadNodes);

        //Go for the VMgroup variable
        VMGroupVarSelector vmGrp = new VMGroupVarSelector(rp);
        rp.addGoal(new AssignVar(vmGrp, new NodeGroupSelector(rp, NodeGroupSelector.Option.wfMem)));


        ManagedElementSet<VirtualMachine> relocalisables = rp.getFutureRunnings();
        TLongIntHashMap oldLocation = new TLongIntHashMap(relocalisables.size());

        for (VirtualMachine vm : relocalisables) {
            int idx = rp.getVirtualMachine(vm);
            VirtualMachineActionModel a = rp.getAssociatedVirtualMachineAction(idx);
            if (a.getClass() == MigratableActionModel.class || a.getClass() == ResumeActionModel.class || a.getClass() == ReInstantiateActionModel.class) {
                oldLocation.put(a.getDemandingSlice().hoster().getIndex(), rp.getCurrentLocation(idx));
            }
        }


        //Get the VMs to move for exclusion issue
        ManagedElementSet<VirtualMachine> vmsToExlude = rp.getSourceConfiguration().getAllVirtualMachines().clone();
        Collections.sort(vmsToExlude, dsc);
        if (managed.isEmpty()) {
            rp.addGoal(new AssignVar(new ExcludedVirtualMachines(rp, rp.getSourceConfiguration(), vmsToExlude), new StayFirstSelector2(rp, oldLocation, packing, StayFirstSelector2.Option.wfMem)));
        } else {
            rp.addGoal(new AssignVar(new ExcludedVirtualMachines(rp, rp.getSourceConfiguration(), vmsToExlude), new StayFirstSelector3(rp, oldLocation, packing, favorites, StayFirstSelector3.Option.wfMem)));
        }


        for (ManagedElementSet<VirtualMachine> vms : rp.getVMGroups()) {
            ManagedElementSet<VirtualMachine> sorted = vms.clone();
            Collections.sort(sorted, dsc);
            List<VirtualMachineActionModel> inGroupActions = rp.getAssociatedActions(sorted);
            HosterVarSelector selectForInGroups = new HosterVarSelector(rp, ActionModels.extractDemandingSlices(inGroupActions));
            if (managed.isEmpty()) {
                rp.addGoal(new AssignVar(selectForInGroups, new StayFirstSelector2(rp, oldLocation, packing, StayFirstSelector2.Option.wfMem)));
            } else {
                rp.addGoal(new AssignVar(selectForInGroups, new StayFirstSelector3(rp, oldLocation, packing, favorites, StayFirstSelector3.Option.wfMem)));
            }
        }
        HosterVarSelector selectForBads = new HosterVarSelector(rp, ActionModels.extractDemandingSlices(badActions));
        if (managed.isEmpty()) {
            rp.addGoal(new AssignVar(selectForBads, new StayFirstSelector2(rp, oldLocation, packing, StayFirstSelector2.Option.wfMem)));
        } else {
            rp.addGoal(new AssignVar(selectForBads, new StayFirstSelector3(rp, oldLocation, packing, favorites, StayFirstSelector3.Option.wfMem)));
        }

        HosterVarSelector selectForGoods = new HosterVarSelector(rp, ActionModels.extractDemandingSlices(goodActions));
        if (managed.isEmpty()) {
            rp.addGoal(new AssignVar(selectForGoods, new StayFirstSelector2(rp, oldLocation, packing, StayFirstSelector2.Option.wfMem)));
        } else {
            rp.addGoal(new AssignVar(selectForGoods, new StayFirstSelector3(rp, oldLocation, packing, favorites, StayFirstSelector3.Option.wfMem)));
        }

        //VMs to run
        ManagedElementSet<VirtualMachine> vmsToRun = rp.getSourceConfiguration().getWaitings().minus(rp.getFutureWaitings()).clone();

        //vmsToRun.removeAll(rp.getFutureWaitings());
        List<VirtualMachineActionModel> runActions = rp.getAssociatedActions(vmsToRun);
        HosterVarSelector selectForRuns = new HosterVarSelector(rp, ActionModels.extractDemandingSlices(runActions));


        if (managed.isEmpty()) {
            rp.addGoal(new AssignVar(selectForRuns, new StayFirstSelector2(rp, oldLocation, packing, StayFirstSelector2.Option.wfMem)));
        } else {
            rp.addGoal(new AssignVar(selectForRuns, new StayFirstSelector3(rp, oldLocation, packing, favorites, StayFirstSelector3.Option.wfMem)));
        }

        ///SCHEDULING PROBLEM
        List<ActionModel> actions = new ArrayList<ActionModel>();
        for (VirtualMachineActionModel vma : rp.getVirtualMachineActions()) {
            actions.add(vma);
        }
        rp.addGoal(new AssignOrForbidIntVarVal(new PureIncomingFirst2(plan, rp, actions), new MinVal()));
        //rp.addGoal(new AssignVar(new PureIncomingFirst(rp, actions, plan.getCostConstraints()), new MinVal()));

        rp.addGoal(new AssignVar(new StaticVarOrder(rp, new IntDomainVar[]{rp.getEnd(), totalDuration}), new MinVal()));

    }
}
