package entropy.vjob.constraint;

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
    /* simple test with 1 VM on 1 node */
    public void testIsSatisfied() {
        Configuration cfg = new SimpleConfiguration();
        Node n = new SimpleNode("N1", 10, 10, 10);
        n.addPlatform("foo");
        n.setCurrentPlatform("foo");
        VirtualMachine vm = new SimpleVirtualMachine("V1");
        vm.setHostingPlatform("foo");
        cfg.addOnline(n);
        cfg.setRunOn(vm, n);
        Platform c = new Platform(cfg.getAllNodes());
        Assert.assertTrue(c.isSatisfied(cfg));
    }
}
