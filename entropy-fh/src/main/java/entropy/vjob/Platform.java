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

package entropy.vjob;

import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ManageableNodeActionModel;
import entropy.plan.choco.actionModel.RetypeNodeActionModel;
import entropy.plan.choco.actionModel.slice.ConsumingSlice;
import entropy.plan.choco.actionModel.slice.DemandingSlice;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;

/* XXX .ordinal to get integer */
enum AllPlatform {
    FOO("foo"),
    BAR("bar"),
    BAZ("baz");
    private String name;

    AllPlatform(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}


public class Platform implements PlacementConstraint {
    private ManagedElementSet<Node> nodes;
    String newPlatform;

    private static final ManagedElementSet<VirtualMachine> empty = new SimpleManagedElementSet<VirtualMachine>();

    /**
     * Make a new constraint.
     *
     * @param nodes the nodes to put online
     */
    public Platform(ManagedElementSet<Node> nodes, String newPlatform) {
        this.nodes = nodes;
        this.newPlatform = newPlatform;
    }

    public Platform(ManagedElementSet<Node> nodes) {
        this(nodes, "any");
    }

    private boolean isValidPlatform() {
        for (Node n : nodes)
            if (!n.getAvailablePlatforms().contains(newPlatform))
                return false;
        return true;
    }

    @Override
    public void inject(ReconfigurationProblem core) {
        //TODO: No possible solution, need the false() constraint.
        if (!isValidPlatform())
            return;

        for (Node n : nodes) {
            /* search time when every machines may have leave the node */
            int maxend = -1;
            for (VirtualMachine vm : core.getSourceConfiguration().getRunnings(n)) {
                int sup = core.getAssociatedAction(vm).getConsumingSlice().end().getSup();
                if (sup > maxend)
                    maxend = sup;
            }
            IntDomainVar end = core.createIntegerConstant("", maxend);

            /* every vm must leave before the previously calculated time */
            for (VirtualMachine vm : core.getSourceConfiguration().getRunnings(n))
                core.post(core.leq(core.getAssociatedAction(vm).getConsumingSlice().end(), end));

            /*
             * constrain only VMs that may move on this node.
             * take an arbitrary node where the VM can be located, and compare
             * its platform type to newPlatform
             */
            for (DemandingSlice d : core.getDemandingSlices())
                if (core.getNode(d.hoster().getInf()).getCurrentPlatform().equals(newPlatform))
                    core.post(core.geq(d.start(), core.plus(end, RetypeNodeActionModel.RETYPE_DURATION)));
        }
    }

    /* XXX check platform? */
    @Override
    public boolean isSatisfied(Configuration cfg) {
        for (Node n : nodes) {
            if (!cfg.isOnline(n)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return empty;
    }

    @Override
    public ManagedElementSet<Node> getNodes() {
        return this.nodes;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        return empty; //The node is offline, so it does not any VMs on it
    }

    @Override
    public String toXML() {
        StringBuilder b = new StringBuilder();
        b.append("<constraint id=\"platform\">");
        b.append("<params>");
        b.append("<param>").append(XmlVJobSerializer.getNodeset(nodes)).append("</param>");
        b.append("</params>");
        b.append("</constraint>");
        return b.toString();
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        PBVJob.vjob.Constraint.Builder b = PBVJob.vjob.Constraint.newBuilder();
        b.setId("platform");
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getNodeset(nodes)).build());
        return b.build();
    }

    @Override
    public Type getType() {
        return Type.absolute;
    }

    @Override
    public String toString() {
        return "platform("+nodes+')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Platform q = (Platform) o;

        return nodes.equals(q.nodes) && newPlatform.equals(q.newPlatform);
    }

    @Override
    public int hashCode() {
        return "platform".hashCode() * 31 + nodes.hashCode();
    }

}
