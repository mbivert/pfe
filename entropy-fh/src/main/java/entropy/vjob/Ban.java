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
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.slice.Slice;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;

/**
 * A constraint to enforce a set of virtual machines to avoid
 * to be hosted on a group of nodes.
 *
 * @author Fabien Hermenier
 */
public class Ban implements PlacementConstraint {

    /**
     * The set of nodes to exlude.
     */
    private ManagedElementSet<Node> nodes;

    /**
     * The set of VMs involved in the constraint.
     */
    private ManagedElementSet<VirtualMachine> vms;

    /**
     * Make a new constraint.
     *
     * @param vms   the VMs to assign
     * @param nodes the nodes to exclude
     */
    public Ban(ManagedElementSet<VirtualMachine> vms, ManagedElementSet<Node> nodes) {
        this.nodes = nodes;
        this.vms = vms;
    }

    /**
     * Get the set of nodes involved in the constraint.
     *
     * @return a set of nodes
     */
    public ManagedElementSet<Node> getNodes() {
        return this.nodes;
    }

    /**
     * Get all the virtual machines involved in the constraint.
     *
     * @return a set of VMs. Should not be empty
     */
    public ManagedElementSet<VirtualMachine> getVirtualMachines() {
        return this.vms;
    }

    /**
     * Get all the virtual machines involved in the constraint.
     *
     * @return a set of VMs. Should not be empty
     */
    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return this.vms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Ban that = (Ban) o;

        return (nodes.equals(that.nodes) && vms.equals(that.vms));
    }

    @Override
    public int hashCode() {
        int result = vms.hashCode();
        result = 31 * result + nodes.hashCode();
        result = 31 * result + "ban".hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("ban(").append(vms);
        buffer.append(", ");
        buffer.append(nodes);
        buffer.append(')');
        return buffer.toString();
    }


    /**
     * Apply the constraint to the plan to all the VMs in a future running state.
     * FIXME: What about running VMs that will be suspended ?
     *
     * @param core the plan to customize. Must implement {@link entropy.plan.choco.ChocoCustomRP}
     */
    @Override
    public void inject(ReconfigurationProblem core) {
        int[] nodesIdx = new int[nodes.size()];
        int i = 0;
        for (Node n : nodes) {
            nodesIdx[i++] = core.getNode(n);
        }

        for (VirtualMachine vm : vms) {
            if (core.getFutureRunnings().contains(vm)) {
                Slice t = core.getAssociatedAction(vm).getDemandingSlice();
                if (t != null) {
                    for (int x : nodesIdx) {
                        try {
                            t.hoster().remVal(x);
                        } catch (Exception e) {
                            VJob.logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Check that the constraint is satified in a configuration.
     *
     * @param cfg the configuration to check
     * @return true if the VMs are not running on the banned nodes.
     */
    @Override
    public boolean isSatisfied(Configuration cfg) {
        for (VirtualMachine vm : vms) {
            if (cfg.isRunning(vm) && nodes.contains(cfg.getLocation(vm))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        ManagedElementSet<VirtualMachine> bad = new SimpleManagedElementSet<VirtualMachine>();
        for (VirtualMachine vm : vms) {
            if (cfg.isRunning(vm) && nodes.contains(cfg.getLocation(vm))) {
                bad.add(vm);
            }
        }
        return bad;
    }

    @Override
    public String toXML() {
        StringBuilder b = new StringBuilder(100);
        b.append("<constraint id=\"ban\">");
        b.append("<params>");
        b.append("<param>").append(XmlVJobSerializer.getVMset(vms)).append("</param>");
        b.append("<param>").append(XmlVJobSerializer.getNodeset(nodes)).append("</param>");
        b.append("</params>");
        b.append("</constraint>");
        return b.toString();
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        PBVJob.vjob.Constraint.Builder b = PBVJob.vjob.Constraint.newBuilder();
        b.setId("ban");
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getVMset(vms)).build());
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getNodeset(nodes)).build());
        return b.build();
    }

    @Override
    public Type getType() {
        return Type.absolute;
    }
}
