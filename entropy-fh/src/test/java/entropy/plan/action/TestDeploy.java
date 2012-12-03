/*
 * Copyright (c) Fabien Hermenier
 *
 *        This file is part of Entropy.
 *
 *        Entropy is free software: you can redistribute it and/or modify
 *        it under the terms of the GNU Lesser General Public License as published by
 *        the Free Software Foundation, either version 3 of the License, or
 *        (at your option) any later version.
 *
 *        Entropy is distributed in the hope that it will be useful,
 *        but WITHOUT ANY WARRANTY; without even the implied warranty of
 *        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *        GNU Lesser General Public License for more details.
 *
 *        You should have received a copy of the GNU Lesser General Public License
 *        along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package entropy.plan.action;

import entropy.configuration.*;
import entropy.plan.MockPlanVisualizer;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link Deploy}.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestDeploy {


    public void testInstantiation() {
        Node n = new SimpleNode("N1", 1, 2, 3);
        n.addPlatform("any");
        Deploy d = new Deploy(n, "any");
        Assert.assertEquals(d.getPlatform(), "any");

        d = new Deploy(n, "any", 5, 10);
        Assert.assertEquals(d.getStartMoment(), 5);
        Assert.assertEquals(d.getFinishMoment(), 10);
        Assert.assertNotNull(d.toString());
    }

    public void testIsSatisfied() {
        Configuration cfg = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 2, 3);
        Node n2 = new SimpleNode("N2", 1, 2, 3);
        Node n3 = new SimpleNode("N3", 1, 2, 3);
        Node n4 = new SimpleNode("N4", 1, 2, 3);

        n1.addPlatform("any");
        n2.addPlatform("any");
        n3.addPlatform("any");
        n4.addPlatform("any");

        cfg.addOnline(n1);
        cfg.addOffline(n2);
        cfg.addOnline(n3);
        cfg.addOnline(n4);
        VirtualMachine vm1 = new SimpleVirtualMachine("vm1", 1, 2, 3);
        VirtualMachine vm2 = new SimpleVirtualMachine("vm2", 1, 3, 6);
        VirtualMachine vm3 = new SimpleVirtualMachine("vm3", 1, 3, 6);
        VirtualMachine vm4 = new SimpleVirtualMachine("vm4", 1, 3, 6);
        cfg.setRunOn(vm1, n1);
        cfg.setSleepOn(vm3, n3);
        cfg.setSleepOn(vm2, n3);

        Deploy d1 = new Deploy(n1, "any");    //vm1 is running, bad
        Deploy d2 = new Deploy(n2, "any"); //offline node. Bad
        Deploy d3 = new Deploy(n3, "any"); //Sleeping & running VMs. Bad
        Deploy d4 = new Deploy(n4, "foo"); //Unavailable platform
        Deploy d5 = new Deploy(n4, "any"); //all good dude
        Assert.assertTrue(d5.isCompatibleWith(cfg));
        Assert.assertFalse(d4.isCompatibleWith(cfg));
        Assert.assertFalse(d3.isCompatibleWith(cfg));
        Assert.assertFalse(d2.isCompatibleWith(cfg));
        Assert.assertFalse(d1.isCompatibleWith(cfg));
    }

    public void testIsSatisfied2() {
        Configuration src = new SimpleConfiguration();

        Node n3 = new SimpleNode("N3", 1, 2, 3);

        n3.addPlatform("p1");
        n3.addPlatform("p2");

        src.addOnline(n3);
        Configuration dst = new SimpleConfiguration();

        Node n22 = n3.clone();
        dst.addOnline(n22);
        n22.setCurrentPlatform("p2");

        Deploy d = new Deploy(n3, "p2");
        Assert.assertTrue(d.isCompatibleWith(src, dst));
        dst.addOffline(n22);
        Assert.assertFalse(d.isCompatibleWith(src, dst));
        n22.setCurrentPlatform("p1");
        Assert.assertFalse(d.isCompatibleWith(src, dst));
        dst.setRunOn(new SimpleVirtualMachine("VM1", 1, 1, 1), n22);
        n22.setCurrentPlatform("p2");
        Assert.assertFalse(d.isCompatibleWith(src, dst));
    }

    public void testApply() {
        Configuration cfg = new SimpleConfiguration();

        Node n3 = new SimpleNode("N3", 1, 2, 3);

        n3.addPlatform("p1");
        n3.addPlatform("p2");

        cfg.addOnline(n3);
        Deploy p = new Deploy(n3, "p2");
        Assert.assertTrue(p.apply(cfg));
        Assert.assertEquals(n3.getCurrentPlatform(), "p2");
    }

    public void testVisuInject() {
        Node n3 = new SimpleNode("N3", 1, 2, 3);
        Deploy p = new Deploy(n3, "p2");
        MockPlanVisualizer v = new MockPlanVisualizer();
        p.injectToVisualizer(v);
        Assert.assertTrue(v.isInjected(p));
    }

}
