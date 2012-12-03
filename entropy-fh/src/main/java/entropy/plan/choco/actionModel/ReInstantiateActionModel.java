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
package entropy.plan.choco.actionModel;

import choco.cp.solver.constraints.integer.TimesXYZ;
import choco.cp.solver.constraints.reified.FastIFFEq;
import choco.cp.solver.constraints.reified.FastImpliesEq;
import choco.cp.solver.constraints.reified.ReifiedFactory;
import choco.cp.solver.variables.integer.BoolVarNot;
import choco.cp.solver.variables.integer.BooleanVarImpl;
import choco.cp.solver.variables.integer.IntDomainVarAddCste;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.action.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.slice.ConsumingSlice;
import entropy.plan.choco.actionModel.slice.DemandingSlice;

import java.util.ArrayList;
import java.util.List;


/**
 * Model a action that may potentially move a VM using a reinstantiation.
 * In practice, a new VM, supposed identical is booted on the destination
 * node. Once booted, the old instance is detroyed
 * <p/>
 * The action is modeled with one consuming slice and one demanding slice.
 * If the demanding slice is hosted on a different node than the consuming slice
 * it will result in a reinstantiation.
 * The demanding slice denotes the action of starting the new VM while
 * the consuming slice denotes the halting process on the old VM.
 *
 * @author Fabien Hermenier
 */
public class ReInstantiateActionModel extends VirtualMachineActionModel {

    /**
     * The global cost of the action.
     */
    private IntDomainVar cost;

    private int forgeDuration;

    private int startDuration;

    private int stopDuration;

    public final static int RENAME_DURATION = 1;

    public ReInstantiateActionModel(ReconfigurationProblem model, VirtualMachine vm, int forgeD, int startD, int stopD, boolean moveable) {
        super(vm);

        this.forgeDuration = forgeD;
        this.startDuration = startD;
        this.stopDuration = stopD;
        assert forgeDuration > 0 && startDuration > 0 && stopDuration > 0 : "The cost of reinstantiation for " + vm + " equals 0 !";

        //
        //    cSlice: default: 0 + var(duration) = var(end)
        //    dSlice: default: var(start) + var(duration) = var(end)
        //
        //    !moveable:
        //        if cpu increase: dSlice: var(start) + 0 = var(end) -> Slice(end, 0, end) -> pas de contraintes
        //        else            cSlice: 0 + 0 = var(end)  -> Slice(0,0,0), pas de contraintes
        //
        //    moveable: cSlice -> Slice(0, var(end), var(end)), pas de plus
        //
        //
        if (moveable) {

            duration = model.createEnumIntVar("overlap(reinst(" + getVirtualMachine().getName() + "))", new int[]{0, stopD + startD + RENAME_DURATION});
            cSlice = new ConsumingSlice(model, "reinstS(" + vm.getName() + ")", model.getSourceConfiguration().getLocation(vm), vm.getCPUConsumption(), vm.getMemoryConsumption());
            dSlice = new DemandingSlice(model, "reinstD(" + vm.getName() + ")", vm.getCPUDemand(), vm.getMemoryDemand());
            //System.err.println("In the model:" + dSlice + " " + vm);

            IntDomainVar forgeCost = model.createEnumIntVar("forge(" + getVirtualMachine().getName() + ")", new int[]{0, forgeD});

            IntDomainVar move = model.createBooleanVar("mv(" + getVirtualMachine().getName() + ")");
            model.post(ReifiedFactory.builder(move, model.neq(cSlice.hoster(), dSlice.hoster()), model));

            IntDomainVar stay = new BoolVarNot(model, "", (BooleanVarImpl) move);

            this.cost = model.createBoundIntVar("k(reinst(" + getVirtualMachine().getName() + "))", 0, ReconfigurationProblem.MAX_TIME);
            model.post(new TimesXYZ(move, new IntDomainVarAddCste(model, "", cSlice.end(), -stopD), cost));

            model.post(new FastIFFEq(stay, duration, 0));


            if (dSlice.getCPUheight() <= cSlice.getCPUheight()) {
                model.post(new FastImpliesEq(stay, cSlice.duration(), 0));
            } else if (dSlice.getCPUheight() > cSlice.getCPUheight()) {
                model.post(new FastImpliesEq(stay, dSlice.duration(), 0));
            }
            model.post(model.eq(dSlice.end(), model.plus(dSlice.start(), dSlice.duration())));
            model.post(model.eq(cSlice.end(), model.plus(cSlice.start(), cSlice.duration())));
            model.post(model.leq(duration, cSlice.duration()));
            model.post(model.leq(duration, dSlice.duration()));
            model.post(model.eq(this.end(), model.plus(this.start(), duration)));

            model.post(new FastIFFEq(stay, forgeCost, 0));
            model.post(model.geq(this.dSlice.start(), forgeCost));
        } else {
            boolean neadIncrease = vm.getCPUConsumption() <= vm.getCPUDemand();
            this.cost = model.createIntegerConstant("c(migrate(" + getVirtualMachine().getName() + "))", 0);
            if (neadIncrease) {
                cSlice = new ConsumingSlice("",
                        model.createIntegerConstant("", model.getNode(model.getSourceConfiguration().getLocation(vm))),
                        model.createTaskVar("", model.getStart(), model.getEnd(), model.getEnd()),
                        vm.getCPUConsumption(),
                        vm.getMemoryConsumption()
                );

                dSlice = new DemandingSlice("migD(" + vm.getName() + ")",
                        model.createIntegerConstant("", model.getNode(model.getSourceConfiguration().getLocation(vm))),
                        model.createTaskVar("", model.getEnd(), model.getEnd(), model.createIntegerConstant("", 0)),
                        vm.getCPUDemand(),
                        vm.getMemoryDemand()

                );
            } else {
                cSlice = new ConsumingSlice("",
                        model.createIntegerConstant("", model.getNode(model.getSourceConfiguration().getLocation(vm))),
                        model.createTaskVar("", model.getStart(), model.getStart(), model.getStart()),
                        vm.getCPUConsumption(),
                        vm.getMemoryConsumption()
                );

                dSlice = new DemandingSlice("",
                        model.createIntegerConstant("", model.getNode(model.getSourceConfiguration().getLocation(vm))),
                        model.createTaskVar("", model.getStart(), model.getEnd(), model.getEnd()),
                        vm.getCPUDemand(),
                        vm.getMemoryDemand()

                );
            }
            model.post(model.eq(this.end(), this.start()));
        }

        model.post(model.leq(cSlice.duration(), model.getEnd()));
        model.post(model.leq(dSlice.duration(), model.getEnd()));
    }

    /**
     * Get the moment the action ends. The action ends at the moment
     * the slice on the source node ends.
     *
     * @return <code>getConsumingSlice().end()</code>
     */
    @Override
    public final IntDomainVar end() {
        return this.getConsumingSlice().end();
    }

    /**
     * Get the moment the action starts. The action starts at the moment
     * the slice on the source node starts.
     *
     * @return <code>getDemandingSlice().start()</code>
     */
    @Override
    public final IntDomainVar start() {
        return this.getDemandingSlice().start();
    }

    /**
     * Return the migration action if the VM have to move.
     *
     * @return a Migration if the source node and the destination node are different. null otherwise
     */
    @Override
    public List<Action> getDefinedAction(ReconfigurationProblem solver) {
        ArrayList<Action> l = new ArrayList<Action>();
        int cIdx = getConsumingSlice().hoster().getVal();
        int dIdx = getDemandingSlice().hoster().getVal();
        if (cIdx != dIdx) {
            int dStart = dSlice.start().getVal();
            int cEnd = cSlice.end().getVal();
            Node srcN = solver.getNode(cIdx);
            Node dstN = solver.getNode(dIdx);
            VirtualMachine cpy = getVirtualMachine().clone();
            cpy.rename(cpy.getName() + "-tmpClone");
            l.add(new Instantiate(cpy, 0, forgeDuration));
            l.add(new Run(cpy, dstN, dStart, dStart + startDuration));
            l.add(new Stop(getVirtualMachine(), srcN, cEnd - stopDuration - RENAME_DURATION, cEnd - RENAME_DURATION));
            l.add(new VirtualMachineRename(cpy, srcN, getVirtualMachine().getName(), cEnd - RENAME_DURATION, cEnd));
        }
        return l;
    }

    @Override
    public boolean putResult(ReconfigurationProblem solver, Configuration cfg) {
        Node n = solver.getNode(getDemandingSlice().hoster().getVal());
        cfg.addOnline(n);
        return cfg.setRunOn(getVirtualMachine(), n);

    }

    @Override
    public String toString() {
        return "reinstantiate(" + getVirtualMachine().getName() + ")";
    }

    @Override
    public IntDomainVar getGlobalCost() {
        return this.cost;
    }
}
