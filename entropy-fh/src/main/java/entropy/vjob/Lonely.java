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

//import choco.cp.solver.constraints.set.Disjoint;

import choco.cp.solver.constraints.global.Disjoint;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.slice.DemandingSlice;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//import entropy.plan.choco.constraint.Disjoint;

/**
 * A placement constraint to ensure the given set of VMs will not be hosted
 * on nodes that host other VMs
 *
 * @author Fabien Hermenier
 */
public class Lonely implements PlacementConstraint {

    private ManagedElementSet<VirtualMachine> vms;

    public Lonely(ManagedElementSet<VirtualMachine> vms) {
        this.vms = vms;
    }

    @Override
    public void inject(ReconfigurationProblem core) {

        //Remove non future-running VMs
        List<IntDomainVar> goods = new ArrayList<IntDomainVar>();
        List<IntDomainVar> otherVMs = new ArrayList<IntDomainVar>();
        for (VirtualMachine vm : core.getFutureRunnings()) {
            DemandingSlice s = core.getAssociatedAction(vm).getDemandingSlice();
            if (s != null) {
                if (vms.contains(vm)) {
                    goods.add(s.hoster());
                } else {
                    otherVMs.add(s.hoster());
                }
            }
        }
        //Link the assignment variables with the set
        core.post(new Disjoint(core.getEnvironment(), goods.toArray(new IntDomainVar[goods.size()]), otherVMs.toArray(new IntDomainVar[otherVMs.size()]), core.getNodes().length));
    }


    @Override
    public boolean isSatisfied(Configuration cfg) {

        Set<Node> s1 = new HashSet<Node>();
        for (VirtualMachine vm : vms) {
            if (cfg.isRunning(vm)) {
                s1.add(cfg.getLocation(vm));
            }
        }
        //If one of the other VMs is running into the nodes, then fail
        for (VirtualMachine vm : cfg.getRunnings()) {
            if (!vms.contains(vm) && s1.contains(cfg.getLocation(vm))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return vms;
    }

    @Override
    public ManagedElementSet<Node> getNodes() {
        return new SimpleManagedElementSet<Node>();
    }

    /**
     * If the constraint is not satisfied, then misplaced VMs are those
     * that share nodes with other VMs. The set is not necessarily
     * composed of only some VMs given as parameters
     *
     * @param cfg the configuration
     * @return a set of virtual machines that may be empty
     */
    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        ManagedElementSet<VirtualMachine> bad = new SimpleManagedElementSet<VirtualMachine>();
        Set<Node> s1 = new HashSet<Node>();
        for (VirtualMachine vm : vms) {
            if (cfg.isRunning(vm)) {
                s1.add(cfg.getLocation(vm));
            }
        }
        for (Node n : s1) { //Every used node that contains VMs in both group is bad
            for (VirtualMachine vm : cfg.getRunnings(n)) {
                if (!vms.contains(vm)) {
                    bad.addAll(cfg.getRunnings(n));
                    break;
                }
            }
        }
        return bad;
    }

    @Override
    public String toString() {
        return new StringBuilder("lonely(").append(vms).append(")").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Lonely lonely = (Lonely) o;
        return lonely.vms.equals(vms);
    }

    @Override
    public int hashCode() {
        return vms.hashCode() + "lonely".hashCode() * 31;
    }

    @Override
    public String toXML() {
        StringBuilder b = new StringBuilder();
        b.append("<constraint id=\"lonely\">");
        b.append("<params>");
        b.append("<param>").append(XmlVJobSerializer.getVMset(vms)).append("</param>");
        b.append("</params>");
        b.append("</constraint>");
        return b.toString();
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        PBVJob.vjob.Constraint.Builder b = PBVJob.vjob.Constraint.newBuilder();
        b.setId("lonely");
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getVMset(vms)).build());
        return b.build();
    }

    @Override
    public Type getType() {
        return Type.relative;
    }
}
