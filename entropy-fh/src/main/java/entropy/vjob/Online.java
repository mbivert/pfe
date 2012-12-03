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
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ManageableNodeActionModel;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;

/**
 * A constraint to force a set nodes to be online at the end of the
 * reconfiguration process.
 *
 * @author Fabien Hermenier
 */
public class Online implements PlacementConstraint {

    private ManagedElementSet<Node> nodes;

    private static final ManagedElementSet<VirtualMachine> empty = new SimpleManagedElementSet<VirtualMachine>();

    /**
     * Make a new constraint.
     *
     * @param nodes the nodes to put online
     */
    public Online(ManagedElementSet<Node> nodes) {
        this.nodes = nodes;
    }

    @Override
    public void inject(ReconfigurationProblem core) {
        for (Node n : nodes) {

            if (core.getFutureOfflines().contains(n)) {
                //TODO: No possible solution, need the false() constraint.
            } else if (!core.getFutureOnlines().contains(n)) {
                ManageableNodeActionModel m = (ManageableNodeActionModel) core.getAssociatedAction(n);
                try {
                    m.getState().setVal(1);
                } catch (ContradictionException e) {
                    //TODO: No possible solution, need the false() constraint.
                }
            } //else, the node will be online, no need to manipulate it
        }
    }

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
        b.append("<constraint id=\"online\">");
        b.append("<params>");
        b.append("<param>").append(XmlVJobSerializer.getNodeset(nodes)).append("</param>");
        b.append("</params>");
        b.append("</constraint>");
        return b.toString();
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        PBVJob.vjob.Constraint.Builder b = PBVJob.vjob.Constraint.newBuilder();
        b.setId("online");
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getNodeset(nodes)).build());
        return b.build();
    }

    @Override
    public Type getType() {
        return Type.absolute;
    }

    @Override
    public String toString() {
        return new StringBuilder("online(").append(nodes).append(')').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Online q = (Online) o;

        return nodes.equals(q.nodes);
    }

    @Override
    public int hashCode() {
        return "online".hashCode() * 31 + nodes.hashCode();
    }

}
