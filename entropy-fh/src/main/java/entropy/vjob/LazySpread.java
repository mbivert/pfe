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

import choco.cp.solver.constraints.global.BoundAllDiff;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.slice.Slice;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of Spread that ensure all the VMs will be on a distinct node
 * at the end of the reconfiguration process. However, they may be on a same node
 * during the reconfiguration. For stronger guarantees, see {@link ContinuousSpread}
 *
 * @author Fabien Hermenier
 * @see ContinuousSpread
 */
public class LazySpread extends Spread {
    /**
     * Make a new constraint.
     *
     * @param vms the involved virtual machines
     */
    public LazySpread(ManagedElementSet<VirtualMachine> vms) {
        super(vms);
    }

    /**
     * Apply the constraint to the plan if the VM must be in the running state.
     * The constraint is applied on all the future running virtual machines given at instantiation.
     * Others are ignored.
     *
     * @param core the plan to customize. Must implement {@link entropy.plan.choco.ChocoCustomRP}
     */
    @Override
    public void inject(ReconfigurationProblem core) {

        //Get only the future running VMS
        List<IntDomainVar> runnings = new ArrayList<IntDomainVar>();

        for (VirtualMachine vm : getAllVirtualMachines()) {
            if (core.getFutureRunnings().contains(vm)) {
                Slice t = core.getAssociatedAction(vm).getDemandingSlice();
                if (t != null) {
                    runnings.add(t.hoster());
                }
            }
        }
        if (runnings.isEmpty()) {
            VJob.logger.debug(this + " is entailed. No VMs are running");
        } else {
            core.post(new BoundAllDiff(runnings.toArray(new IntDomainVar[runnings.size()]), true));
            //core.post(new AllDifferent(runnings.toArray(new IntDomainVar[runnings.size()]), core.getEnvironment()));
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("lazySpread(").append(vms).append(')').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LazySpread that = (LazySpread) o;
        return vms.equals(that.vms);
    }


    @Override
    public int hashCode() {
        return vms.hashCode() + "lSpread".hashCode() * 31;
    }

    @Override
    public String toXML() {
        StringBuilder b = new StringBuilder(100);
        b.append("<constraint id=\"lazySpread\">");
        b.append("<params>");
        b.append("<param>").append(XmlVJobSerializer.getVMset(getAllVirtualMachines())).append("</param>");
        b.append("</params>");
        b.append("</constraint>");
        return b.toString();
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        PBVJob.vjob.Constraint.Builder b = PBVJob.vjob.Constraint.newBuilder();
        b.setId("lazySpread");
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getVMset(getAllVirtualMachines())).build());
        return b.build();
    }
}
