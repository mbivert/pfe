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
import entropy.template.MockVirtualMachineTemplateFactory;
import entropy.vjob.DefaultVJob;
import entropy.vjob.Fence;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.VJob;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.plasma.BasicPlasmaVJob;
import entropy.vjob.builder.plasma.PlasmaVJob;
import entropy.vjob.builder.protobuf.DefaultPBConstraintsCatalog;
import entropy.vjob.builder.protobuf.FenceBuilder;
import entropy.vjob.builder.protobuf.ProtobufVJobBuilder;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.DefaultXMLConstraintsCatalog;
import entropy.vjob.builder.xml.XMLVJobBuilder;
import entropy.vjob.builder.xml.XmlVJobSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Unit tests for ChocoFence.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestFence {

    /**
     * Location of resources used for tests.
     */
    public static final String RESOURCES_LOCATION = "src/test/resources/entropy/vjob/constraint/TestFence.";

    public void testBasics() {
        Configuration cfg = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 1, 1);
        Node n2 = new SimpleNode("N2", 1, 1, 1);
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 1);
        VirtualMachine vm3 = new SimpleVirtualMachine("VM3", 1, 1, 1);

        cfg.addOnline(n1);
        cfg.addOnline(n2);
        cfg.setRunOn(vm1, n1);
        cfg.setRunOn(vm2, n1);
        cfg.setSleepOn(vm3, n2);

        Fence f = new Fence(cfg.getAllVirtualMachines(), cfg.getAllNodes());
        Assert.assertFalse(f.toString().contains("null"));
        Assert.assertEquals(f.getNodes(), cfg.getAllNodes());
        Assert.assertEquals(f.getVirtualMachines(), cfg.getAllVirtualMachines());

        Fence f2 = new Fence(cfg.getAllVirtualMachines(), cfg.getAllNodes());
        Assert.assertEquals(f, f2);
        Assert.assertEquals(f.hashCode(), f2.hashCode());

        ManagedElementSet<VirtualMachine> vms = cfg.getAllVirtualMachines().clone();
        vms.remove(vms.get("VM1"));

        f2 = new Fence(vms, cfg.getAllNodes());
        Assert.assertNotEquals(f, f2);
        Assert.assertNotEquals(f.hashCode(), f2.hashCode());

        ManagedElementSet<Node> ns = cfg.getAllNodes().clone();
        ns.remove(ns.get("N1"));

        f2 = new Fence(cfg.getAllVirtualMachines(), ns);
        Assert.assertNotEquals(f, f2);
        Assert.assertNotEquals(f.hashCode(), f2.hashCode());
    }


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
            List<VJob> vjobs = new ArrayList<VJob>();
            PlasmaVJob v = new BasicPlasmaVJob("v1");
            v.addVirtualMachines(t1);
            Set<ManagedElementSet<Node>> ns = new HashSet<ManagedElementSet<Node>>();
            ns.add(g1);
            v.addConstraint(new Fence(t1, g1));
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
        Assert.assertFalse(new Fence(cfg.getAllVirtualMachines(), new SimpleManagedElementSet<Node>()).isSatisfied(cfg));
        Assert.assertTrue(new Fence(vms, grp1).isSatisfied(cfg));
        grp1.remove(n1);
        Assert.assertFalse(new Fence(cfg.getAllVirtualMachines(), grp1).isSatisfied(cfg));
    }

    /**
     * Test getMisPlaced() in various situations.
     */
    public void testGetMisPlaced() {
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
        ManagedElementSet<VirtualMachine> bads = new Fence(cfg.getAllVirtualMachines(), new SimpleManagedElementSet<Node>()).getMisPlaced(cfg);
        Assert.assertEquals(bads.size(), 3);
        Assert.assertFalse(bads.contains(vm3));
        Assert.assertEquals(new Fence(vms, grp1).getMisPlaced(cfg).size(), 0);
        grp1.remove(n1);
        bads = new Fence(cfg.getAllVirtualMachines(), grp1).getMisPlaced(cfg);
        Assert.assertEquals(bads.size(), 2);
        Assert.assertTrue(bads.contains(vm4) && bads.contains(vm1));
    }

    /**
     * Test if protobuf serialization if fine by doing a cycle serialization/deserialization
     */
    public void testProtobufSerialization() {

        Configuration cfg = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 1, 1);
        Node n2 = new SimpleNode("N2", 1, 1, 1);
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 1);
        VirtualMachine vm3 = new SimpleVirtualMachine("VM3", 1, 1, 1);

        cfg.addOnline(n1);
        cfg.addOnline(n2);
        cfg.setRunOn(vm1, n1);
        cfg.setRunOn(vm2, n1);
        cfg.setSleepOn(vm3, n2);


        Fence f = new Fence(cfg.getAllVirtualMachines(), cfg.getAllNodes());
        VJob v = new DefaultVJob("V1");
        v.addConstraint(f);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            ProtobufVJobSerializer.getInstance().write(v, out.getPath());
            ProtobufVJobBuilder vb = new ProtobufVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null));
            vb.getElementBuilder().useConfiguration(cfg);
            DefaultPBConstraintsCatalog cat = new DefaultPBConstraintsCatalog();
            cat.add(new FenceBuilder());
            vb.setConstraintCatalog(cat);
            VJob v2 = vb.build(out);
            Fence f2 = (Fence) v2.getConstraints().iterator().next();
            Assert.assertEquals(f, f2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test if xml serialization if fine by doing a cycle serialization/deserialization
     */
    public void testXMLSerialization() {
        Configuration cfg = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 1, 1);
        Node n2 = new SimpleNode("N2", 1, 1, 1);
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 1);
        VirtualMachine vm3 = new SimpleVirtualMachine("VM3", 1, 1, 1);

        cfg.addOnline(n1);
        cfg.addOnline(n2);
        cfg.setRunOn(vm1, n1);
        cfg.setRunOn(vm2, n1);
        cfg.setSleepOn(vm3, n2);

        Fence f = new Fence(cfg.getAllVirtualMachines(), cfg.getAllNodes());
        VJob v = new DefaultVJob("V1");
        v.addConstraint(f);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            DefaultXMLConstraintsCatalog cat = new DefaultXMLConstraintsCatalog();
            cat.add(new entropy.vjob.builder.xml.FenceBuilder());
            XmlVJobSerializer.getInstance().write(v, out.getPath());
            XMLVJobBuilder vb = new XMLVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null), cat);
            vb.getElementBuilder().useConfiguration(cfg);
            VJob v2 = vb.build(out);
            Fence f2 = (Fence) v2.getConstraints().iterator().next();
            Assert.assertEquals(f, f2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
