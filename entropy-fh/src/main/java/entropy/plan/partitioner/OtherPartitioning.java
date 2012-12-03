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

package entropy.plan.partitioner;

import entropy.configuration.*;
import entropy.vjob.*;

import java.util.*;

/**
 * @author Fabien Hermenier
 */
public class OtherPartitioning implements PlanPartitioner {

    private List<Partition> parts;

    private Map<Node, Partition> sample;

    private int nb = 0;

    public OtherPartitioning(Configuration cfg) {
        this.parts = new ArrayList<Partition>();
        this.sample = new HashMap<Node, Partition>(cfg.getAllNodes().size());
    }

    @Override
    public void part(Fence f) throws PartitioningException {
        //Check if a partition already exists than contains both the set of VMs and node
        Partition parent = null;
        ManagedElementSet<Node> ns = f.getNodes();
        ManagedElementSet<VirtualMachine> vms = f.getAllVirtualMachines();

        Node n1 = ns.get(0);
        Partition p1 = sample.get(n1);

        if (p1 != null) {
            //System.err.println("Reuse");
            for (Node n : ns) {
                if (sample.get(n) != p1) {
                    throw new PartitioningException("Nodes in " + f + " do not all belong to '" + p1 + "'");
                }
            }
            p1.getVirtualMachines().addAll(vms);
            p1.getConstraints().add(f);
        } else {
            //System.err.println("Browse all");
            for (Partition p : parts) {
                if (p.getNodes().containsAll(ns)) {
                    if (parent == null) {
                        p.getVirtualMachines().addAll(vms);
                        p.getConstraints().add(f);
                        parent = p;
                    } else {
                        throw new PartitioningException(vms + " already belong to partition: '" + p + "'");
                    }
                }
            }
            if (parent == null) {
                Partition p = new Partition(++nb);
                p.getNodes().addAll(ns);
                p.getVirtualMachines().addAll(vms);
                p.getConstraints().add(f);
                parts.add(p);
                for (Node n : ns) {
                    sample.put(n, p);
                }
            }
        }
    }

    @Override
    public void part(Ban b) throws PartitioningException {

    }

    public void part(Root c) throws PartitioningException {
        //Similar to spread, one per vm partition depending on the current
        //position of the VM. If it is not running, we can remove the constraint
        ManagedElementSet<VirtualMachine> vms = c.getAllVirtualMachines().clone();
        for (Partition p : parts) {
            if (p.getVirtualMachines().containsAll(vms)) {
                //System.err.println(c + " embedded into " + p.getVirtualMachines());
                p.getConstraints().add(c);
                return;
            } else {
                //If vms are partially in, we can split the constraint
                if (vms.retainAll(p.getVirtualMachines())) {
                    if (!vms.isEmpty()) {
                        Root sub = new Root(vms);
                        p.getConstraints().add(sub);
                    } else {
                        vms = c.getAllVirtualMachines().clone();
                    }
                }
            }
        }
    }

    public void part(Lonely c) throws PartitioningException {
        //Similar to spread, one per vm partition depending on the current
        //position of the VM. If it is not running, we can remove the constraint
        ManagedElementSet<VirtualMachine> vms = c.getAllVirtualMachines().clone();
        for (Partition p : parts) {
            if (p.getVirtualMachines().containsAll(vms)) {
                //System.err.println(c + " embedded into " + p.getVirtualMachines());
                p.getConstraints().add(c);
                return;
            } else {
                //If vms are partially in, we can split the constraint
                if (vms.retainAll(p.getVirtualMachines())) {
                    if (!vms.isEmpty()) {
                        Lonely sub = new Lonely(vms);
                        p.getConstraints().add(sub);
                    } else {
                        vms = c.getAllVirtualMachines().clone();
                    }
                }
            }
        }
    }

    public void part(Among of) throws PartitioningException {
        //All the VMs must be on a single group
        Partition parent = null;
        ManagedElementSet<VirtualMachine> vms = of.getAllVirtualMachines();
        //If all the VM are in a single VM partition
        //it is possible to filter the nodes group
        for (Partition p : parts) {
            if (p.getVirtualMachines().containsAll(vms)) {
                if (parent == null) {
                    //VMs will belong to this partition
                    p.getVirtualMachines().addAll(vms);

                    //Alter the candidate nodes: remove all nodes not in the part
                    Set<ManagedElementSet<Node>> g2 = new HashSet<ManagedElementSet<Node>>();
                    for (ManagedElementSet<Node> s : of.getGroups()) {

                        ManagedElementSet<Node> ss = new SimpleManagedElementSet<Node>();
                        for (Node n : s) {
                            if (p.getNodes().contains(n)) {
                                ss.add(n);
                            }
                        }
                        if (!ss.isEmpty()) {
                            g2.add(ss);
                        }
                    }
                    if (g2.size() == 1) {
                        p.getConstraints().add(new Fence(vms, g2.iterator().next()));
                    } else {
                        p.getConstraints().add(new Among(vms, g2));
                    }
                    parent = p;
                } else {
                    throw new PartitioningException(vms + " already belong to a partition" + p);
                }
            }
        }
        if (parent == null) {
            for (Partition p : parts) {
                for (VirtualMachine vm : vms) {
                    if (p.getVirtualMachines().contains(vm)) {
                        System.out.println("\t" + vm.getName());
                    }
                }
            }
            Partition p = new Partition(++nb);
            p.getNodes().addAll(of.getNodes());
            p.getVirtualMachines().addAll(vms);
            p.getConstraints().add(of);
            parts.add(p);
        }
    }

    @Override
    public void part(Spread c) throws PartitioningException {
        //We can split the spread constraint
        ManagedElementSet<VirtualMachine> vms = c.getAllVirtualMachines().clone();
        for (Partition p : parts) {
            if (p.getVirtualMachines().containsAll(vms)) {
                p.getConstraints().add(c);
                return;
            } else {
                //If vms are partially in, we can split the constraint
                if (vms.retainAll(p.getVirtualMachines())) {
                    if (!vms.isEmpty()) {
                        Spread sub = new ContinuousSpread(vms);
                        p.getConstraints().add(sub);
                    } else {
                        vms = c.getAllVirtualMachines().clone();
                    }
                }
            }
        }
    }

    @Override
    public List<Partition> getResultingPartitions() {
        return parts;
    }
}
