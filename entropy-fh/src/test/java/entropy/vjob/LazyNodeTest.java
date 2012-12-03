package entropy.vjob;

import entropy.configuration.*;
import entropy.plan.PlanException;
import entropy.plan.choco.ChocoCustomRP;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.vjob.builder.plasma.ExplodedSet;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class LazyNodeTest {
    @SuppressWarnings("unused")
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
            .getLogger(LazyNodeTest.class);

    @Test
    public void testIsSatisfied() {
        entropy.configuration.Configuration cfg = makeVaryingLoadConfiguration();
        ManagedElementSet<Node> nodes = cfg.getAllNodes();
        for (int nodei = 0; nodei < nodes.size(); nodei++) {
            Node n = nodes.get("N" + nodei);
            for (int maxLoad = 0; maxLoad < 100; maxLoad += 10) {
                LazyNode toTest = new LazyNode(maxLoad, new ExplodedSet<Node>(
                        new SimpleManagedElementSet<Node>(n)));
                // satisfied IF real load (=10*nodei)<= maxload
                Assert.assertEquals(toTest.isSatisfied(cfg), maxLoad >= 10 * nodei,
                        "error while applying constraint " + toTest + " to cfg " + cfg);
            }
        }
    }

    /**
     * configuration in which each node N[i in 0..9] has a VMi on it that consumes
     * i*10 % of Ni CPU<br />
     * CPU RAM:1024MB CPU:1x100<br />
     * VM RAM:128 CPU:i*10
     */
    public static SimpleConfiguration makeVaryingLoadConfiguration() {
        SimpleConfiguration ret = new SimpleConfiguration();
        for (int i = 0; i < 10; i++) {
            Node n = new SimpleNode("N" + i, 1, 100, 1024);
            ret.addOnline(n);
            VirtualMachine vm = new SimpleVirtualMachine("VM" + i, 1, i * 10, 128);
            ret.setRunOn(vm, n);
        }
        return ret;
    }

    @Test
    public void testInject() throws PlanException {
        // we want one node with 100% load, and 3 nodes with 25% load
        int lowNodesLoad = 25;
        int nbVMs = 100 / 5 + 3 * lowNodesLoad / 5;
        SimpleConfiguration cfg = makeIdleVMsConfiguration(nbVMs);
        ManagedElementSet<Node> nodes = cfg.getAllNodes();
        LazyNode toTest = new LazyNode(lowNodesLoad, nodes.get("N0"),
                nodes.get("N1"), nodes.get("N2"));
        ChocoCustomRP solver = new ChocoCustomRP(new MockDurationEvaluator(1, 2, 3,
                4, 5, 6, 7, 8, 0));
        List<VJob> vjobs = new ArrayList<VJob>();
        VJob vjob = new DefaultVJob("vjob1");
        vjob.addConstraint(toTest);
        vjobs.add(vjob);
        entropy.plan.TimedReconfigurationPlan result = solver.compute(cfg,
                cfg.getAllVirtualMachines(),
                new SimpleManagedElementSet<VirtualMachine>(),
                new SimpleManagedElementSet<VirtualMachine>(),
                new SimpleManagedElementSet<VirtualMachine>(), cfg.getAllNodes(),
                new SimpleManagedElementSet<Node>(),
                vjobs);
        Configuration dest = result.getDestination();
        StringBuilder error = null;
        for (int i = 0; i < 3; i++) {
            Node n = nodes.get("N" + i);
            int totalLoad = 0;
            for (VirtualMachine vm : dest.getRunnings(n)) {
                totalLoad += vm.getCPUDemand();
            }
            if (totalLoad != lowNodesLoad) {
                if (error == null) {
                    error = new StringBuilder("total load on node " + n + " = "
                            + totalLoad);
                } else {
                    error.append(";total load on node " + n + " = " + totalLoad);
                }
            }
        }
        {
            Node n = nodes.get("N3");
            int totalLoad = 0;
            for (VirtualMachine vm : dest.getRunnings(n)) {
                totalLoad += vm.getCPUDemand();
            }
            Assert.assertTrue(totalLoad == 100, "total load on node " + n + " = "
                    + totalLoad);
        }
        Assert.assertTrue(error == null, "" + error);
    }

    /**
     * @return a configuration with 4 Nodes , at 100 monocore CPU capacity and
     *         1024 MB RAM, plus n VirtualMachine, waiting, each of one using 5
     *         CPU unit and 1 MB of ram.<br />
     *         Nodes are identified as N0, N1, N2, N3.<br />
     *         VMs are identified as VM0, ..., VMn
     */
    public static SimpleConfiguration makeIdleVMsConfiguration(int nbVMs) {
        SimpleConfiguration ret = new SimpleConfiguration();
        for (int i = 0; i < 4; i++) {
            Node n = new SimpleNode("N" + i, 1, 100, 1024);
            ret.addOnline(n);
        }
        for (int i = 0; i < nbVMs; i++) {
            VirtualMachine vm = new SimpleVirtualMachine("VM" + i, 1, 5, 1);
            ret.addWaiting(vm);
        }
        return ret;
    }

}
