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

package entropy.vjob;

import choco.kernel.solver.ContradictionException;
import entropy.configuration.*;
import entropy.plan.Plan;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.VirtualMachineActionModel;
import entropy.plan.choco.actionModel.slice.ConsumingSlice;
import entropy.plan.choco.actionModel.slice.DemandingSlice;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;

/**
 * Place a set of nodes under quarantine.
 * In this settings, no new VMs can be hosted on these nodes. Hosted VMs can
 * not leave the nodes. The only solution will be to terminate them.
 * TODO: What if these nodes become saturated ? set the VM consumption to 1 on these nodes ?
 *
 * @author Fabien Hermenier
 */
public class Quarantine implements PlacementConstraint {

    private ManagedElementSet<Node> nodes;

    private static final ManagedElementSet<VirtualMachine> empty = new SimpleManagedElementSet<VirtualMachine>();

    public Quarantine(ManagedElementSet<Node> ns) {
        this.nodes = ns;
    }

    @Override
    public void inject(ReconfigurationProblem core) {

        //Cool, just a composition: root for the VMs already hosted and a ban for the other VMs
        Configuration cfg = core.getSourceConfiguration();
        int[] nIdxs = new int[nodes.size()];
        int i = 0;
        for (Node n : nodes) {
            nIdxs[i++] = core.getNode(n);
        }
        for (VirtualMachine vm : core.getFutureRunnings()) {
            Node n = cfg.getLocation(vm);
            VirtualMachineActionModel a = core.getAssociatedAction(vm);
            DemandingSlice d = a.getDemandingSlice();
            if (nodes.contains(n)) {
                //Root the VM to its current location if exists
                ConsumingSlice c = a.getConsumingSlice();
                if (c != null && d != null) {
                    try {
                        if (!c.hoster().isInstantiated()) {
                            Plan.logger.error("The consumng slice " + c + " should be instantiated");
                        }
                        d.hoster().setVal(c.hoster().getVal());
                    } catch (ContradictionException e) {
                        Plan.logger.error(e.getMessage(), e);
                    }
                }
            } else {
                //Avoid the other VMs to come here
                if (d != null) {
                    try {
                        //TODO: Should be better with an set to avoid to remove value when possible
                        for (int nIdx : nIdxs) {
                            d.hoster().remVal(nIdx);
                        }
                    } catch (ContradictionException e) {
                        Plan.logger.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    @Override
    public boolean isSatisfied(Configuration cfg) {
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
        return empty;
    }

    @Override
    public String toXML() {
        StringBuilder b = new StringBuilder();
        b.append("<constraint id=\"quarantine\">");
        b.append("<params>");
        b.append("<param>").append(XmlVJobSerializer.getNodeset(nodes)).append("</param>");
        b.append("</params>");
        b.append("</constraint>");
        return b.toString();
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        PBVJob.vjob.Constraint.Builder b = PBVJob.vjob.Constraint.newBuilder();
        b.setId("quarantine");
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getNodeset(nodes)).build());
        return b.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Quarantine q = (Quarantine) o;

        return nodes.equals(q.nodes);
    }

    @Override
    public int hashCode() {
        return "quarantine".hashCode() * 31 + nodes.hashCode();
    }

    @Override
    public Type getType() {
        return Type.absolute;
    }
}
