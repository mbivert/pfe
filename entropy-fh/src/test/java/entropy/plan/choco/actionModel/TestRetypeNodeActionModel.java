
package entropy.plan.choco.actionModel;

import entropy.configuration.*;
import entropy.plan.PlanException;
import entropy.plan.action.Retype;
import entropy.plan.choco.DefaultReconfigurationProblem;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.durationEvaluator.DurationEvaluator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;

@Test(groups = {"unit", "RP-core"})
public class TestRetypeNodeActionModel {
    public void testActionDetectionAndCreation() {
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

//        System.err.println(m.extractSolution().getDestination().hashCode());

        for (Node n1 : m.extractSolution().getDestination().getAllNodes()) {
            SimpleNode n2 = (SimpleNode) n1;
            if (n2.getName().equals("N1")) {
                Assert.assertEquals(n2.getCurrentPlatform(), "bar");
            }

        }
    }
}
