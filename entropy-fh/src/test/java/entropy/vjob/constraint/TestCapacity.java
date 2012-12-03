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

package entropy.vjob.constraint;

import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;
import entropy.configuration.*;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.choco.ChocoCustomRP;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSlicesHeightsFastBP;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.template.MockVirtualMachineTemplateFactory;
import entropy.vjob.Capacity;
import entropy.vjob.DefaultVJob;
import entropy.vjob.VJob;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.protobuf.CapacityBuilder;
import entropy.vjob.builder.protobuf.DefaultPBConstraintsCatalog;
import entropy.vjob.builder.protobuf.ProtobufVJobBuilder;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.DefaultXMLConstraintsCatalog;
import entropy.vjob.builder.xml.XMLVJobBuilder;
import entropy.vjob.builder.xml.XmlVJobSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the capacity constraint
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestCapacity {

    /**
     * Test instantiation, getters, hashCode and equals
     */
    public void testBasics() {
        ManagedElementSet<Node> ns = new SimpleManagedElementSet<Node>();
        ns.add(new SimpleNode("N1", 1, 2, 3));
        ns.add(new SimpleNode("N2", 1, 2, 3));
        Capacity c = new Capacity(ns, 5);
        Assert.assertFalse(c.toString().contains("null"));
        Assert.assertEquals(c.getNodes(), ns);
        Assert.assertEquals(c.getMaximumCapacity(), 5);
        Assert.assertEquals(c.getAllVirtualMachines().size(), 0);

        Capacity c2 = new Capacity(ns, 5);
        Assert.assertEquals(c, c2);
        Assert.assertEquals(c.hashCode(), c2.hashCode());
        ManagedElementSet<Node> ns2 = ns.clone();
        ns2.remove(ns2.get("N2"));
        c2 = new Capacity(ns2, 5);
        Assert.assertNotEquals(c, c2);
        Assert.assertNotEquals(c.hashCode(), c2.hashCode());

        c2 = new Capacity(ns2, 3);
        Assert.assertNotEquals(c, c2);
        Assert.assertNotEquals(c.hashCode(), c2.hashCode());

    }

    /**
     * Basic test, all the VMs are online, so as the nodes
     * Move some VMs from one partition to another one
     */
    public void test1() {
        //6 nodes, capacity of the 3 first of 5 VMs
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i <= 6; i++) {
            Node n = new SimpleNode("N" + i, 10, 10, 10);
            cfg.addOnline(n);
        }

        for (int i = 1; i <= 8; i++) {
            VirtualMachine vm = new SimpleVirtualMachine("VM" + i, 1, 1, 1);
            if (i % 3 == 0) {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N1"));
            } else if (i % 2 == 0) {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N2"));
            } else {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N3"));
            }
        }

        for (int i = 9; i <= 12; i++) {
            VirtualMachine vm = new SimpleVirtualMachine("VM" + i, 1, 1, 1);
            if (i % 3 == 0) {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N4"));
            } else if (i % 2 == 0) {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N5"));
            } else {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N6"));
            }
        }

        ManagedElementSet<Node> set = new SimpleManagedElementSet<Node>();
        set.add(cfg.getAllNodes().get("N1"));
        set.add(cfg.getAllNodes().get("N2"));
        set.add(cfg.getAllNodes().get("N3"));
        ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 3, 2, 3, 4, 5, 6, 7, 8));
        plan.setTimeLimit(0);
        ChocoLogging.setVerbosity(Verbosity.SILENT);
        plan.setRepairMode(false);
        List<VJob> vjobs = new ArrayList<VJob>();
        try {
            VJob v = new DefaultVJob("v1");
            v.addVirtualMachines(cfg.getAllVirtualMachines());
            Capacity c = new Capacity(set, 5);
            v.addConstraint(c);
            //Dumb
            Assert.assertEquals(c.getMaximumCapacity(), 5);
            vjobs.add(v);
            TimedReconfigurationPlan p = plan.compute(cfg,
                    cfg.getRunnings(),
                    cfg.getWaitings(),
                    cfg.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    cfg.getOnlines(),
                    cfg.getOfflines(),
                    vjobs);
            System.err.println(p);
            Configuration dst = p.getDestination();
            Assert.assertEquals(p.size(), 3);

        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            System.err.flush();
            ChocoLogging.flushLogs();
        }
    }

    /**
     * Trickier test.
     * while some VMs in the other partition will have to move to the restricted partition.
     * Last, one VM that was suspended on the restricted partition is resumed
     */
    public void test2() {
        //6 nodes, capacity of the 3 first of 5 VMs
        ChocoLogging.setVerbosity(Verbosity.SILENT);
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i <= 6; i++) {
            if (i <= 3) {
                Node n = new SimpleNode("N" + i, 10, 10, 10);
                cfg.addOnline(n);
            } else {
                Node n = new SimpleNode("N" + i, 5, 5, 5);
                cfg.addOnline(n);
            }
        }

        for (int i = 1; i <= 8; i++) {

            VirtualMachine vm = new SimpleVirtualMachine("VM" + i, 1, 1, 1);
            if (i % 3 == 0) {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N1"));
            } else if (i % 2 == 0) {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N2"));
            } else {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N3"));
            }
        }
        //A big VM on N6 that will have to move to N1 or N2
        VirtualMachine vmFat = new SimpleVirtualMachine("VMfat", 3, 3, 3);
        vmFat.setCPUDemand(7);
        cfg.setRunOn(vmFat, cfg.getAllNodes().get("N6"));

        //One suspended VM on N1 for the fun
        VirtualMachine vmxx = new SimpleVirtualMachine("VMxx", 1, 1, 1);
        cfg.setSleepOn(vmxx, cfg.getAllNodes().get("N1"));
        //System.err.println(Configurations.futureOverloadedNodes(cfg));

        //N3 will have to be turned off
        ManagedElementSet<Node> futureOnlines = cfg.getAllNodes().clone();
        futureOnlines.remove(futureOnlines.get("N3"));
        ManagedElementSet<Node> futureOfflines = new SimpleManagedElementSet<Node>();
        futureOnlines.add(cfg.getAllNodes().get("N3"));


        for (int i = 9; i <= 12; i++) {
            VirtualMachine vm = new SimpleVirtualMachine("VM" + i, 1, 1, 1);
            if (i % 3 == 0) {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N4"));
            } else if (i % 2 == 0) {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N5"));
            } else {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N6"));
            }
        }

        ManagedElementSet<Node> set = new SimpleManagedElementSet<Node>();
        set.add(cfg.getAllNodes().get("N1"));
        set.add(cfg.getAllNodes().get("N2"));
        set.add(cfg.getAllNodes().get("N3"));
        ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 3, 2, 3, 4, 5, 6, 7, 8));
        plan.setPackingConstraintClass(new SatisfyDemandingSlicesHeightsFastBP());
        //ChocoLogging.setVerbosity(Verbosity.SOLUTION);
        plan.setRepairMode(false);
        List<VJob> vjobs = new ArrayList<VJob>();
        try {
            VJob v = new DefaultVJob("v1");
            v.addVirtualMachines(cfg.getAllVirtualMachines());
            Capacity c = new Capacity(set, 5);
            v.addConstraint(c);
            //Dumb
            Assert.assertEquals(c.getMaximumCapacity(), 5);
            vjobs.add(v);
            //plan.setTimeLimit(10);

            TimedReconfigurationPlan p = plan.compute(cfg,
                    cfg.getAllVirtualMachines(),
                    cfg.getWaitings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    futureOnlines,
                    futureOfflines,
                    vjobs);
            System.err.println(p);
            Configuration dst = p.getDestination();
            //1 resume action
            //1 migration to the restricted partition
            //4 migrations to liberate the restricted partition ( 3 + 1 as a side effect of the incoming migration)
            Assert.assertEquals(p.size(), 6);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            System.err.flush();
            ChocoLogging.flushLogs();
        }
    }

    public void testIsSatisfied() {
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i <= 6; i++) {
            if (i <= 3) {
                Node n = new SimpleNode("N" + i, 10, 10, 10);
                cfg.addOnline(n);
            } else {
                Node n = new SimpleNode("N" + i, 5, 5, 5);
                cfg.addOnline(n);
            }
        }

        for (int i = 1; i <= 8; i++) {

            VirtualMachine vm = new SimpleVirtualMachine("VM" + i, 1, 1, 1);
            if (i % 3 == 0) {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N1"));
            } else if (i % 2 == 0) {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N2"));
            } else {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N3"));
            }
        }

        ManagedElementSet<Node> set = new SimpleManagedElementSet<Node>();
        set.add(cfg.getAllNodes().get("N1"));
        set.add(cfg.getAllNodes().get("N2"));
        set.add(cfg.getAllNodes().get("N3"));
        Capacity c = new Capacity(set, 5);
        Assert.assertFalse(c.isSatisfied(cfg));
    }

    public void testMisPlacedHeuristics() {
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i <= 6; i++) {
            if (i <= 3) {
                Node n = new SimpleNode("N" + i, 10, 10, 10);
                cfg.addOnline(n);
            } else {
                Node n = new SimpleNode("N" + i, 5, 5, 5);
                cfg.addOnline(n);
            }
        }


        for (int i = 1; i <= 8; i++) {
            VirtualMachine vm = new SimpleVirtualMachine("VM" + i, 1, 1, 1);
            if (i % 3 == 0) {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N1"));
            } else if (i % 2 == 0) {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N2"));
            } else {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N3"));
            }
        }
        ManagedElementSet<VirtualMachine> bad = cfg.getAllVirtualMachines().clone();

        for (int i = 9; i <= 12; i++) {
            VirtualMachine vm = new SimpleVirtualMachine("VM" + i, 1, 1, 1);
            if (i % 3 == 0) {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N4"));
            } else if (i % 2 == 0) {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N5"));
            } else {
                cfg.setRunOn(vm, cfg.getAllNodes().get("N6"));
            }
        }

        ManagedElementSet<Node> set = new SimpleManagedElementSet<Node>();
        set.add(cfg.getAllNodes().get("N1"));
        set.add(cfg.getAllNodes().get("N2"));
        set.add(cfg.getAllNodes().get("N3"));
        Capacity c = new Capacity(set, 5);
        ManagedElementSet<VirtualMachine> test = c.getMisPlaced(cfg);
        Assert.assertEquals(test.size(), bad.size());
        Assert.assertTrue(test.containsAll(bad));
    }

    /**
     * Test if protobuf serialization if fine by doing a cycle serialization/deserialization
     */
    public void testProtobufSerialization() {
        ManagedElementSet<Node> ns = new SimpleManagedElementSet<Node>();
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i < 5; i++) {
            Node n = new SimpleNode("N" + i, 1, 5 - i, 5 - i);
            ns.add(n);
            cfg.addOnline(n);
        }
        Capacity c1 = new Capacity(ns, 5);
        VJob v = new DefaultVJob("V1");
        v.addConstraint(c1);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            ProtobufVJobSerializer.getInstance().write(v, out.getPath());
            ProtobufVJobBuilder vb = new ProtobufVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null));
            vb.getElementBuilder().useConfiguration(cfg);
            DefaultPBConstraintsCatalog cat = new DefaultPBConstraintsCatalog();
            cat.add(new CapacityBuilder());
            vb.setConstraintCatalog(cat);
            VJob v2 = vb.build(out);
            Capacity c2 = (Capacity) v2.getConstraints().iterator().next();
            Assert.assertEquals(c1, c2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test if xml serialization if fine by doing a cycle serialization/deserialization
     */
    public void testXMLSerialization() {
        ManagedElementSet<Node> ns = new SimpleManagedElementSet<Node>();
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i < 5; i++) {
            Node n = new SimpleNode("N" + i, 1, 5 - i, 5 - i);
            ns.add(n);
            cfg.addOnline(n);
        }
        Capacity c1 = new Capacity(ns, 5);
        VJob v = new DefaultVJob("V1");
        v.addConstraint(c1);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            DefaultXMLConstraintsCatalog cat = new DefaultXMLConstraintsCatalog();
            cat.add(new entropy.vjob.builder.xml.CapacityBuilder());
            XmlVJobSerializer.getInstance().write(v, out.getPath());
            XMLVJobBuilder vb = new XMLVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null), cat);
            vb.getElementBuilder().useConfiguration(cfg);
            VJob v2 = vb.build(out);
            Capacity c2 = (Capacity) v2.getConstraints().iterator().next();
            Assert.assertEquals(c1, c2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
