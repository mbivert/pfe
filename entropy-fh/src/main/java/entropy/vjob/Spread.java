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

package entropy.vjob;

import entropy.configuration.*;
import gnu.trove.THashSet;

import java.util.HashMap;
import java.util.Map;

/**
 * A constraint to ensure a set of VMs will be hosted on different nodes.
 *
 * @author Fabien Hermenier
 */
public abstract class Spread implements PlacementConstraint {

    /**
     * The VMs involved in the constraint.
     */
    protected ManagedElementSet<VirtualMachine> vms;

    private final static ManagedElementSet<Node> empty = new SimpleManagedElementSet<Node>();

    /**
     * Make a new constraint.
     *
     * @param vms the involved virtual machines
     */
    public Spread(ManagedElementSet<VirtualMachine> vms) {
        this.vms = vms;
    }

    public Spread(VirtualMachine... vms) {
        this.vms = new SimpleManagedElementSet<VirtualMachine>();
        for (VirtualMachine vm : vms) {
            this.vms.add(vm);
        }
    }

    /**
     * Get the virtual machines involved in the constraint.
     *
     * @return a set of virtual machines. Should not be empty
     */
    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return vms;
    }

    /**
     * Get the set of virtual machines involved in the constraint.
     *
     * @return a set of virtual machine. Should not be empty
     */
    public ManagedElementSet<VirtualMachine> getVirtualMachines() {
        return vms;
    }

    @Override
    public ManagedElementSet<Node> getNodes() {
        return empty;
    }

    /**
     * Check that the constraint is satified in a configuration.
     *
     * @param cfg the configuration to check
     * @return true if the running VMs are hosted on distinct nodes
     */
    @Override
    public boolean isSatisfied(Configuration cfg) {
        THashSet<String> used = new THashSet<String>();
        for (VirtualMachine vm : vms) {
            if (cfg.isRunning(vm)) {
                Node h = cfg.getLocation(vm);
                if (!used.add(h.getName())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns every running VMs that are colocated.
     *
     * @param cfg the configuration to check
     * @return a set of virtual machines that share nodes.
     */
    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        Map<Node, ManagedElementSet<VirtualMachine>> spots = new HashMap<Node, ManagedElementSet<VirtualMachine>>();
        ManagedElementSet<VirtualMachine> bad = new SimpleManagedElementSet<VirtualMachine>();
        for (VirtualMachine vm : vms) {
            Node h = cfg.getLocation(vm);
            if (cfg.isRunning(vm)) {
                if (!spots.containsKey(h)) {
                    spots.put(h, new SimpleManagedElementSet<VirtualMachine>());
                }
                spots.get(h).add(vm);
            }

        }
        for (Map.Entry<Node, ManagedElementSet<VirtualMachine>> e : spots.entrySet()) {
            if (e.getValue().size() > 1) {
                bad.addAll(e.getValue());
                VJob.logger.debug(e.getValue() + " are hosted on the same node: '" + e.getKey().getName() + "'");
            }
        }
        return bad;
    }

    @Override
    public Type getType() {
        return Type.relative;
    }
}
