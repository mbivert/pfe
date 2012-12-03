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
 * a placement constraint that requires VMs to be placed on hosts with a given
 * free amount of CPU.
 *
 * @author guillaume
 */
public class MargingHostCPU implements PlacementConstraint {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
            .getLogger(MargingHostCPU.class);

    public MargingHostCPU(int maxHostLoad, VirtualMachine... vms) {
        this.maxHostLoad = maxHostLoad;
        DefaultManagedElementSet<VirtualMachine> s = new DefaultManagedElementSet<VirtualMachine>();
        for (VirtualMachine vm : vms) {
            s.add(vm);
        }
        this.vms = new ExplodedSet<VirtualMachine>(s);
    }

    private int maxHostLoad;

    /**
     * @return the max load allowed for the VMs' host
     */
    public double getMaxHostLoad() {
        return maxHostLoad;
    }

    /**
     * The list of VMs involved in the constraint.
     */
    private VJobSet<VirtualMachine> vms;

    /**
     * Get the virtual machines involved in the constraint.
     *
     * @return a set of VMs. Should not be empty
     */
    @Override
    public ExplodedSet<VirtualMachine> getAllVirtualMachines() {
        return vms.flatten();
    }

    /**
     * Get the set of virtual machines involved in the constraint.
     *
     * @return a set of VMs, should not be empty
     */
    public VJobSet<VirtualMachine> getVirtualMachines() {
        return vms;
    }

    @Override
    public void inject(ReconfigurationProblem core) {
        for (VirtualMachine vm : vms) {
            core.post(core.leq(core.mult(core.getHostUSedCPU(vm), 100),
                    core.mult(core.getHostMaxCPU(vm), maxHostLoad)));
            // core.post(core.leq(core.getHostUSedCPU(vm),
            // core.div(core.mult(core.getHostMaxCPU(vm), maxHostLoad), 100)));
        }
    }

    @Override
    public boolean isSatisfied(Configuration cfg) {
        if (getAllVirtualMachines().size() == 0) {
            logger.debug("No virtual machines was specified");
            return true;
        }
        for (VirtualMachine vm : getAllVirtualMachines()) {
            if (cfg.isRunning(vm)) {
                Node n = cfg.getLocation(vm);
                int nodeCapa = n.getCPUCapacity() * n.getNbOfCPUs();
                int runningActivity = 0;
                for (VirtualMachine running : cfg.getRunnings(n)) {
                    runningActivity += running.getCPUDemand();
                }
                if ((double) runningActivity / nodeCapa > getMaxHostLoad()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ExplodedSet<Node> getNodes() {
        return new ExplodedSet<Node>();
    }

    @Override
    public ExplodedSet<VirtualMachine> getMisPlaced(Configuration cfg) {
        ExplodedSet<VirtualMachine> ret = new ExplodedSet<VirtualMachine>();
        if (getAllVirtualMachines().size() == 0) {
            logger.debug("No virtual machines was specified");
            return ret;
        }
        for (VirtualMachine vm : getAllVirtualMachines()) {
            if (cfg.isRunning(vm)) {
                Node n = cfg.getLocation(vm);
                int nodeCapa = n.getCPUCapacity() * n.getNbOfCPUs();
                int runningActivity = 0;
                for (VirtualMachine running : cfg.getRunnings(n)) {
                    runningActivity += running.getCPUDemand();
                }
                if ((double) runningActivity / nodeCapa > getMaxHostLoad()) {
                    ret.add(vm);
                }
            }
        }
        return ret;
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
