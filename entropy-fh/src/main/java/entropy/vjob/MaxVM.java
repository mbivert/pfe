package entropy.vjob;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.action.Action;
import entropy.plan.action.Retype;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.vjob.builder.protobuf.PBVJob;

public class MaxVM implements PlacementConstraint {
    private int nVM;
    private String type;
    private ManagedElementSet<Node> nodes;

    /* constraint nodes with hypervisor type not to run more than nVM virtual machines */
    public MaxVM(ManagedElementSet<Node> nodes, String type, int nVM) {
        this.type = type;
        this.nVM = nVM;
        this.nodes = nodes;
    }

    public String toString() {
        return null;
    }

    public void inject(ReconfigurationProblem core) {
        IntDomainVar nt = core.createIntegerConstant("", 0);

        for (Node n : nodes)
            /* for each action associated with n */
            for (Action a : core.getAssociatedAction(n).getDefinedAction(core))
                /* if the action consist in retyping the node to the registred type */
                if (a instanceof Retype && ((Retype)a).getNewPlatform().equals(type))
                    core.leq(core.getNbHosted(n), nVM);
    }

    public boolean isSatisfied(Configuration cfg) {
        for (Node n : nodes)
            if (n.getCurrentPlatform().equals(type) && nVM < cfg.getRunnings(n).size())
                return false;
        return true;
    }

    public ManagedElementSet<VirtualMachine> getAllVirtualMachines() {
        return null;
    }

    public ManagedElementSet<Node> getNodes() {
        return null;
    }

    public ManagedElementSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        return null;
    }

    public String toXML() {
        return null;
    }

    public PBVJob.vjob.Constraint toProtobuf() {
        return null;
    }

    public Type getType() {
        return Type.relative;
    }

}
