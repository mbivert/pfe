/*
 * Copyright (c) 2010 Fabien Hermenier
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

import choco.cp.solver.constraints.reified.FastImpliesEq;
import choco.cp.solver.constraints.reified.ReifiedFactory;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.Node;
import entropy.plan.action.Action;
import entropy.plan.action.Startup;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.slice.ConsumingSlice;

import java.util.ArrayList;
import java.util.List;

/**
 * An action for an offline node thay can be booted if necessary.
 * By default, it stays offline. At the moment a future running VM is assigned
 * to it, the node will be automatically booted.
 *
 * @author Fabien Hermenier
 */
public class BootableNodeActionModel2 extends ManageableNodeActionModel {

    /**
     * The cost of the booting action.
     */
    private IntDomainVar cost;

    /**
     * A boolean variable to indicate weither or not the
     * node must be booted ({@code 1} to make it be booted), {@code 0} to let it offline
     */
    private IntDomainVar required;

    /**
     * Make a new action model.
     *
     * @param model the core reconfiguration problem
     * @param n     the managed node
     * @param d     the estimated duration of the booting proccess.
     * @param force {@code true} to force the node to be booted. {@code false} if it may be booted if necessary
     */
    public BootableNodeActionModel2(ReconfigurationProblem model, Node n, int d, boolean force) {
        super(n);

        //A c-slice that consume all the resources to represent the boot proccess.
        cSlice = new ConsumingSlice(model, "boot?(" + n.getName() + ")", n, n.getCPUCapacity(), n.getMemoryCapacity());
        cost = null;


        if (force) { //The node will necessarily be booted
            cost = model.createIntegerConstant("", d); //So we know the duration of the action.
            required = model.createIntegerConstant("", 1); //add the action is guarantee to occur
        } else { //The node may be booted
            cost = model.createEnumIntVar("cost(" + toString() + ")", new int[]{0, d});
            required = model.createBooleanVar("need(" + n.getName() + ")");

            //The cost equals the estimated duration <=> the node is booted. Otherwise it will equals 0
            model.post(new FastImpliesEq(required, cost, d));

            /**
             * used denotes whether or not the node is used, \ie it host running VMs
             * In practice, we consider that if some memory are used, then the node is used
             * (it avoids to use an Occurrence constraint)
             */
            IntDomainVar used = model.createBooleanVar("used(" + n.getName() + ")");
            model.post(ReifiedFactory.builder(used, model.neq(model.getUsedMem(n), 0), model));
            model.post(new FastImpliesEq(used, required, 1));
        }

        model.post(model.eq(cSlice.end(), cost));


        cSlice.addToModel(model);
    }

    /**
     * Make a new action model.
     * Model a node that may be booted is necessary.
     *
     * @param model the core reconfiguration problem
     * @param n     the managed node
     * @param d     the estimated duration of the booting proccess.
     */
    public BootableNodeActionModel2(ReconfigurationProblem model, Node n, int d) {
        this(model, n, d, false);
    }

    @Override
    public final IntDomainVar start() {
        return cSlice.start();
    }

    @Override
    public final IntDomainVar end() {
        return cost;
    }

    @Override
    public List<Action> getDefinedAction(ReconfigurationProblem solver) {
        ArrayList<Action> l = new ArrayList<Action>();
        if (cost.getVal() != 0) {
            l.add(new Startup(getNode(), start().getVal(), end().getVal()));
        }
        return l;
    }

    @Override
    public boolean putResult(ReconfigurationProblem solver, Configuration cfg) {
        //Check weither a VM will be on the node to boot it.
        if (cost.getVal() != 0) {
            cfg.addOnline(getNode());
        } else {
            cfg.addOffline(getNode());
        }
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder("boot(").append(getNode().getName()).append(")?").toString();
    }

    @Override
    public IntDomainVar getDuration() {
        return cost;
    }

    @Override
    public IntDomainVar getGlobalCost() {
        return cost;
    }

    @Override
    public IntDomainVar getState() {
        return required;
    }

}
