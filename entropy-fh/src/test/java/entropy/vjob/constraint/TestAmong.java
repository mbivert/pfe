/*
 * Copyright (c) 2010 Ecole des Mines de Nantes.
 *
 *      This file is part of Entropy.
 *
 *      Entropy is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      Entropy is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package entropy.vjob.constraint;

import entropy.TestHelper;
import entropy.configuration.*;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.choco.ChocoCustomRP;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.vjob.Among;
import entropy.vjob.DefaultVJob;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.VJob;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Unit tests for Among
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestAmong {

    public void testBasics() {
        Configuration cfg = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 1, 1);
        Node n2 = new SimpleNode("N2", 1, 1, 1);
        Node n3 = new SimpleNode("N3", 1, 1, 1);
        Node n4 = new SimpleNode("N4", 1, 1, 1);

        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 1);
        VirtualMachine vm3 = new SimpleVirtualMachine("VM3", 1, 1, 1);

        cfg.addOnline(n1);
        cfg.addOnline(n2);
        cfg.addOnline(n3);
        cfg.addOnline(n4);

        cfg.setRunOn(vm1, n1);
        cfg.setRunOn(vm2, n1);
        cfg.setSleepOn(vm3, n2);

        Set<ManagedElementSet<Node>> grps = new HashSet<ManagedElementSet<Node>>();
        ManagedElementSet<Node> g1 = new SimpleManagedElementSet<Node>();
        ManagedElementSet<Node> g2 = new SimpleManagedElementSet<Node>();
        g1.add(n1);
        g1.add(n2);
        g2.add(n3);
        g2.add(n4);
        grps.add(g1);
        grps.add(g2);
        Among a = new Among(cfg.getAllVirtualMachines(), grps);
        Assert.assertFalse(a.toString().contains("null"));
        Assert.assertEquals(a.getNodes(), cfg.getAllNodes());
        Assert.assertEquals(a.getGroups(), grps);
        Assert.assertEquals(a.getAllVirtualMachines(), cfg.getAllVirtualMachines());

        Among a2 = new Among(cfg.getAllVirtualMachines(), grps);
        Assert.assertEquals(a, a2);
        Assert.assertEquals(a.hashCode(), a2.hashCode());

        ManagedElementSet<VirtualMachine> vms = cfg.getAllVirtualMachines().clone();
        vms.remove(vms.get("VM1"));

        a2 = new Among(vms, grps);
        Assert.assertNotEquals(a, a2);
        Assert.assertNotEquals(a.hashCode(), a2.hashCode());

        ManagedElementSet<Node> ns = g1.clone();
        Set<ManagedElementSet<Node>> grps2 = new HashSet<ManagedElementSet<Node>>();
        ns.remove(ns.get("N1"));
        grps2.add(ns);
        grps2.add(g2);

        a2 = new Among(cfg.getAllVirtualMachines(), grps2);
        Assert.assertNotEquals(a, a2);
        Assert.assertNotEquals(a.hashCode(), a2.hashCode());
    }

    /**
     * Location of resources used for tests.
     */
    public static final String RESOURCES_LOCATION = "src/test/resources/entropy/vjob/constraint/TestOneOf.";

    public void testWithOneGroup() {
        Configuration src = TestHelper.readConfiguration(RESOURCES_LOCATION + "src.txt");
        Configuration dst = TestHelper.readConfiguration(RESOURCES_LOCATION + "dst.txt");
        ManagedElementSet<VirtualMachine> t1 = new SimpleManagedElementSet<VirtualMachine>();
        VirtualMachine vm1 = src.getAllVirtualMachines().get("VM1");
        VirtualMachine vm2 = src.getAllVirtualMachines().get("VM2");
        t1.add(vm1);
        t1.add(vm2);
        ManagedElementSet<Node> g1 = new SimpleManagedElementSet<Node>();
        Node n3 = src.getOnlines().get("N3");
        Node n4 = src.getOnlines().get("N4");
        g1.add(n3);
        g1.add(n4);
        try {
            ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 1, 2, 3, 4, 5, 6, 7, 8));
            //plan.setRepairMode(true);
            List<VJob> vjobs = new ArrayList<VJob>();
            VJob v = new DefaultVJob("v1");
            v.addVirtualMachines(t1);
            Set<ManagedElementSet<Node>> ns = new HashSet<ManagedElementSet<Node>>();
            ns.add(g1);
            v.addConstraint(new Among(t1, ns));
            vjobs.add(v);
            TimedReconfigurationPlan p = plan.compute(src,
                    dst.getRunnings(),
                    dst.getWaitings(),
                    dst.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    dst.getOnlines(),
                    dst.getOfflines(),
                    vjobs);
            Assert.assertEquals(p.size(), 3);
            Configuration res = p.getDestination();
            Node n1 = res.getAllNodes().get("N1");
            Node n2 = res.getAllNodes().get("N2");
            Assert.assertTrue(!res.getRunnings(n1).contains(vm1) && !res.getRunnings(n2).contains(vm1));
            Assert.assertTrue(!res.getRunnings(n1).contains(vm2) && !res.getRunnings(n2).contains(vm2));

            for (PlacementConstraint c : v.getConstraints()) {
                if (!c.isSatisfied(res)) {
                    Assert.fail(c + " is not satisfied");
                }
            }

        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    public void testWithTwoGroup() {
        Configuration src = TestHelper.readConfiguration(RESOURCES_LOCATION + "src.txt");
        Configuration dst = TestHelper.readConfiguration(RESOURCES_LOCATION + "dst.txt");
        ManagedElementSet<VirtualMachine> t1 = new SimpleManagedElementSet<VirtualMachine>();
        VirtualMachine vm1 = src.getAllVirtualMachines().get("VM1");
        VirtualMachine vm2 = src.getAllVirtualMachines().get("VM2");
        VirtualMachine vm3 = src.getAllVirtualMachines().get("VM3");
        t1.add(vm1);
        t1.add(vm2);
        t1.add(vm3);
        ManagedElementSet<Node> g1 = new SimpleManagedElementSet<Node>();
        ManagedElementSet<Node> g2 = new SimpleManagedElementSet<Node>();
        Node n3 = src.getOnlines().get("N3");
        Node n4 = src.getOnlines().get("N4");
        g1.add(n3);
        g1.add(n4);
        g2.add(src.getOnlines().get("N1"));
        g2.add(src.getOnlines().get("N2"));
        try {
            ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 1, 2, 3, 4, 5, 6, 7, 8));
            plan.setRepairMode(false);
            List<VJob> vjobs = new ArrayList<VJob>();
            VJob v = new DefaultVJob("v1");
            v.addVirtualMachines(t1);
            Set<ManagedElementSet<Node>> ns = new HashSet<ManagedElementSet<Node>>();
            ns.add(g1);
            ns.add(g2);
            Among f = new Among(t1, ns);
            v.addConstraint(f);
            vjobs.add(v);
            TimedReconfigurationPlan p = plan.compute(src,
                    dst.getRunnings(),
                    dst.getWaitings(),
                    dst.getSleepings(),
                    new DefaultManagedElementSet<VirtualMachine>(),
                    dst.getOnlines(),
                    dst.getOfflines(),
                    vjobs);
            System.err.println(p);
            Configuration res = p.getDestination();
            System.err.println(res);

            Assert.assertEquals(p.size(), 3);
            Node n1 = res.getAllNodes().get("N1");
            Node n2 = res.getAllNodes().get("N2");
            Assert.assertEquals(res.getRunnings(n1).size(), 0);
            Assert.assertEquals(res.getRunnings(n2).size(), 0);
            Assert.assertTrue(res.getRunnings(n3).contains(vm1) || res.getRunnings(n3).contains(vm2) || res.getRunnings(n3).contains(vm3));
            Assert.assertTrue(res.getRunnings(n4).contains(vm1) || res.getRunnings(n4).contains(vm2) || res.getRunnings(n4).contains(vm3));
            for (PlacementConstraint c : v.getConstraints()) {
                if (!c.isSatisfied(res)) {
                    Assert.fail(c + " is not satisfied");
                }
            }

        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    /**
     * Test isSatisfied() in various situations.
     */
    public void testIsSatisfied() {
        Configuration cfg = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 1, 1);
        Node n2 = new SimpleNode("N2", 1, 1, 1);
        Node n3 = new SimpleNode("N3", 1, 1, 1);
        Node n4 = new SimpleNode("N4", 1, 1, 1);
        Set<ManagedElementSet<Node>> grps = new HashSet<ManagedElementSet<Node>>();

        ManagedElementSet<Node> grp1 = new SimpleManagedElementSet<Node>();
        grp1.add(n1);
        grp1.add(n2);
        grps.add(grp1);

        ManagedElementSet<Node> grp2 = new SimpleManagedElementSet<Node>();
        grp2.add(n3);
        grp2.add(n4);
        grps.add(grp2);

        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 1);
        VirtualMachine vm3 = new SimpleVirtualMachine("VM3", 1, 1, 1);
        VirtualMachine vm4 = new SimpleVirtualMachine("VM4", 1, 1, 1);

        cfg.addOnline(n1);
        cfg.addOnline(n2);
        cfg.addOnline(n3);
        cfg.addOnline(n4);
        cfg.setRunOn(vm1, n1);
        cfg.setRunOn(vm2, n2);
        cfg.setRunOn(vm4, n3);
        cfg.setSleepOn(vm3, n2);

        ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
        vms.add(vm1);
        vms.add(vm2);
        vms.add(vm3);
        Assert.assertFalse(new Among(cfg.getAllVirtualMachines(), grps).isSatisfied(cfg));
        vms.remove(vm4);
        Assert.assertTrue(new Among(vms, grps).isSatisfied(cfg));
        grp1.remove(n1);
        Assert.assertFalse(new Among(cfg.getAllVirtualMachines(), grps).isSatisfied(cfg));
    }
}
