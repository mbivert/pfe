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
package entropy.plan.choco.actionModel;

import choco.cp.solver.variables.integer.IntDomainVarAddCste;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.VirtualMachine;
import entropy.plan.Plan;
import entropy.plan.action.Action;
import entropy.plan.action.Run;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.slice.DemandingSlice;

import java.util.ArrayList;
import java.util.List;

/**
 * Model an action that run a virtual machine.
 * The action is modeled with one demanding slice.
 * The action will start at the beginning of the slice however, it may finish before the end of the slice
 * (if the duration of the action is < to the duration of the slice).
 * This little hack tends to run the VM sooner.
 * <p/>
 * The action handles the necessity of instantiating the VM if it is its first start.
 * In this situation, if indicates, an Instantiate action will be generated in addition to the Run action.
 *
 * @author Fabien Hermenier
 */
public class RunActionModel extends VirtualMachineActionModel {

    /**
     * The moment the action ends.
     */
    private IntDomainVar finish;

    private InstantiateActionModel inst;

    /**
     * Make a new run action that will not generate an instantiate action.
     * <p/>
     * The following constraints are added:
     * <ul>
     * <li>{@code end().inf = cost }</li>
     * <li>{@code end() < model.getEnd() }</li>
     * <li>{@code slice.duration.inf = cost }</li>
     * <li>{@code actionDuration <= slice.duration }</li>
     * <li>{@code end() = slice.start() + d }</li>
     * </ul>
     *
     * @param model the model of the reconfiguration problem
     * @param vm    the virtual machine associated to the action
     * @param d     the duration of the action
     */
    public RunActionModel(ReconfigurationProblem model, VirtualMachine vm, int d) {
        this(model, vm, d, -1);
    }

    /**
     * Make a new run action.
     * <p/>
     * The following constraints are added:
     * <ul>
     * <li>{@code end().inf = cost }</li>
     * <li>{@code end() < model.getEnd() }</li>
     * <li>{@code slice.duration.inf = cost }</li>
     * <li>{@code actionDuration <= slice.duration }</li>
     * <li>{@code end() = slice.start() + d }</li>
     * </ul>
     *
     * @param model the model of the reconfiguration problem
     * @param vm    the virtual machine associated to the action
     * @param d     the duration of the action
     * @param i     the duration of the instantiation process {@code -1} if no instantiation is needed
     */
    public RunActionModel(ReconfigurationProblem model, VirtualMachine vm, int d, int i) {
        super(vm);

        this.dSlice = new DemandingSlice(model, "run(" + vm.getName() + ")", vm.getCPUDemand(), vm.getMemoryDemand());

        dSlice.addToModel(model);

        duration = model.createIntegerConstant("d(run(" + vm.getName() + "))", d);

        this.finish = new IntDomainVarAddCste(model, "", start(), d);

        //  this.finish = model.createBoundIntVar("end(run(" + vm.getName() + "))", d, ReconfigurationProblem.MAX_TIME);
        //  model.post(model.eq(finish, model.plus(duration, start())));

        model.post(model.leq(finish, end()));
        try {
            dSlice.duration().setInf(d);
        } catch (ContradictionException e) {
            Plan.logger.error(e.getMessage(), e);
        }
        if (i > 0) {
            inst = new InstantiateActionModel(model, vm, i);
            model.post(model.geq(start(), model.getTimeVMReady(vm)));
        }

    }

    @Override
    public List<Action> getDefinedAction(ReconfigurationProblem solver) {
        ArrayList<Action> l = new ArrayList<Action>();
        if (inst != null) {
            l.addAll(inst.getDefinedAction(solver));
        }
        l.add(new Run(getVirtualMachine(),
                solver.getNode(dSlice.hoster().getVal()),
                start().getVal(),
                end().getVal()));
        return l;
    }

    @Override
    public boolean putResult(ReconfigurationProblem solver, Configuration cfg) {
        cfg.setRunOn(getVirtualMachine(), solver.getNode(dSlice.hoster().getVal()));
        return true;
    }

    @Override
    public final IntDomainVar start() {
        return dSlice.start();
    }

    /**
     * Get the moment the action finishes, which
     * is not necessarily the end of the slice.
     *
     * @return a moment between the beginning and the end of the slice
     */
    @Override
    public final IntDomainVar end() {
        return finish;
    }


    @Override
    public DemandingSlice getDemandingSlice() {
        return dSlice;
    }

    @Override
    public String toString() {
        return new StringBuilder("run(").append(getVirtualMachine().getName()).append(")").toString();
    }

    @Override
    public IntDomainVar getGlobalCost() {
        return finish;
    }

    /**
     * Indicate weither the action model has to handle an Instantiate action or not
     *
     * @return {@code true} to generate the associated Instantiate action.
     */
    public boolean hasToInstantiate() {
        return this.inst != null;
    }
}
