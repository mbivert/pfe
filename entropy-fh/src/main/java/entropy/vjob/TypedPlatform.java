package entropy.vjob;

import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: mb
 * Date: 2/5/13
 * Time: 11:53 AM
 * To change this template use File | Settings | File Templates.
 */
public class TypedPlatform implements PlacementConstraint {
    private ManagedElementSet<TypedNode> nodes;
    private static final ManagedElementSet<VirtualMachine> empty = new SimpleManagedElementSet<VirtualMachine>();


    public TypedPlatform(ManagedElementSet<TypedNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return empty;
    }

    @Override
    public void inject(ReconfigurationProblem core) {

    }

    @Override
    public ManagedElementSet<Node> getNodes() {
        return null; // TODO convert nodes to untyped node;
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
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        return empty;
    }

    @Override
    public String toXML() {
        StringBuilder b = new StringBuilder();
        b.append("<constraint id=\"platform\">");
        b.append("<params>");
        // TODO convert nodes
        //b.append("<param>").append(XmlVJobSerializer.getNodeset(nodes)).append("</param>");
        b.append("</params>");
        b.append("</constraint>");
        return b.toString();
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        PBVJob.vjob.Constraint.Builder b = PBVJob.vjob.Constraint.newBuilder();
        b.setId("typedplatform");
        // TODO convert nodes (TypedNode) to ManagedElementSet<Node>
        //b.addParam(PBVJob.vjob.Param.newBuilder().setType(PBVJob.vjob.Param.Type.SET).setSet(ProtobufVJobSerializer.getNodeset(nodes)).build());
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

        TypedPlatform q = (TypedPlatform) o;

        return nodes.equals(q.nodes);
    }

    @Override
    public int hashCode() {
        return "platform".hashCode() * 31 + nodes.hashCode();
    }
}
