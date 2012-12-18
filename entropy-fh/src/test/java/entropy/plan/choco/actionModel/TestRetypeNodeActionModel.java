
package entropy.plan.choco.actionModel;

import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;
import entropy.PropertiesHelper;
import entropy.configuration.*;
import entropy.plan.PlanException;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.action.Retype;
import entropy.plan.choco.DefaultReconfigurationProblem;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.durationEvaluator.DurationEvaluator;
import entropy.plan.durationEvaluator.FastDurationEvaluatorFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;

@Test(groups = {"unit", "RP-core"})
public class TestRetypeNodeActionModel {

    public void testActionDetectionAndCreation() {
        ChocoLogging.setVerbosity(Verbosity.SEARCH);
        Configuration src = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 2, 42, 42);

        n1.addPlatform("orig"); n1.addPlatform("dest");
        n1.setCurrentPlatform("orig");

        VirtualMachine v1 = new SimpleVirtualMachine("V1", 1, 20, 20, 10, 10);
        VirtualMachine v2 = new SimpleVirtualMachine("V2", 1, 20, 20, 10, 10);
        v1.setHostingPlatform("orig");
        v2.setHostingPlatform("dest");

        src.addOnline(n1);
        src.setRunOn(v1, n1);

        ReconfigurationProblem rp = null;
        try {
            ManagedElementSet<VirtualMachine> empty = new SimpleManagedElementSet<VirtualMachine>();
            ManagedElementSet<VirtualMachine> on = new SimpleManagedElementSet<VirtualMachine>();
            ManagedElementSet<VirtualMachine> off = new SimpleManagedElementSet<VirtualMachine>();
            ManagedElementSet<VirtualMachine> all = new SimpleManagedElementSet<VirtualMachine>();
            // at the end of the process, v1 is down; v2 is up
            on.add(v2); off.add(v1);
            // both vms are manageable
            all.add(v1); all.add(v2);

            HashMap<Node, String> willChange = new HashMap<Node, String>();
            willChange.put(n1, "dest");

            PropertiesHelper props = new PropertiesHelper();
            DurationEvaluator de = FastDurationEvaluatorFactory.readFromProperties(props);

            rp = new DefaultReconfigurationProblem(src, on, empty,
                    empty, off, all, src.getOnlines(), src.getOfflines(),
                    de, willChange);
        }
        catch(Exception e) {
            System.err.println(e);
            Assert.fail(e.getMessage(), e);
        }

        Assert.assertTrue(rp.solve());

        Assert.assertTrue(rp.extractSolution().getDestination().isRunning(v2));
        Assert.assertFalse(rp.extractSolution().getDestination().isRunning(v1));

        for (Node n : rp.extractSolution().getDestination().getAllNodes()) {
            SimpleNode nn = (SimpleNode) n;
            if (nn.getName().equals("N1")) {
                Assert.assertEquals(nn.getCurrentPlatform(), "dest");
            }
        }

    }

    public void testActionDetectionAndCreation2() {
        ChocoLogging.setVerbosity(Verbosity.SEARCH);
        Configuration src = new SimpleConfiguration();
//        Configuration dst = new SimpleConfiguration();
        Node n = new SimpleNode("N1", 2, 42, 42);
        n.addPlatform("foo");        // old platform
        n.addPlatform("bar");        // new -
        n.setCurrentPlatform("foo");
        HashMap<Node,String> willChange =new HashMap<Node, String>();
        willChange.put(n, "bar");
        src.addOnline(n);
//        dst.addOffline(n);
//        ReconfigurationProblem m = TimedReconfigurationPlanModelHelper.makeBasicModel(src, dst);
        ManagedElementSet<Node> on = new SimpleManagedElementSet<Node>();
        on.add(n);
        ManagedElementSet<Node> off = new SimpleManagedElementSet<Node>();
        ManagedElementSet<VirtualMachine> empty = new SimpleManagedElementSet<VirtualMachine>();
        ReconfigurationProblem m = null;
        try {
            m = new DefaultReconfigurationProblem(src, empty, empty,
                    empty, empty, empty, on, off, null, willChange);
        }
        catch(entropy.plan.PlanException e) {
            System.err.println(e);
            Assert.assertFalse(true);
        }
        RetypeNodeActionModel a = (RetypeNodeActionModel) m.getAssociatedAction(n);
//        Assert.assertEquals(a.getNode(), n);
//        Assert.assertEquals(a.getDuration().getVal(), 8);
//        Assert.assertNotNull(a.getDemandingSlice());
//        Assert.assertEquals(a.getDemandingSlice().getCPUheight(), a.getNode().getCPUCapacity());
//        Assert.assertEquals(a.getDemandingSlice().getMemoryheight(), a.getNode().getMemoryCapacity());
        Assert.assertTrue(m.solve());
        Retype rt = (Retype) a.getDefinedAction(m).get(1);
//        Assert.assertEquals(rt.getNode(), n);
//        Assert.assertEquals(rt.getStartMoment(), 0);
//        Assert.assertEquals(rt.getFinishMoment(), 8);
//        Assert.assertEquals(a.getDuration().getVal(), 8);

        TimedReconfigurationPlan p = m.extractSolution();

        Assert.assertNotNull(p);
        System.out.println(p);
        for (Node n1 : p.getDestination().getAllNodes()) {
            SimpleNode n2 = (SimpleNode) n1;
            if (n2.getName().equals("N1")) {
                Assert.assertEquals(n2.getCurrentPlatform(), "bar");
            }

        }
    }
}
