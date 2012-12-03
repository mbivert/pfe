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

package entropy.plan.choco.constraint.platform;

import entropy.configuration.*;
import entropy.plan.Plan;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.vjob.Fence;

import java.util.HashMap;
import java.util.Map;

/**
 * A Static version of the platform Constraint.
 * All the VMs can only be hosted on nodes having the right deployed platform.
 * Nodes and VMs without a declared platform matches.
 * If a node has a change in its platform, it is not considered
 * @author Fabien Hermenier
 */
public class StaticPlatform implements Platform {

    @Override
    public void add(ReconfigurationProblem rp) {
        HashMap<String, ManagedElementSet<Node>> nodes = new HashMap<String, ManagedElementSet<Node>>();
        HashMap<String, ManagedElementSet<VirtualMachine>> vms = new HashMap<String, ManagedElementSet<VirtualMachine>>();
        for (Node n : rp.getNodes()) {
            String p = n.getCurrentPlatform();
                ManagedElementSet<Node> ins = nodes.get(p);
                if (ins == null) {
                    ins = new SimpleManagedElementSet<Node>();
                    nodes.put(p == null ? null : p, ins);
                }
                ins.add(n);
        }

        for (VirtualMachine vm : rp.getVirtualMachines()) {
            String p = vm.getHostingPlatform();
            if (!nodes.containsKey(p)) {
                Plan.logger.error("No hosting platform for VM '" + vm + ": " + p);
                return;
            }
            ManagedElementSet<VirtualMachine> ins = vms.get(p);
            if (ins == null) {
                ins = new SimpleManagedElementSet<VirtualMachine>();
                vms.put(p, ins);
            }
            ins.add(vm);
        }

        //Now, just a series of Fence
        for (Map.Entry<String, ManagedElementSet<VirtualMachine>> e : vms.entrySet()) {
            String t = e.getKey();
            Fence f = new Fence(e.getValue(), nodes.get(t));
            f.inject(rp);
        }
    }


    @Override
    public boolean isSatisfied(Configuration cfg) {
        for (Node n : cfg.getOnlines()) {
            for (VirtualMachine vm : cfg.getRunnings(n)) {
                String p = n.getCurrentPlatform();
                String k2 = vm.getHostingPlatform();
                if (p == null && k2 != null) {
                    return false;
                } else if (p != null) {
                    if (!p.equals(k2)) {
                        return false;
                    }
                }
            }

            for (VirtualMachine vm : cfg.getSleepings(n)) {
                String p = n.getCurrentPlatform();
                String k2 = vm.getHostingPlatform();
                if (p == null && k2 != null) {
                    return false;
                }
                if (p != null) {
                    if (!p.equals(k2)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return null;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
        for (Node n : cfg.getOnlines()) {
            for (VirtualMachine vm : cfg.getRunnings(n)) {
                String p = n.getCurrentPlatform();
                String k2 = vm.getHostingPlatform();
                if (p == null && k2 != null) {
                    vms.add(vm);
                } else if (p != null) {
                    if (!p.equals(k2)) {
                        vms.add(vm);
                    }
                }
            }

            for (VirtualMachine vm : cfg.getSleepings(n)) {
                String p = n.getCurrentPlatform();
                String k2 = vm.getHostingPlatform();
                if (p == null && k2 != null) {
                    vms.add(vm);
                } else if (p != null) {
                    if (!p.equals(k2)) {
                        vms.add(vm);
                    }
                }
            }
        }
        return vms;
    }
}
