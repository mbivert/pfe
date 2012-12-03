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
import entropy.vjob.Ban;
import entropy.vjob.DefaultVJob;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.VJob;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.plasma.BasicPlasmaVJob;
import entropy.vjob.builder.plasma.PlasmaVJob;
import entropy.vjob.builder.protobuf.BanBuilder;
import entropy.vjob.builder.protobuf.DefaultPBConstraintsCatalog;
import entropy.vjob.builder.protobuf.ProtobufVJobBuilder;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.DefaultXMLConstraintsCatalog;
import entropy.vjob.builder.xml.XMLVJobBuilder;
import entropy.vjob.builder.xml.XmlVJobSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Unit tests for ChocoBan.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestBan {

    /**
     * Location of resources used for tests.
     */
    public static final String RESOURCES_LOCATION = "src/test/resources/entropy/vjob/constraint/TestBan.";


    /**
     * Test with a set composed only with future running VMs.
     */
    public void testWithAllRunnings() {
        Configuration src = TestHelper.readConfiguration(RESOURCES_LOCATION + "src.txt");
        Configuration dst = TestHelper.readConfiguration(RESOURCES_LOCATION + "dst.txt");

        ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 1, 2, 3, 4, 5, 6, 7, 8));

        try {
            ManagedElementSet<Node> ns = new SimpleManagedElementSet<Node>();
            Node n1 = src.getAllNodes().get("N1");
            Node n3 = src.getAllNodes().get("N3");
            ns.add(n1);
            ns.add(n3);
            PlasmaVJob v = new BasicPlasmaVJob("v");
            v.addVirtualMachines(src.getAllVirtualMachines());
            ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
            VirtualMachine vm1 = v.getVirtualMachines().get("VM1");
            VirtualMachine vm2 = v.getVirtualMachines().get("VM2");
            VirtualMachine vm4 = v.getVirtualMachines().get("VM4");
            vms.add(vm1);
            vms.add(vm2);
            v.addConstraint(new Ban(vms, ns));
            v.addConstraint(new Ban(new SimpleManagedElementSet<VirtualMachine>(vm4), new SimpleManagedElementSet<Node>(n3)));
            v.addConstraint(new Ban(new SimpleManagedElementSet<VirtualMachine>(vm4), new SimpleManagedElementSet<Node>(src.getAllNodes().get("N2"))));
            List<VJob> queue = new LinkedList<VJob>();
            queue.add(v);
            TimedReconfigurationPlan p = plan.compute(src,
                    dst.getRunnings(),
                    dst.getWaitings(),
                    dst.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    dst.getOnlines(),
                    dst.getOfflines(),
                    queue);
            Configuration res = p.getDestination();
            Node n2 = res.getAllNodes().get("N2");
            Assert.assertEquals(p.size(), 2);
            Assert.assertEquals(res.getLocation(vm1), n2);
            Assert.assertEquals(res.getLocation(vm2), n2);
            Assert.assertEquals(res.getLocation(vm4), n1);

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
     * Test with a set composed with runnings & non-running VMs.
     * The constraint is not considered for non-running VMs.
     */
    public void testWithNonRunnings() {
        Configuration src = TestHelper.readConfiguration(RESOURCES_LOCATION + "src.txt");
        Configuration dst = TestHelper.readConfiguration(RESOURCES_LOCATION + "dst2.txt");
        ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 1, 2, 3, 4, 5, 6, 7, 8));
        plan.setRepairMode(false);
        try {
            ManagedElementSet<Node> ns = new SimpleManagedElementSet<Node>();
            Node n1 = src.getAllNodes().get("N1");
            Node n2 = src.getAllNodes().get("N2");
            Node n3 = src.getAllNodes().get("N3");
            ns.add(n1);
            ns.add(n2);
            PlasmaVJob v = new BasicPlasmaVJob("v");
            v.addVirtualMachines(src.getAllVirtualMachines());
            ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
            VirtualMachine vm3 = v.getVirtualMachines().get("VM3");
            VirtualMachine vm2 = v.getVirtualMachines().get("VM2");
            VirtualMachine vm4 = v.getVirtualMachines().get("VM4");
            vms.add(vm3);
            vms.add(vm2);
            Ban b1 = new Ban(vms, ns);
            v.addConstraint(b1);
            //An entailed constraint, vm4 is sleeping so stay on a node supposed to be avoided
            Ban b2 = new Ban(new SimpleManagedElementSet<VirtualMachine>(vm4), new SimpleManagedElementSet<Node>(n3));
            v.addConstraint(b2);
            List<VJob> queue = new LinkedList<VJob>();
            queue.add(v);
            TimedReconfigurationPlan p = plan.compute(src,
                    dst.getRunnings(),
                    dst.getWaitings(),
                    dst.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    dst.getOnlines(),
                    dst.getOfflines(),
                    queue);
            Configuration res = p.getDestination();
            Assert.assertEquals(p.size(), 2);
            Assert.assertEquals(res.getLocation(vm3), n3);
            Assert.assertEquals(res.getLocation(vm2), n2);
            Assert.assertEquals(res.getLocation(vm4), n3);

            Assert.assertTrue(b1.isSatisfied(res));
            Assert.assertTrue(b2.isSatisfied(res));

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
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 1);
        VirtualMachine vm3 = new SimpleVirtualMachine("VM3", 1, 1, 1);

        cfg.addOnline(n1);
        cfg.addOnline(n2);
        cfg.setRunOn(vm1, n1);
        cfg.setRunOn(vm2, n1);
        cfg.setSleepOn(vm3, n2);
        Assert.assertTrue(new Ban(cfg.getAllVirtualMachines(), new SimpleManagedElementSet<Node>(n2)).isSatisfied(cfg));
        Assert.assertFalse(new Ban(cfg.getAllVirtualMachines(), new SimpleManagedElementSet<Node>(n1)).isSatisfied(cfg));
        Assert.assertFalse(new Ban(cfg.getAllVirtualMachines(), cfg.getAllNodes()).isSatisfied(cfg));
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


        Ban b = new Ban(cfg.getAllVirtualMachines(), cfg.getAllNodes());
        VJob v = new DefaultVJob("V1");
        v.addConstraint(b);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            ProtobufVJobSerializer.getInstance().write(v, out.getPath());
            ProtobufVJobBuilder vb = new ProtobufVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null));
            vb.getElementBuilder().useConfiguration(cfg);
            DefaultPBConstraintsCatalog cat = new DefaultPBConstraintsCatalog();
            cat.add(new BanBuilder());
            vb.setConstraintCatalog(cat);
            VJob v2 = vb.build(out);
            Ban b2 = (Ban) v2.getConstraints().iterator().next();
            Assert.assertEquals(b, b2);
            Assert.assertEquals(b.hashCode(), b2.hashCode());
            Assert.assertEquals(b.getNodes(), b2.getNodes());
            Assert.assertEquals(b.getVirtualMachines(), b2.getVirtualMachines());
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

        Ban b = new Ban(cfg.getAllVirtualMachines(), cfg.getAllNodes());
        VJob v = new DefaultVJob("V1");
        v.addConstraint(b);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            DefaultXMLConstraintsCatalog cat = new DefaultXMLConstraintsCatalog();
            cat.add(new entropy.vjob.builder.xml.BanBuilder());
            XmlVJobSerializer.getInstance().write(v, out.getPath());
            XMLVJobBuilder vb = new XMLVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null), cat);
            vb.getElementBuilder().useConfiguration(cfg);
            VJob v2 = vb.build(out);
            Ban b2 = (Ban) v2.getConstraints().iterator().next();
            Assert.assertEquals(b, b2);
            Assert.assertEquals(b.hashCode(), b2.hashCode());
            Assert.assertEquals(b.getNodes(), b2.getNodes());
            Assert.assertEquals(b.getVirtualMachines(), b2.getVirtualMachines());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
