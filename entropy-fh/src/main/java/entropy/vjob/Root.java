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
import entropy.plan.Plan;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.VirtualMachineActionModel;
import entropy.plan.choco.actionModel.slice.ConsumingSlice;
import entropy.plan.choco.actionModel.slice.DemandingSlice;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;

/**
 * A constraint to avoid relocation. Any running VMs given in parameters
 * will be disallowed to be moved to another host. Other VMs are ignored.
 * <p/>
 * In practice, for every VM having a d-slice and a c-slice, the constraint
 * enforces <code>d.hoster() == c.hoster()</code>
 * <p/>
 * TODO: Test and state if preemptible VMs have to be considered.
 *
 * @author Fabien Hermenier
 */
public class Root implements PlacementConstraint {

    /**
     * The VMs to manipulate.
     */
    private ManagedElementSet<VirtualMachine> vms;

    /**
     * Make a new constraint.
     *
     * @param v the VMs to consider.
     */
    public Root(ManagedElementSet<VirtualMachine> v) {
        this.vms = v;
    }

    @Override
    public void inject(ReconfigurationProblem core) {
        for (VirtualMachine vm : vms) {
            VirtualMachineActionModel a = core.getAssociatedAction(vm);
            if (a != null) {
                ConsumingSlice cSlice = a.getConsumingSlice();
                DemandingSlice dSlice = a.getDemandingSlice();
                if (cSlice != null && dSlice != null) {
                    try {
                        //core.post(core.eq(cSlice.hoster(), dSlice.hoster()));
                        dSlice.hoster().setVal(cSlice.hoster().getVal());
                    } catch (ContradictionException e) {
                        Plan.logger.error("Unable to root the running VM " + vm.getName() + ": " + e.getMessage());
                    }

                }
            }
        }
    }

    /**
     * Entailed method
     *
     * @param configuration the configuration to check
     * @return {@code true}
     */
    @Override
    public boolean isSatisfied(Configuration configuration) {
        return true;
    }

    /**
     * Get the VMs involved in the constraint.
     *
     * @return a set of virtual machines, should not be empty
     */
    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return vms;
    }

    @Override
    public ManagedElementSet<Node> getNodes() {
        return new SimpleManagedElementSet<Node>();
    }

    /**
     * Entailed method. No VMs may be misplaced without consideration of the reconfiguration plan.
     *
     * @param configuration the configuration to check
     * @return an empty set
     */
    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration configuration) {
        return new SimpleManagedElementSet<VirtualMachine>();
    }

    @Override
    public String toString() {
        return new StringBuilder("root(").append(vms).append(')').toString();
    }

    @Override
    public String toXML() {
        StringBuilder b = new StringBuilder(100);
        b.append("<constraint id=\"root\">");
        b.append("<params>");
        b.append("<param>").append(XmlVJobSerializer.getVMset(vms)).append("</param>");
        b.append("</params>");
        b.append("</constraint>");
        return b.toString();
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        PBVJob.vjob.Constraint.Builder b = PBVJob.vjob.Constraint.newBuilder();
        b.setId("root");
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getVMset(vms)).build());
        return b.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Root root = (Root) o;

        return vms.equals(root.vms);
    }

    @Override
    public int hashCode() {
        return vms.hashCode() + 31 * "root".hashCode();
    }

    @Override
    public Type getType() {
        //It's an absolute constraint as we already know the hosting server.
        return Type.absolute;
    }
}
