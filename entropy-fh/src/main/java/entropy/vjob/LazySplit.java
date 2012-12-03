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

import choco.cp.solver.constraints.global.Disjoint;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ActionModel;
import entropy.plan.choco.actionModel.ActionModels;
import entropy.plan.choco.actionModel.slice.DemandingSlice;
import entropy.plan.choco.actionModel.slice.Slices;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;

import java.util.LinkedList;
import java.util.List;

/**
 * A lazy implementation of Split focused only on the demanding slices.
 * So during a reconfiguration process, some VMs of the two set may be hosted
 * on a common nodes.
 *
 * @author Fabien Hermenier
 */
public class LazySplit extends Split {

    /**
     * Make a new constraint.
     *
     * @param vmset1 the first set of virtual machines
     * @param vmset2 the second set of virtual machines
     */
    public LazySplit(ManagedElementSet<VirtualMachine> vmset1, ManagedElementSet<VirtualMachine> vmset2) {
        super(vmset1, vmset2);
    }

    /**
     * TODO documentation
     *
     * @param core
     */
    @Override
    public void inject(ReconfigurationProblem core) {
        List<ActionModel> actions1 = new LinkedList<ActionModel>();
        List<ActionModel> actions2 = new LinkedList<ActionModel>();
        for (VirtualMachine vm : getFirstSet()) {
            actions1.add(core.getAssociatedAction(vm));
        }

        for (VirtualMachine vm : getSecondSet()) {
            actions2.add(core.getAssociatedAction(vm));
        }

        List<DemandingSlice> slices1 = ActionModels.extractDemandingSlices(actions1);
        List<DemandingSlice> slices2 = ActionModels.extractDemandingSlices(actions2);

        IntDomainVar[] hosters1 = Slices.extractHosters(slices1);
        IntDomainVar[] hosters2 = Slices.extractHosters(slices2);
        for (int i = 0; i < hosters1.length; i++) {
            for (int j = 0; j < hosters2.length; j++) {
                core.post(core.neq(hosters1[i], hosters2[j]));
            }
        }


        //Remove non future-running VMs
        ManagedElementSet<VirtualMachine> goods = getFirstSet().clone();
        goods.retainAll(core.getFutureRunnings());

        ManagedElementSet<VirtualMachine> otherVMs = getSecondSet().clone();
        otherVMs.retainAll(core.getFutureRunnings());

        //Link the assignment variables with the set
        List<DemandingSlice> myDSlices = ActionModels.extractDemandingSlices(core.getAssociatedActions(goods));
        IntDomainVar[] myAssigns = Slices.extractHosters(myDSlices);

        List<DemandingSlice> otherDSlices = ActionModels.extractDemandingSlices(core.getAssociatedActions(otherVMs));
        IntDomainVar[] otherAssigns = Slices.extractHosters(otherDSlices);

        core.post(new Disjoint(core.getEnvironment(), myAssigns, otherAssigns, core.getNodes().length));
    }

    @Override
    public String toString() {
        return new StringBuilder("lSplit(").append(getFirstSet()).append(",").append(getSecondSet()).append(")").toString();
    }

    @Override
    public String toXML() {
        StringBuilder b = new StringBuilder(100);
        b.append("<constraint id=\"split\">");
        b.append("<params>");
        b.append("<param>").append(XmlVJobSerializer.getVMset(getFirstSet())).append("</param>");
        b.append("<param>").append(XmlVJobSerializer.getVMset(getSecondSet())).append("</param>");
        b.append("</params>");
        b.append("</constraint>");
        return b.toString();
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        PBVJob.vjob.Constraint.Builder b = PBVJob.vjob.Constraint.newBuilder();
        b.setId("split");
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getVMset(getFirstSet())).build());
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getVMset(getSecondSet())).build());
        return b.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Split that = (Split) o;
        return getFirstSet().equals(that.getFirstSet()) && getSecondSet().equals(that.getSecondSet());
    }

    @Override
    public int hashCode() {
        int result = getFirstSet().hashCode();
        result = 31 * result + getSecondSet().hashCode();
        result = 31 * result + "lazySplit".hashCode();
        return result;
    }
}
