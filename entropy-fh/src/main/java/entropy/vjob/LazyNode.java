package entropy.vjob;

import entropy.configuration.Configuration;
import entropy.configuration.DefaultManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.vjob.builder.plasma.ExplodedSet;
import entropy.vjob.builder.plasma.VJobSet;
import entropy.vjob.builder.protobuf.PBVJob.vjob.Constraint;

/**
 * requires a set of {@link Node}s to run at a maximum load.
 *
 * @author guillaume
 */
public class LazyNode implements PlacementConstraint {
    @SuppressWarnings("unused")
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
            .getLogger(LazyNode.class);

    /**
     * construct a constraint with internal parameters
     *
     * @param maxPCLoad the max percentage load (integer) of the host's CPU.
     * @param nodes     the set of nodes to apply this constraint on
     */
    public LazyNode(int maxPCLoad, VJobSet<Node> nodes) {
        setMaxPCLoad(maxPCLoad);
        setNodes(nodes);
    }

    /**
     * easier to use constructor that produces an internal vjobset
     *
     * @param nodes the array of nodes to convert to a {@link VJobSet}
     * @see #LazyNode(int, VJobSet)
     */
    public LazyNode(int maxPCLoad, Node... nodes) {
        setMaxPCLoad(maxPCLoad);
        DefaultManagedElementSet<Node> s = new DefaultManagedElementSet<Node>();
        for (Node n : nodes) {
            s.add(n);
        }
        setNodes(new ExplodedSet<Node>(s));
    }

    /**
     * the max percentage of load a specified node can be used at
     */
    private int maxPCLoad;

    /**
     * @return the maxPCLoad
     */
    public int getMaxPCLoad() {
        return maxPCLoad;
    }

    /**
     * @param maxPCLoad the maxPCLoad to set
     */
    public void setMaxPCLoad(int maxPCLoad) {
        this.maxPCLoad = maxPCLoad;
    }

    /**
     * the list of Hosts that are target of the constraint
     */
    private VJobSet<Node> nodes;

    /**
     * @param nodes the nodes to set
     */
    public void setNodes(VJobSet<Node> nodes) {
        this.nodes = nodes;
    }

    @Override
    public void inject(ReconfigurationProblem core) {
        if (getNodes().size() == 0) {
            VJob.logger.debug("Ignoring " + this + ", no nodes were specified");
            return;
        }
        for (Node n : getNodes()) {
            core.post(core.leq(core.getUsedCPU(n),
                    n.getCPUCapacity() * n.getNbOfCPUs() * getMaxPCLoad() / 100));
        }
    }

    @Override
    public boolean isSatisfied(Configuration cfg) {
        for (Node n : getNodes()) {
            int nodeLoad = 0;
            for (VirtualMachine vm : cfg.getRunnings(n)) {
                nodeLoad += vm.getCPUDemand();
            }
            // we want Load/(CPUCapa*nbCPU) <= maxPCLoad /100
            if (nodeLoad * 100 > getMaxPCLoad() * n.getCPUCapacity()
                    * n.getNbOfCPUs()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ExplodedSet<VirtualMachine> getAllVirtualMachines() {
        return new ExplodedSet<VirtualMachine>();
    }

    @Override
    public ExplodedSet<Node> getNodes() {
        return nodes.flatten();
    }

    @Override
    public ExplodedSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        ExplodedSet<VirtualMachine> ret = new ExplodedSet<VirtualMachine>();
        return ret;
    }

    @Override
    public String toString() {
        return getNodes() + ".totalPCLoad <= " + getMaxPCLoad() + "%";
    }

    @Override
    public String toXML() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("implement this !");
    }

    @Override
    public Constraint toProtobuf() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("implement this !");
    }

    @Override
    public Type getType() {
        return Type.relative;
    }
}
