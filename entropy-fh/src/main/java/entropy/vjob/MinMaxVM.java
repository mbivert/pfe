package entropy.vjob;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.vjob.builder.protobuf.PBVJob;

import java.util.List;


public class MinMaxVM implements PlacementConstraint {
    private int nVM;
    private String type;
    private ManagedElementSet<Node> nodes;
    boolean minmax; // true: min, false: max (maybe use enum)

    public MinMaxVM(ManagedElementSet<Node> nodes, boolean minmax, String type, int nVM) {
        this.minmax = minmax;
        this.type = type;
        this.nVM = nVM;
        this.nodes = nodes;
    }

    public String toString() {
        return null;
    }

    public void inject(ReconfigurationProblem core) {
        for (Node n : nodes) {
            if (n.getCurrentPlatform().equals(type)) {
                /* at least 'nVM' vm with hypervisor 'type' */
                if (minmax == true) {
                    core.post(core.leq(nVM, core.getNbHosted(n)));
                }
                /* at most … */
                else {
                    core.post(core.geq(nVM, core.getNbHosted(n)));
                }
            }
        }
    }

    public boolean isSatisfied(Configuration cfg) {
        for (Node n : nodes) {
            if (n.getCurrentPlatform().equals(type)) {
                /* at least */
                if (minmax == true && nVM > cfg.getRunnings(n).size())
                    return false;
                /* at most  */
                if (minmax == false && nVM < cfg.getRunnings(n).size())
                    return false;
            }
        }
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
