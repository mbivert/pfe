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

import choco.cp.solver.constraints.integer.MyElement;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.Plan;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;

import java.util.Iterator;
import java.util.Set;

/**
 * A constraint to enforce a set of virtual machines
 * to be hosted on a single group of physical elements among those give in parameters.
 *
 * @author Fabien Hermenier
 */
public class Among implements PlacementConstraint {

    /**
     * The set of possible groups of nodes.
     */
    private Set<ManagedElementSet<Node>> groups;

    /**
     * The list of VMs involved in the constraint.
     */
    private ManagedElementSet<VirtualMachine> vms;

    private ManagedElementSet<Node> allNodes;

    /**
     * Make a new constraint that enforce all the virtual machines
     * to be hosted on a single group of node among those given in parameters.
     *
     * @param vms    the set of VMs to assign.
     * @param groups the list of possible groups of nodes.
     */
    public Among(ManagedElementSet<VirtualMachine> vms, Set<ManagedElementSet<Node>> groups) {
        this.vms = vms;
        this.groups = groups;
        this.allNodes = null;
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

    @Override
    public ManagedElementSet<Node> getNodes() {
        if (allNodes == null) {
            allNodes = new SimpleManagedElementSet<Node>();
            for (ManagedElementSet<Node> grp : groups) {
                allNodes.addAll(grp);
            }
        }
        return allNodes;
    }

    /**
     * Get the different groups of nodes involved in the constraint.
     *
     * @return a set of groups. May be empty
     */
    public Set<ManagedElementSet<Node>> getGroups() {
        return this.groups;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("among(").append(vms);
        buffer.append(", {");
        for (Iterator<ManagedElementSet<Node>> ite = groups.iterator(); ite.hasNext(); ) {
            buffer.append(ite.next().toString());
            if (ite.hasNext()) {
                buffer.append(',');
            }
        }
        buffer.append("})");
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
        Among that = (Among) o;
        return groups.equals(that.groups) && vms.equals(that.vms);
    }

    @Override
    public int hashCode() {
        int result = vms.hashCode();
        result = 31 * result + groups.hashCode();
        result = 31 * result + "among".hashCode();
        return result;
    }

    @Override
    public void inject(ReconfigurationProblem core) {

        if (groups.isEmpty()) {
            VJob.logger.debug("Ignoring " + this + ", no groups were specified");
            return;
        }

        if (groups.size() == 1 && !groups.iterator().next().equals(core.getFutureOnlines())) {
            new Fence(vms, groups.iterator().next()).inject(core);
        } else {
            //Get only the future running VMs
            ManagedElementSet<VirtualMachine> runnings = new SimpleManagedElementSet<VirtualMachine>();
            for (VirtualMachine vm : vms) {
                if (core.getFutureRunnings().contains(vm)) {
                    runnings.add(vm);
                }
            }

            //Now, we create a group variable & all that stuff
            IntDomainVar vmGrpId = core.makeGroup(vms, groups);

            if (vmGrpId.isInstantiated()) {
                new Fence(runnings, core.getNodeGroup(vmGrpId.getVal())).inject(core);
            } else {
                for (VirtualMachine vm : runnings) {
                    IntDomainVar assign = core.getAssociatedAction(vm).getDemandingSlice().hoster();
                    if (assign.isInstantiated()) {
                        if (vmGrpId.isInstantiated() && vmGrpId.getVal() != core.getNodesGroupId()[assign.getVal()]) {
                            Plan.logger.error("Unconsistent among");
                        } else if (!vmGrpId.isInstantiated()) {
                            try {
                                vmGrpId.setVal(core.getNodesGroupId()[assign.getVal()]);
                            } catch (ContradictionException e) {
                                Plan.logger.error(e.getMessage(), e);
                            }
                            break;
                        }

                    }
                }
                if (vmGrpId.isInstantiated()) {
                    new Fence(runnings, core.getNodeGroup(vmGrpId.getVal())).inject(core);
                } else {
                    new Fence(runnings, getNodes()).inject(core);
                    //new Fence(runnings, core.getNodeGroup(vmGrpId.getVal())).inject(core);
                    for (VirtualMachine vm : runnings) {
                        IntDomainVar assign = core.getAssociatedAction(vm).getDemandingSlice().hoster();
                        //System.err.println(Arrays.toString(core.getNodesGroupId()));
                        SConstraint c = new MyElement(assign, core.getNodesGroupId(), vmGrpId, 0, MyElement.Sort.detect);
                        //SConstraint c = new Element(assign, core.getNodesGroupId(), vmGrpId, 0);
                        core.post(c);
                    }
                }
            }
        }
    }

    /**
     * Check that the constraint is satisfied in a configuration.
     *
     * @param cfg the configuration to check
     * @return false if the running VMs are hosted on more than one group
     */
    @Override
    public boolean isSatisfied(Configuration cfg) {
        if (groups.isEmpty()) {
            VJob.logger.error("No group of nodes was specified");
            return false;
        }

        VirtualMachine vm1 = null;
        for (VirtualMachine vm : vms) {
            if (cfg.isRunning(vm)) {
                vm1 = vm;
                break;
            }
        }
        if (vm1 == null) {
            //No running VMs, no need to check
            return true;
        }
        Node n1 = cfg.getLocation(vm1);
        ManagedElementSet<Node> selectedGroup = null;
        for (ManagedElementSet<Node> grp : groups) {
            if (grp.contains(n1)) {
                selectedGroup = grp;
                break;
            }
        }
        if (selectedGroup == null) {
            VJob.logger.error(this + ": " + vm1.getName() + " is running on an invalid group");
            return false;
        }
        for (VirtualMachine vm : vms) {
            Node n = cfg.getLocation(vm);
            if (cfg.isRunning(vm)) {
                if (!selectedGroup.contains(n)) {
                    return false;
                }
            }
        }

        return true;
    }


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
        b.append("<constraint id=\"among\">");
        b.append("<params>");
        b.append("<param>").append(XmlVJobSerializer.getVMset(vms)).append("</param>");
        b.append("<param>").append(XmlVJobSerializer.getNodeBigSet(groups)).append("</param>");
        b.append("</params>");
        b.append("</constraint>");
        return b.toString();
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        PBVJob.vjob.Constraint.Builder b = PBVJob.vjob.Constraint.newBuilder();
        b.setId("among");
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getVMset(vms)).build());
        b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getNodeBigSet(groups)).build());
        return b.build();
    }

    @Override
    public Type getType() {
        return Type.relative;
    }
}