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

import entropy.configuration.*;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.choco.ChocoCustomRP;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.template.MockVirtualMachineTemplateFactory;
import entropy.vjob.DefaultVJob;
import entropy.vjob.Gather;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.VJob;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.protobuf.DefaultPBConstraintsCatalog;
import entropy.vjob.builder.protobuf.GatherBuilder;
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
 * Unit tests for Gather.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestGather {

    public void testBasics() {
        ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
        vms.add(new SimpleVirtualMachine("VM1", 1, 2, 3));
        vms.add(new SimpleVirtualMachine("VM2", 1, 2, 3));
        Gather s = new Gather(vms);
        Assert.assertFalse(s.toString().contains("null"));
        Assert.assertEquals(s.getNodes().size(), 0);
        Assert.assertEquals(s.getAllVirtualMachines(), vms);

        Gather s2 = new Gather(vms);
        Assert.assertEquals(s, s2);
        Assert.assertEquals(s.hashCode(), s2.hashCode());
        ManagedElementSet<VirtualMachine> vms2 = vms.clone();
        vms2.remove(vms2.get("VM2"));
        s2 = new Gather(vms2);
        Assert.assertNotEquals(s, s2);
        Assert.assertNotEquals(s.hashCode(), s2.hashCode());
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
        VirtualMachine vm4 = new SimpleVirtualMachine("VM4", 1, 1, 1);

        cfg.addOnline(n1);
        cfg.addOnline(n2);
        cfg.setRunOn(vm1, n1);
        cfg.setRunOn(vm2, n1);
        cfg.setRunOn(vm4, n2);
        cfg.setSleepOn(vm3, n2);

        ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
        vms.add(vm1);
        vms.add(vm2);
        Assert.assertTrue(new Gather(new SimpleManagedElementSet<VirtualMachine>()).isSatisfied(cfg));
        Assert.assertFalse(new Gather(cfg.getAllVirtualMachines()).isSatisfied(cfg));
        Assert.assertTrue(new Gather(vms).isSatisfied(cfg));
    }

    public void testGetMisplaced() {
        Configuration cfg = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 1, 1);
        Node n2 = new SimpleNode("N2", 1, 1, 1);
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 1);
        VirtualMachine vm3 = new SimpleVirtualMachine("VM3", 1, 1, 1);
        VirtualMachine vm4 = new SimpleVirtualMachine("VM4", 1, 1, 1);

        cfg.addOnline(n1);
        cfg.addOnline(n2);
        cfg.setRunOn(vm1, n1);
        cfg.setRunOn(vm2, n1);
        cfg.setRunOn(vm4, n2);
        cfg.setSleepOn(vm3, n2);

        ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
        vms.add(vm1);
        vms.add(vm2);
        Assert.assertEquals(new Gather(new SimpleManagedElementSet<VirtualMachine>()).getMisPlaced(cfg), new SimpleManagedElementSet<VirtualMachine>());
        ManagedElementSet<VirtualMachine> baddies = cfg.getAllVirtualMachines();
        Assert.assertEquals(new Gather(baddies).getMisPlaced(cfg), baddies);
        Assert.assertEquals(new Gather(vms).getMisPlaced(cfg), new SimpleManagedElementSet<VirtualMachine>());
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


        Gather s = new Gather(cfg.getAllVirtualMachines());
        VJob v = new DefaultVJob("V1");
        v.addConstraint(s);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            ProtobufVJobSerializer.getInstance().write(v, out.getPath());
            ProtobufVJobBuilder vb = new ProtobufVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null));
            vb.getElementBuilder().useConfiguration(cfg);
            DefaultPBConstraintsCatalog cat = new DefaultPBConstraintsCatalog();
            cat.add(new GatherBuilder());
            vb.setConstraintCatalog(cat);
            VJob v2 = vb.build(out);
            Gather s2 = (Gather) v2.getConstraints().iterator().next();
            Assert.assertEquals(s, s2);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
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

        Gather r = new Gather(cfg.getAllVirtualMachines());
        VJob v = new DefaultVJob("V1");
        v.addConstraint(r);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            DefaultXMLConstraintsCatalog cat = new DefaultXMLConstraintsCatalog();
            cat.add(new entropy.vjob.builder.xml.GatherBuilder());
            XmlVJobSerializer.getInstance().write(v, out.getPath());
            XMLVJobBuilder vb = new XMLVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null), cat);
            vb.getElementBuilder().useConfiguration(cfg);
            VJob v2 = vb.build(out);
            Gather s2 = (Gather) v2.getConstraints().iterator().next();
            Assert.assertEquals(r, s2);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    public void testSolving() {
        Configuration cfg = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 5, 5);
        Node n2 = new SimpleNode("N2", 1, 5, 5);
        Node n3 = new SimpleNode("N3", 1, 5, 5);
        Node n4 = new SimpleNode("N4", 1, 10, 10);
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 5, 5);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 5, 5);
        VirtualMachine vm3 = new SimpleVirtualMachine("VM3", 1, 1, 1);
        VirtualMachine vm4 = new SimpleVirtualMachine("VM4", 1, 1, 1);
        cfg.addOnline(n1);
        cfg.addOnline(n2);
        cfg.addOnline(n4);
        //cfg.addOnline(n2);
        cfg.setRunOn(vm1, n1);
        cfg.setRunOn(vm2, n2);
        cfg.setRunOn(vm4, n4);
        cfg.setSleepOn(vm3, n2);
        ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
        vms.add(vm1);
        vms.add(vm2);
        vms.add(vm3);
        VJob v = new DefaultVJob("v1");
        v.addConstraint(new Gather(vms));

        ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 3, 2, 3, 4, 5, 6, 7, 8));
        plan.setRepairMode(false);
        List<VJob> vjobs = new ArrayList<VJob>();
        try {
            vjobs.add(v);
            TimedReconfigurationPlan p = plan.compute(cfg,
                    cfg.getRunnings(),
                    cfg.getWaitings(),
                    cfg.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    cfg.getOnlines(),
                    cfg.getOfflines(),
                    vjobs);
            Configuration res = p.getDestination();
            for (PlacementConstraint c : v.getConstraints()) {
                if (!c.isSatisfied(res)) {
                    Assert.fail(c + " is not satisfied");
                }
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            System.err.flush();
        }
    }
}
