
package entropy.plan.choco.actionModel;

import entropy.configuration.Configuration;
import entropy.configuration.Node;
import entropy.configuration.SimpleConfiguration;
import entropy.configuration.SimpleNode;
import entropy.plan.action.Retype;
import entropy.plan.choco.ReconfigurationProblem;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = {"unit", "RP-core"})
public class TestRetypeNodeActionModel {
    public void testActionDetectionAndCreation() {
        Configuration src = new SimpleConfiguration();
        Configuration dst = new SimpleConfiguration();
        Node n = new SimpleNode("N1", 1, 1, 1);
        n.addPlatform("foo");
        n.addPlatform(RetypeNodeActionModel.newPlatform);
        n.setCurrentPlatform("foo");
        src.addOnline(n);
        dst.addOnline(n);
        ReconfigurationProblem m = TimedReconfigurationPlanModelHelper.makeBasicModel(src, dst);
        System.err.println(m.getAssociatedAction(n));
        RetypeNodeActionModel a = (RetypeNodeActionModel) m.getAssociatedAction(n);
        Assert.assertEquals(a.getNode(), n);
//        Assert.assertEquals(a.getDuration().getVal(), 8);
        Assert.assertNotNull(a.getDemandingSlice());
        Assert.assertEquals(a.getDemandingSlice().getCPUheight(), a.getNode().getCPUCapacity());
        Assert.assertEquals(a.getDemandingSlice().getMemoryheight(), a.getNode().getMemoryCapacity());
        Assert.assertTrue(m.solve());
        Retype rt = (Retype) a.getDefinedAction(m).get(0);
        Assert.assertEquals(rt.getNode(), n);
        Assert.assertEquals(rt.getStartMoment(), 0);
//        Assert.assertEquals(rt.getFinishMoment(), 8);
//        Assert.assertEquals(a.getDuration().getVal(), 8);

        Assert.assertEquals(a.getNode().getCurrentPlatform(),
        	RetypeNodeActionModel.newPlatform);
    }
}
