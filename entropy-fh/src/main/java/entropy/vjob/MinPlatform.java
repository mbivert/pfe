package entropy.vjob;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.action.Action;
import entropy.plan.action.Retype;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.NodeActionModel;
import entropy.vjob.builder.protobuf.PBVJob;

public class MinPlatform implements PlacementConstraint {
    private int nType;
    private String type;
    private ManagedElementSet<Node> nodes;

    /* constraint 'nodes' set so that at least 'nType' of them run 'type' hypervisor*/
    public MinPlatform(ManagedElementSet<Node> nodes, String type, int nType) {
        this.type = type;
        this.nType = nType;
        this.nodes = nodes;
    }

    @Override
    public void inject(ReconfigurationProblem core) {
        IntDomainVar nt = core.createIntegerConstant("", 0);
        for (Node n : nodes)
            /* for each action associated with n */
            for (Action a : core.getAssociatedAction(n).getDefinedAction(core)) {
                /* if the action consist in retyping the node to the registred type */
                if (a instanceof Retype && ((Retype)a).getNewPlatform().equals(type))
                    core.plus(nt, 1);
            }
        core.geq(nt, nType);
    }

    @Override
    public boolean isSatisfied(Configuration cfg) {
        int i = 0;
        for (Node n : nodes)
            if (n.getCurrentPlatform().equals(type))
                i++;
        return i <= nType;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return null;
    }

    @Override
    public ManagedElementSet<Node> getNodes() {
        return null;
    }

    @Override
    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        return null;
    }

    @Override
    public String toXML() {
        return null;
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        return null;
    }

    @Override
    public Type getType() {
        return null;
    }
}
