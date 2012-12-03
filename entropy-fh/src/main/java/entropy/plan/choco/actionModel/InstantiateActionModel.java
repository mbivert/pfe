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

import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.VirtualMachine;
import entropy.plan.Plan;
import entropy.plan.action.Action;
import entropy.plan.action.Instantiate;
import entropy.plan.choco.ReconfigurationProblem;

import java.util.ArrayList;
import java.util.List;

/**
 * Model an instantiation action.
 * No consuming nor demanding slice. Instead, it restricts the variable that indicates
 * the moment the VM is ready.
 *
 * @author Fabien Hermenier
 */
public class InstantiateActionModel extends VirtualMachineActionModel {

    private IntDomainVar start;

    public InstantiateActionModel(ReconfigurationProblem rp, VirtualMachine vm, int d) {
        super(vm);
        this.start = rp.getStart();
        this.duration = rp.getTimeVMReady(vm);
        assert d > 0 : "forge duration equals to 0";
        rp.post(rp.geq(rp.getEnd(), d));
        try {
            this.duration.setVal(d);
        } catch (ContradictionException e) {
            Plan.logger.error(e.getMessage(), e);
        }

    }

    @Override
    public IntDomainVar start() {
        return start;
    }

    @Override
    public IntDomainVar end() {
        return duration;
    }

    @Override
    public List<Action> getDefinedAction(ReconfigurationProblem solver) {
        List<Action> l = new ArrayList<Action>();
        l.add(new Instantiate(getVirtualMachine(), start.getVal(), duration.getVal()));
        return l;
    }

    @Override
    public boolean putResult(ReconfigurationProblem solver, Configuration cfg) {
        if (!cfg.contains(getVirtualMachine())) {
            cfg.addWaiting(getVirtualMachine());
            return true;
        }
        return false;
    }

    @Override
    public IntDomainVar getDuration() {
        return this.duration;
    }

    @Override
    public IntDomainVar getGlobalCost() {
        return this.duration;
    }
}
