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

import choco.kernel.solver.ContradictionException;
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.slice.Slice;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;
import gnu.trove.TIntHashSet;

/**
 * A constraint to enforce a set of virtual machines
 * to be hosted on a single group of physical elements.
 *
 * @author Fabien Hermenier
 */
public class Fence implements PlacementConstraint {

    /**
     * The list of possible groups of nodes.
     */
    private ManagedElementSet<Node> group;

    /**
     * The list of VMs involved in the constraint.
     */
    private ManagedElementSet<VirtualMachine> vms;

    /**
     * Make a new constraint that enforce all the virtual machines
     * to be hosted on a single group of nodes.
     *
     * @param vms   the set of VMs to assign.
     * @param group the group of nodes.
     */
    public Fence(ManagedElementSet<VirtualMachine> vms, ManagedElementSet<Node> group) {
        this.vms = vms;
        this.group = group;
    }

    /**
     * Get the virtual machines involved in the constraint.
     *
     * @return a set of VMs. should not be empty
     */
    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return this.vms;
    }

    /**
     * Get the set of nodes involved in the constraint.
     *
     * @return a set of nodes, should not be empty
     */
    public ManagedElementSet<Node> getNodes() {
        return this.group;
    }

    /**
     * Get the set of virtual machines involved in the constraint.
     *
     * @return a set of virtual machines, should not be empty
     */
    public ManagedElementSet<VirtualMachine> getVirtualMachines() {
        return this.vms;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("fence(").append(vms);
        buffer.append(", ").append(group);
        buffer.append(")");
        return buffer.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Fence that = (Fence) o;
        return group.equals(that.group) && vms.equals(that.vms);
    }

    @Override
    public int hashCode() {
        int result = vms.hashCode();
        result = 31 * result + group.hashCode();
        result = 31 * result + "fence".hashCode();
        return result;
    }

    @Override
    public void inject(ReconfigurationProblem core) {

        ManagedElementSet<VirtualMachine> runnings = new SimpleManagedElementSet<VirtualMachine>();
        for (VirtualMachine vm : vms) {
            if (core.getFutureRunnings().contains(vm)) {
                runnings.add(vm);
            }
        }
        if (!runnings.isEmpty()) {
            if (group.size() == 1) { //Only 1 possible destination node, so we directly instantiate the variable.
                for (VirtualMachine vm : runnings) {
                    Slice t = core.getAssociatedAction(vm).getDemandingSlice();
                    if (t != null) {
                        try {
                            t.hoster().setVal(core.getNode(group.get(0)));
                        } catch (ContradictionException e) {
                            VJob.logger.error(e.getMessage(), e);
                        }
                    }
                }
            } else {
                int[] iExlude = new int[core.getSourceConfiguration().getAllNodes().size()];
                TIntHashSet toKeep = new TIntHashSet(group.size());
                for (Node n : group) {
                    toKeep.add(core.getNode(n));
                }
                int i = 0;
                for (Node n : core.getSourceConfiguration().getOnlines()) {
                    int idx = core.getNode(n);
                    if (!toKeep.contains(idx)) {
                        iExlude[i++] = idx;
                    }
                }

                for (Node n : core.getSourceConfiguration().getOfflines()) {
                    int idx = core.getNode(n);
                    if (!toKeep.contains(idx)) {
                        iExlude[i++] = idx;
                    }
                }

                //Domain restriction. Remove all the non-involved nodes
                for (VirtualMachine vm : runnings) {
                    Slice t = core.getAssociatedAction(vm).getDemandingSlice();
                    if (t != null) {
                        //if (t.hoster().isInstantiated() && toKeep.contains(t.hoster().getVal())) {

                        //} else {
                        for (int a = 0; a < i; a++) {
                            try {
                                t.hoster().remVal(iExlude[a]);
                            } catch (ContradictionException e) {
                                VJob.logger.error(e.getMessage(), e);
                            }
                        }
                        //}
                    }
                }
            }
        }
    }

    /**
     * Check that the constraint is satified in a configuration.
     *
     * @param cfg the configuration to check
     * @return true if the running VMs are hosted on more than one group
     */
    @Override
    public boolean isSatisfied(Configuration cfg) {
        if (group.isEmpty()) {
            VJob.logger.error("No group of nodes was specified");
            return false;
        }
        for (VirtualMachine vm : vms) {
            if (cfg.isRunning(vm) && !group.contains(cfg.getLocation(vm))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        ManagedElementSet<VirtualMachine> bad = new SimpleManagedElementSet<VirtualMachine>();
        for (VirtualMachine vm : vms) {
            if (cfg.isRunning(vm) && !group.contains(cfg.getLocation(vm))) {
                bad.add(vm);
            }
        }
        return bad;
    }

    @Override
    public String toXML() {
        StringBuilder b = new StringBuilder(100);
        b.append("<constraint id=\"fence\">");
        b.append("<params>");
        b.append("<param>").append(XmlVJobSerializer.getVMset(vms)).append("</param>");
        b.append("<param>").append(XmlVJobSerializer.getNodeset(group)).append("</param>");
        b.append("</params>");
        b.append("</constraint>");
        return b.toString();
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        PBVJob.vjob.Constraint.Builder b = PBVJob.vjob.Constraint.newBuilder();
        b.setId("fence");
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getVMset(vms)).build());
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getNodeset(group)).build());
        return b.build();
    }

    @Override
    public Type getType() {
        return Type.absolute;
    }
}
