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
 * A constraint to assign a set of virtual machines to a single node.
 *
 * @author Fabien Hermenier
 */
public class Gather implements PlacementConstraint {

    /**
     * The involved VMs.
     */
    private ManagedElementSet<VirtualMachine> vms;

    /**
     * Make a new constraint.
     *
     * @param vms A non-empty set of virtual machines
     */
    public Gather(ManagedElementSet<VirtualMachine> vms) {
        this.vms = vms;
    }

    /**
     * Get the virtual machines involved in the constraint.
     *
     * @return a set of VMs. Should not be empty
     */
    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return this.vms;
    }

    /**
     * Get the set of virtual machines involved in the constraint.
     *
     * @return a set of VMs, should not be empty
     */
    public ManagedElementSet<VirtualMachine> getVirtualMachines() {
        return this.vms;
    }

    @Override
    public ManagedElementSet<Node> getNodes() {
        return new SimpleManagedElementSet<Node>();
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("gather(").append(vms).append(")");
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
        Gather that = (Gather) o;
        return vms.equals(that.vms);
    }

    @Override
    public int hashCode() {
        return vms.hashCode() + 31 * "gather".hashCode();
    }

    @Override
    public void inject(ReconfigurationProblem core) {

        //Get only the future running VMs
        ManagedElementSet<VirtualMachine> runnings = vms.clone();
        runnings.retainAll(core.getFutureRunnings());
        VJob.logger.debug(this + " only consider " + runnings);

        for (int i = 0; i < runnings.size(); i++) {
            for (int j = 0; j < i; j++) {
                Slice t1 = core.getAssociatedAction(runnings.get(i)).getDemandingSlice();
                Slice t2 = core.getAssociatedAction(runnings.get(j)).getDemandingSlice();
                core.post(core.eq(t1.hoster(), t2.hoster()));
            }
        }
    }

    /**
     * Check that the constraint is satified in a configuration.
     *
     * @param cfg the configuration to check
     * @return true if the running VMs are hosted on the same node
     */
    @Override
    public boolean isSatisfied(Configuration cfg) {
        if (vms.isEmpty()) {
            VJob.logger.debug("No virtual machines was specified");
            return true;
        }
        Node usedNode = null;
        for (VirtualMachine vm : vms) {
            if (cfg.isRunning(vm)) {
                Node n = cfg.getLocation(vm);
                if (usedNode == null) {
                    usedNode = n;
                } else if (!n.equals(usedNode)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Get VMs that seems to be misplaced.
     * If all the VMs are not running on the same node, all the VMs
     * are considered as misplaced as the node that they were supposed
     * to be hosted on is not guarantee to be known at 100%.
     */
    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        if (!isSatisfied(cfg)) {
            return vms;

        }
        return new SimpleManagedElementSet<VirtualMachine>();
    }

    @Override
    public String toXML() {
        StringBuilder b = new StringBuilder(100);
        b.append("<constraint id=\"gather\">");
        b.append("<params>");
        b.append("<param>").append(XmlVJobSerializer.getVMset(vms)).append("</param>");
        b.append("</params>");
        b.append("</constraint>");
        return b.toString();
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        PBVJob.vjob.Constraint.Builder b = PBVJob.vjob.Constraint.newBuilder();
        b.setId("gather");
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getVMset(vms)).build());
        return b.build();
    }

    @Override
    public Type getType() {
        return Type.relative;
    }
}
