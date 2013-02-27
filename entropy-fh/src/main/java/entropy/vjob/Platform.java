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

import choco.cp.solver.constraints.reified.ReifiedFactory;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.choco.Chocos;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.RetypeNodeActionModel;
import entropy.plan.choco.actionModel.slice.DemandingSlice;
import entropy.plan.durationEvaluator.DurationEvaluationException;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;

import java.util.*;

public class Platform implements PlacementConstraint {
    private ManagedElementSet<Node> nodes;
    private HashMap<Node, String> willChange;
    // somehow duplicate in TypedPlatform (List.indexOf(s) will allow String<->int correspondence)
    private static List<String> existingPlatforms = null;

    private static final ManagedElementSet<VirtualMachine> empty = new SimpleManagedElementSet<VirtualMachine>();

    /**
     * Make a new constraint.
     *
     * @param nodes the nodes to put online
     */
    public Platform(ManagedElementSet<Node> nodes, HashMap<Node,String> willChange) {
        this.nodes = nodes;
        this.willChange = willChange;

        if (existingPlatforms == null) {
            /* use set to avoid duplicates */
            Set<String> tmp = new HashSet<String>();
            for (Node n : nodes)
                tmp.addAll(n.getAvailablePlatforms());
            existingPlatforms = new ArrayList<String>();
            existingPlatforms.addAll(tmp);
        }
    }

    public Platform(ManagedElementSet<Node> nodes) {
       this(nodes, new HashMap<Node, String>());
    }

    /**
     *
     * @return true if every node that will change its platform are in n & if the new platform is valid
     */
    private boolean isValidPlatform() {
        for (Node n : willChange.keySet())
            if (!n.getAvailablePlatforms().contains(willChange.get(n)) || !nodes.contains(n))
                return false;

        return true;
    }

    @Override
    public void inject(ReconfigurationProblem core) {
        //TODO: No possible solution, need the false() constraint.
        if (!isValidPlatform())
            return;

        for (Node n : nodes) {
            /* unknown platform TODO false() constraint? (btw, should _never_ happen) */
            if (existingPlatforms.contains(n.getCurrentPlatform()))
                return;
            IntDomainVar start, end;
            try {
               int start_shutdown =  core.getDurationEvaluator().evaluateShutdown(n);
               start = core.createIntegerConstant("", start_shutdown);
               end = core.createIntegerConstant("", start_shutdown+core.getDurationEvaluator().evaluateStartup(n));
            }
            catch(DurationEvaluationException e) {
                System.err.println(e);
                return;
            }

            /*
             * every vm must leave before the shutdown (end)
             */
            for (VirtualMachine vm : core.getSourceConfiguration().getRunnings(n))
                core.post(core.leq(core.getAssociatedAction(vm).getConsumingSlice().end(), start));

            /*
             * constrain only VMs that may move on this node.
             * take an arbitrary node where the VM can be located, and compare
             * its platform type to n's future platform.
             */
            if (willChange.get(n) != null)
            for (DemandingSlice d : core.getDemandingSlices()) {
                // platform associated to slice
                IntDomainVar sp = core.createIntegerConstant("slicePlatform",
                        existingPlatforms.indexOf(core.getNode(d.hoster().getInf()).getCurrentPlatform()));
                // and the one associated to the node
                IntDomainVar np = core.createIntegerConstant("nodePlatform",
                        existingPlatforms.indexOf(willChange.get(n)));

                IntDomainVar eq = core.createBooleanVar("eq");
                core.post(ReifiedFactory.builder(eq, core.eq(sp, np), core));
                Chocos.postImplies(core, eq, core.geq(d.start(), core.plus(end, RetypeNodeActionModel.RETYPE_DURATION)));
            }
        }

    }

    @Override
    public boolean isSatisfied(Configuration cfg) {
        /* rapport.tex:^\\section{Formalisation} */
        for (VirtualMachine vm : cfg.getAllVirtualMachines()) {
            Node n = cfg.getLocation(vm);
            /* vm must be running on a node TODO need false() constraint? */
            if (n == null && cfg.isRunning(vm))
                return false;
            if (!vm.getHostingPlatform().equals(n.getCurrentPlatform()))
                return false;
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

    /*
     * check when called; here assumed before anything only
     * return all the vm on nodes which will change their platform
     */
    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        ManagedElementSet<VirtualMachine> set = new SimpleManagedElementSet<VirtualMachine>();
        for (Node n : willChange.keySet())
            for(VirtualMachine vm : cfg.getRunnings(n))
                set.add(vm);
        return set;
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

        return nodes.equals(q.nodes) && willChange.equals(q.willChange);
    }

    @Override
    public int hashCode() {
        return "platform".hashCode() * 31 + nodes.hashCode();
    }
}
