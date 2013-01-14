package entropy.vjob.constraint;

import com.sun.org.apache.xml.internal.security.transforms.implementations.TransformC14N11;
import entropy.configuration.SimpleConfiguration;
import entropy.configuration.Configuration;
import entropy.configuration.SimpleNode;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.configuration.SimpleVirtualMachine;
import entropy.vjob.Platform;
import junit.framework.Assert;
import org.testng.annotations.Test;

@Test(groups = {"unit"})
public class TestPlatform {
    /*
     * check that vm0 can be placed on n and that vm1 can't.
     */
    public void testIsSatisfied() {
        Configuration cfg = new SimpleConfiguration();
        Node n = new SimpleNode("N1", 10, 10, 10);
        n.addPlatform("foo");
        n.setCurrentPlatform("foo");
        VirtualMachine vm0 = new SimpleVirtualMachine("V0");
        vm0.setHostingPlatform("foo");
        cfg.addOnline(n);
        cfg.setRunOn(vm0, n);
        Platform c = new Platform(cfg.getAllNodes());
        Assert.assertTrue(c.isSatisfied(cfg));

        VirtualMachine vm1 = new SimpleVirtualMachine("V1");
        vm1.setHostingPlatform("bar");
        cfg.setRunOn(vm1, n);
        c = new Platform(cfg.getAllNodes());
        Assert.assertFalse(c.isSatisfied(cfg));
    }

    /*
     * Create two nodes (n0, n1), two VMs (v0, v1).
     * n0 will change its platform type from 'foo' to 'bar'
     * n1 sticks with 'foo'
     * v0 (type 'foo') starts by running on n0 and shall
     * move to n1 after n0 changes its type.
     * v1 (type 'bar') is not running and shall be running
     * on n0 at the end of the process.
     */
    public void testInject() {
        Configuration cfg = new SimpleConfiguration();

        Node n0 = new SimpleNode("N0", 10, 10, 10);
        n0.addPlatform("foo");
        n0.addPlatform("bar");
        n0.setCurrentPlatform("foo");
        cfg.addOnline(n0);

        Node n1 = new SimpleNode("N1", 10, 10, 10);
        n1.addPlatform("foo");
        n1.setCurrentPlatform("foo");
        cfg.addOnline(n1);

        VirtualMachine v0 = new SimpleVirtualMachine("V0");
        VirtualMachine v1 = new SimpleVirtualMachine("V1");
        v0.setHostingPlatform("foo");
        v1.setHostingPlatform("bar");

        cfg.setRunOn(v0, n0);

        /* TODO */

        Assert.assertTrue(cfg.isRunning(v0));
        Assert.assertEquals(cfg.getLocation(v0), n1);
        Assert.assertEquals(cfg.getLocation(v1), n0);
    }
}
