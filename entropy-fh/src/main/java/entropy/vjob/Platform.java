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

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.RetypeNodeActionModel;
import entropy.plan.choco.actionModel.slice.DemandingSlice;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;
import java.util.HashMap;

/* XXX .ordinal to get integer */
enum AllPlatform {
    FOO("foo"),
    BAR("bar"),
    BAZ("baz"),
    ANY("any");
    private String name;

    AllPlatform(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static AllPlatform getPlatform(String s) {
        for (AllPlatform p : AllPlatform.values())
            if (p.getName().equals(s))
                return p;
        return null;
    }
}


public class Platform implements PlacementConstraint {
    private ManagedElementSet<Node> nodes;
    private HashMap<Node, String> willChange;
    /* start of the shutdown operation */
    private int sShutdown;
    /* duration of the shutdown op */
    private int dShutdown;

    private static final ManagedElementSet<VirtualMachine> empty = new SimpleManagedElementSet<VirtualMachine>();

    /**
     * Make a new constraint.
     *
     * @param nodes the nodes to put online
     */
    public Platform(ManagedElementSet<Node> nodes, HashMap<Node,String> willChange, int sShutdown, int dShutdown) {
        this.nodes = nodes;
        this.sShutdown = sShutdown;
        this.dShutdown = dShutdown;
        this.willChange = willChange;
    }

    public Platform(ManagedElementSet<Node> nodes) {
        this(nodes, new HashMap<Node, String>(), 0, 1);
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
        IntDomainVar start = core.createIntegerConstant("", sShutdown);
        IntDomainVar end = core.createIntegerConstant("", sShutdown+dShutdown);

        for (Node n : nodes) {
            AllPlatform np = AllPlatform.getPlatform(n.getCurrentPlatform());
            /* unknown platform TODO false() constraint? */
            if (np == null)
                return;
            /*
             * every vm must leave before the shutdown (end)
             */
            for (VirtualMachine vm : core.getSourceConfiguration().getRunnings(n))
                core.post(core.leq(core.getAssociatedAction(vm).getConsumingSlice().end(), start));

            /*
             * constrain only VMs that may move on this node.
             * take an arbitrary node where the VM can be located, and compare
             * its platform type to n's future platform.
             *
             * constraints vm and node to have the same platform id
             *
             */
            if (willChange.get(n) != null)
            for (DemandingSlice d : core.getDemandingSlices()) {
                String p = core.getNode(d.hoster().getInf()).getCurrentPlatform();
                if (p.equals(willChange.get(n))) {
                    core.post(core.geq(d.start(), core.plus(end, RetypeNodeActionModel.RETYPE_DURATION)));

                    AllPlatform vp = AllPlatform.getPlatform(p);
                    /* unknown platform TODO false() constraint? */
                    if (vp == null)
                        return;
                    core.post(core.eq(core.createIntegerConstant("", vp.ordinal()), np.ordinal()));
                }
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

    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        /* TODO get VMs misplaced in isSatisfied? */
        return empty;
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
