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

import choco.cp.solver.constraints.integer.TimesXYZ;
import choco.cp.solver.constraints.reified.FastIFFEq;
import choco.cp.solver.constraints.reified.FastImpliesEq;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.Node;
import entropy.plan.Plan;
import entropy.plan.action.Action;
import entropy.plan.action.Shutdown;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.slice.DemandingSlice;

import java.util.ArrayList;
import java.util.List;

/**
 * /**
 * An action for an online node thay can be shutdown if necessary.
 *
 * @author Fabien Hermenier
 */
public class ShutdownableNodeActionModel extends ManageableNodeActionModel {

    /**
     * {@code 1} if the node has to stay online. {@code 0} if it has to go offline.
     */
    private IntDomainVar state;

    /**
     * The end of the action.
     */
    private IntDomainVar end;

    private IntDomainVar cost;

    /**
     * Make a new action model.
     * Model a node that may be turned of is necessary.
     *
     * @param rp the core reconfiguration problem
     * @param n  the managed node
     * @param d  the estimated duration of the shutdown action.
     */
    public ShutdownableNodeActionModel(ReconfigurationProblem rp, Node n, int d) {
        this(rp, n, d, false);
    }

    /**
     * Make a new action model.
     *
     * @param rp    the core reconfiguration problem
     * @param n     the managed node
     * @param d     the estimated duration of the shutdown proccess.
     * @param force {@code true} to force the node to be turned offline. {@code false} if it may be turned off
     */
    public ShutdownableNodeActionModel(ReconfigurationProblem rp, Node n, int d, boolean force) {
        super(n);

        //The moment the action end, which is not necessarily the end of the d-slice
        end = rp.createBoundIntVar("end(shutdown(" + n.getName() + "))", 0, ReconfigurationProblem.MAX_TIME);

        if (force) {  //Has to be offline so some simplification
            cost = end;
            //A slice that consume all the resources
            this.dSlice = new DemandingSlice(rp, "shutdown(" + n.getName() + ")", rp.getNode(n), n.getCPUCapacity(), n.getMemoryCapacity());

            //the node will necessarily go offline
            state = rp.createIntegerConstant("", 0);
            //So we know the duration of the action
            duration = rp.createIntegerConstant("", d);

            //rp.post(rp.eq(duration, rp.plus(dSlice.start(), d)));
            try {
                //The action ends at least after 'd' seconds
                end.setInf(d);
                dSlice.duration().setInf(d);

                //And the d-slice is exclusive on the node
                this.dSlice.isExclusive().setVal(1);

            } catch (ContradictionException e) {
                Plan.logger.error(e.getMessage(), e);
            }
        } else { //may go offline

            //Duration is either 0 (no shutdown) or 'd' (shutdown)
            duration = rp.createEnumIntVar("effectiveD(shutdown(" + n.getName() + "))", new int[]{0, d});

            //A dslice without height to be ignored by the packing constraint. So it does not disallow to
            //have other d-slices on it. But required to be handled by the scheduling problem.
            this.dSlice = new DemandingSlice(rp, "shutdown(" + n.getName() + ")", rp.getNode(n), 0, 0);

            //The future state is uncertain yet
            state = rp.createBooleanVar("state(" + n.getName() + ")");


            IntDomainVar isOffline = dSlice.isExclusive(); //offline means there will be an exclusive d-Slice
            rp.post(rp.neq(isOffline, state)); //Cannot rely on BoolVarNot cause it is not compatible with the eq() below
            // Duration necessarily < end of the duration of the reconfiguration process.
            rp.post(rp.leq(duration, rp.getEnd()));

            /**
             * If it is state to shutdown the node, then the duration of the dSlice is not null
             */
            rp.post(new FastIFFEq(state, duration, 0)); //Stay online <-> duration = 0
            //rp.post(new FastIFFEq(state, end, 0)); //Stay online <-> duration = 0
            rp.post(new FastImpliesEq(isOffline, rp.getUsedMem(n), 0)); //Packing stuff; isOffline -> mem == 0

            cost = rp.createBoundIntVar("cost(shutdown(" + n.getName() + "))", 0, ReconfigurationProblem.MAX_TIME);
            rp.post(new TimesXYZ(end, isOffline, cost));
        }

        //The end of the action is 'd' seconds after starting the d-slice
        rp.post(rp.eq(end(), rp.plus(dSlice.start(), duration)));
        rp.post(rp.leq(end(), rp.getEnd()));
        dSlice.addToModel(rp);
    }

    @Override
    public IntDomainVar getState() {
        return state;
    }

    @Override
    public IntDomainVar start() {
        return dSlice.start();
    }

    @Override
    public IntDomainVar end() {
        return this.end;
    }

    @Override
    public List<Action> getDefinedAction(ReconfigurationProblem solver) {
        //If the node was online, shutdown action. No action otherwise
        ArrayList<Action> l = new ArrayList<Action>();
        if (solver.getSourceConfiguration().isOnline(getNode()) && state.isInstantiated() && state.getVal() == 0) {
            l.add(new Shutdown(getNode(), start().getVal(), end().getVal()));
        }
        return l;
    }

    @Override
    public boolean putResult(ReconfigurationProblem solver, Configuration cfg) {
        return cfg.addOffline(getNode());
    }

    @Override
    public String toString() {
        return new StringBuilder("shutdown(").append(getNode().getName()).append(')').toString();
    }

    @Override
    public IntDomainVar getGlobalCost() {
        return cost;
    }

    @Override
    public IntDomainVar getDuration() {
        return this.duration;
    }
}
