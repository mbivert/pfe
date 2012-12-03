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

import entropy.configuration.*;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.choco.ChocoCustomRP;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.template.MockVirtualMachineTemplateFactory;
import entropy.vjob.DefaultVJob;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.Root;
import entropy.vjob.VJob;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.plasma.BasicPlasmaVJob;
import entropy.vjob.builder.plasma.PlasmaVJob;
import entropy.vjob.builder.protobuf.DefaultPBConstraintsCatalog;
import entropy.vjob.builder.protobuf.ProtobufVJobBuilder;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.protobuf.RootBuilder;
import entropy.vjob.builder.xml.DefaultXMLConstraintsCatalog;
import entropy.vjob.builder.xml.XMLVJobBuilder;
import entropy.vjob.builder.xml.XmlVJobSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for Root.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestRoot {

    /**
     * Test instantiation, getters, hashCode and equals
     */
    public void testBasics() {
        ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
        vms.add(new SimpleVirtualMachine("VM1", 1, 2, 3));
        vms.add(new SimpleVirtualMachine("VM2", 1, 2, 3));
        Root r = new Root(vms);
        Assert.assertFalse(r.toString().contains("null"));
        Assert.assertEquals(r.getNodes().size(), 0);
        Assert.assertEquals(r.getAllVirtualMachines(), vms);
        Assert.assertEquals(r.getMisPlaced(new SimpleConfiguration()).size(), 0);
        Assert.assertTrue(r.isSatisfied(new SimpleConfiguration()));

        Root r2 = new Root(vms);
        Assert.assertEquals(r, r2);
        Assert.assertEquals(r.hashCode(), r2.hashCode());
        ManagedElementSet<VirtualMachine> vms2 = vms.clone();
        vms2.remove(vms2.get("VM2"));
        r2 = new Root(vms2);
        Assert.assertNotEquals(r, r2);
        Assert.assertNotEquals(r.hashCode(), r2.hashCode());
    }

    public void simpleTest() {
        Configuration src = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 5, 5, 5);
        Node n2 = new SimpleNode("N2", 5, 5, 5);
        src.addOnline(n1);
        src.addOnline(n2);

        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 2, 2, 2);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 2, 2, 1);
        vm2.setCPUDemand(4);
        vm2.setCPUMax(5);
        src.setRunOn(vm1, n1);
        src.setRunOn(vm2, n1);
        Configuration dst = src.clone();
        ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>(vm2);
        try {
            ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 1, 2, 3, 4, 5, 6, 7, 8));
            List<VJob> vjobs = new ArrayList<VJob>();
            PlasmaVJob v = new BasicPlasmaVJob("v1");
            v.addConstraint(new Root(vms));
            vjobs.add(v);
            TimedReconfigurationPlan p = plan.compute(src,
                    dst.getRunnings(),
                    dst.getWaitings(),
                    dst.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    dst.getOnlines(),
                    dst.getOfflines(),
                    vjobs);
            Assert.assertEquals(p.size(), 1);
            Configuration res = p.getDestination();
            Assert.assertEquals(res.getLocation(vm1), n2);
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


        Root r = new Root(cfg.getAllVirtualMachines());
        VJob v = new DefaultVJob("V1");
        v.addConstraint(r);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            ProtobufVJobSerializer.getInstance().write(v, out.getPath());
            ProtobufVJobBuilder vb = new ProtobufVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null));
            vb.getElementBuilder().useConfiguration(cfg);
            DefaultPBConstraintsCatalog cat = new DefaultPBConstraintsCatalog();
            cat.add(new RootBuilder());
            vb.setConstraintCatalog(cat);
            VJob v2 = vb.build(out);
            Root r2 = (Root) v2.getConstraints().iterator().next();
            Assert.assertEquals(r, r2);
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

        Root r = new Root(cfg.getAllVirtualMachines());
        VJob v = new DefaultVJob("V1");
        v.addConstraint(r);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            DefaultXMLConstraintsCatalog cat = new DefaultXMLConstraintsCatalog();
            cat.add(new entropy.vjob.builder.xml.RootBuilder());
            XmlVJobSerializer.getInstance().write(v, out.getPath());
            XMLVJobBuilder vb = new XMLVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null), cat);
            vb.getElementBuilder().useConfiguration(cfg);
            VJob v2 = vb.build(out);
            Root r2 = (Root) v2.getConstraints().iterator().next();
            Assert.assertEquals(r, r2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
