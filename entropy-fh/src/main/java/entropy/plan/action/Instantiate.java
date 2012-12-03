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

package entropy.plan.action;

import entropy.configuration.Configuration;
import entropy.configuration.VirtualMachine;
import entropy.execution.TimedExecutionGraph;
import entropy.plan.parser.TimedReconfigurationPlanSerializer;
import entropy.plan.visualization.PlanVisualizer;

import java.io.IOException;

/**
 * Create a VM that will be able to be booted after. The action may be
 * a standalone action, or attached to a boot action in the same plan.
 * <p/>
 * If it is standalone, then the VM is not a part of the source configuration
 * will be in the waiting state at the end of the reconfiguration.
 * Otherwise, the VM is not a part of the source configuration and will be
 * in the running state at the end of the reconfiguration.
 *
 * @author Fabien Hermenier
 */
public class Instantiate extends VirtualMachineAction {

    private String name;

    /**
     * Make a new action.
     *
     * @param vm the VM to instantiate.
     */
    public Instantiate(VirtualMachine vm) {
        super(vm, null);
        this.name = vm.getName();
    }

    /**
     * Make a new action.
     *
     * @param vm the VM to instantiate.
     */
    public Instantiate(VirtualMachine vm, int st, int ed) {
        super(vm, null, st, ed);
        this.name = vm.getName();
    }

    @Override
    public boolean isCompatibleWith(Configuration src) {
        return (src.isRunning(getVirtualMachine()) || !src.getAllVirtualMachines().contains(getVirtualMachine()));
    }

    @Override
    public boolean isCompatibleWith(Configuration src, Configuration dst) {
        return (!src.getAllVirtualMachines().contains(getVirtualMachine())
                && (dst.getWaitings().contains(getVirtualMachine()) || dst.getRunnings().contains(getVirtualMachine())));
    }

    @Override
    public boolean apply(Configuration c) {
        if (!c.getAllVirtualMachines().contains(getVirtualMachine())) {
            c.addWaiting(getVirtualMachine());
            return true;
        }
        //Because a run action may have insert the VM before. Not nice but still...
        return c.isRunning(getVirtualMachine());
    }

    /**
     * Test if this action is equals to another object.
     *
     * @param o the object to compare with
     * @return true if ref is an instance of Instantiate and if both
     *         instance involve the same virtual machine
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o.getClass() == this.getClass()) {
            Instantiate m = (Instantiate) o;
            return this.getVirtualMachine().equals(m.getVirtualMachine());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getVirtualMachine().hashCode() * 31;
    }

    @Override
    public boolean insertIntoGraph(TimedExecutionGraph graph) {
        //Unlock the VM when performed cause the VM is ready.
        return graph.getUnlockings(getVirtualMachine()).add(this);
    }

    @Override
    public String toString() {
        return new StringBuilder("instantiate(").append(name).append(')').toString();
    }

    @Override
    public void injectToVisualizer(PlanVisualizer vis) {
        vis.inject(this);
    }

    @Override
    public void serialize(TimedReconfigurationPlanSerializer s) throws IOException {
        s.serialize(this);
    }
}
