
package entropy.plan.action;

import entropy.configuration.Configuration;
import entropy.configuration.Node;
import entropy.configuration.SimpleConfiguration;
import entropy.configuration.SimpleNode;
import org.testng.Assert;

import org.testng.annotations.Test;

@Test(groups = {"unit"})
public class TestRetype {
    /**
     * Test apply().
     */
    public void testApply() {
        Configuration c = new SimpleConfiguration();
        Node n1 = new SimpleNode("n1", 1, 2, 3);
        n1.setCurrentPlatform("foo");
        c.addOffline(n1);
        Retype s = new Retype(n1, "bar");
        s.apply(c);
        Assert.assertTrue(c.getOnlines().contains(n1));
        Assert.assertEquals(n1.getCurrentPlatform(), "bar");
    }
}
