package entropy.vjob;

import choco.DeprecatedChoco;
import choco.cp.solver.constraints.global.Occurrence;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.vjob.builder.protobuf.PBVJob;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;

import java.util.HashSet;
import java.util.Set;

public class TypedPlatform implements PlacementConstraint {
    private ManagedElementSet<TypedNode> nodes;
    private static final ManagedElementSet<VirtualMachine> empty = new SimpleManagedElementSet<VirtualMachine>();
    private Set<String> existingPlatforms = new HashSet<String>();


    public TypedPlatform(ManagedElementSet<TypedNode> nodes) {
        this.nodes = nodes;

        /* create list of available platforms */
        for (Node n : nodes)
            existingPlatforms.addAll(n.getAvailablePlatforms());
    }

    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return empty;
    }

    @Override
    public void inject(ReconfigurationProblem core) {
        // can't find choco.Problem nor choco.global.Occurence for
        // http://choco.sourceforge.net/api/choco/Problem.html#createOccurrence%28choco.integer.IntVar[],%20int,%20boolean,%20boolean%29
        for (Node n : nodes) {
            IntDomainVar len = core.createIntegerConstant("", n.getAvailablePlatforms().size()-1);
            //DeprecatedChoco.occurenceMin(0, len, n.getAvailablePlatforms().toArray());
        }
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
