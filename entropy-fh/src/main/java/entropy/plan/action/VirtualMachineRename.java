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
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.execution.TimedExecutionGraph;
import entropy.plan.parser.TimedReconfigurationPlanSerializer;
import entropy.plan.visualization.PlanVisualizer;

import java.io.IOException;

/**
 * An action to rename a virtual machine.
 * The virtual machine must be known and the new name must not be already used by another
 * virtual machine
 *
 * @author Fabien Hermenier
 */
public class VirtualMachineRename extends VirtualMachineAction {

    private String name;

    private String oldName;

    /**
     * Make a new action.
     *
     * @param vm      the virtual machine to rename
     * @param n       the current hosting node. If it is {@code null}, the VM is supposed to be waiting
     * @param newName the new name of the virtual machine. Must not be already used by any virtual machine
     * @param start   the moment the action start
     * @param finish  the moment the action ends
     */
    public VirtualMachineRename(VirtualMachine vm, Node n, String newName, int start, int finish) {
        super(vm, n);
        setStartMoment(start);
        setFinishMoment(finish);
        this.name = newName;
        this.oldName = getVirtualMachine().getName();
    }

    public String getOldName() {
        return this.oldName;
    }

    public String getNewName() {
        return this.name;
    }

    @Override
    public boolean isCompatibleWith(Configuration src) {
/*        if (!src.getAllVirtualMachines().contains(getVirtualMachine())) {
            return false;
        }
        if (src.getAllVirtualMachines().get(name) != null) {
            return false;
        } */
        return true;
    }

    @Override
    public boolean isCompatibleWith(Configuration src, Configuration dst) {
        return isCompatibleWith(src);
    }

    @Override
    public boolean apply(Configuration c) {

        //Kinda tricky. We have first to remove the VM from the configuration
        //as its name is used as an hashcode.
        if (isCompatibleWith(c)) {
            Node n = c.getLocation(getVirtualMachine());
            if (n == null) {
                c.remove(getVirtualMachine());
                getVirtualMachine().rename(name);
                c.addWaiting(getVirtualMachine());
            } else {
                boolean isRunning = c.isRunning(getVirtualMachine());
                c.remove(getVirtualMachine());
                getVirtualMachine().rename(name);
                if (isRunning) {
                    c.setRunOn(getVirtualMachine(), n);
                } else {
                    c.setSleepOn(getVirtualMachine(), n);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean insertIntoGraph(TimedExecutionGraph graph) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String toString() {
        return new StringBuilder("rename(").append(oldName).append(',')
                .append(name).append(')').toString();
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
