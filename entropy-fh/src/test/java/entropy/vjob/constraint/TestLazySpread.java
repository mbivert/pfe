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
import entropy.vjob.*;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.protobuf.DefaultPBConstraintsCatalog;
import entropy.vjob.builder.protobuf.LazySpreadBuilder;
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
 * Unit tests for ChocoLazySpread.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestLazySpread {

    /**
     * Location of resources used for tests.
     */
    public static final String RESOURCES_LOCATION = "src/test/resources/entropy/vjob/constraint/TestSpread.";

    public void testBasics() {
        ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
        vms.add(new SimpleVirtualMachine("VM1", 1, 2, 3));
        vms.add(new SimpleVirtualMachine("VM2", 1, 2, 3));
        Spread s = new LazySpread(vms);
        Assert.assertFalse(s.toString().contains("null"));
        Assert.assertEquals(s.getNodes().size(), 0);
        Assert.assertEquals(s.getAllVirtualMachines(), vms);

        Spread s2 = new LazySpread(vms);
        Assert.assertEquals(s, s2);
        Assert.assertEquals(s.hashCode(), s2.hashCode());
        ManagedElementSet<VirtualMachine> vms2 = vms.clone();
        vms2.remove(vms2.get("VM2"));
        s2 = new LazySpread(vms2);
        Assert.assertNotEquals(s, s2);
        Assert.assertNotEquals(s.hashCode(), s2.hashCode());
    }

    /**
     * A test with only future running VMs.
     */
    public void testWithAllRunnings() {
        Configuration src = TestHelper.readConfiguration(RESOURCES_LOCATION + "src.txt");
        Configuration dst = TestHelper.readConfiguration(RESOURCES_LOCATION + "dst.txt");
        ManagedElementSet<VirtualMachine> t1 = new SimpleManagedElementSet<VirtualMachine>();
        t1.add(src.getAllVirtualMachines().get("VM1"));
        t1.add(src.getAllVirtualMachines().get("VM2"));
        ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 1, 2, 3, 4, 5, 6, 7, 8));
        List<VJob> vjobs = new ArrayList<VJob>();
        try {
            VJob v = new DefaultVJob("v1");
            v.addVirtualMachines(t1);
            v.addConstraint(new LazySpread(t1));
            vjobs.add(v);
            TimedReconfigurationPlan p = plan.compute(src,
                    dst.getRunnings(),
                    dst.getWaitings(),
                    dst.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    dst.getOnlines(),
                    dst.getOfflines(),
                    vjobs);
            Assert.assertEquals(p.size(), 2);
            Configuration res = p.getDestination();
            System.err.println(src.getAllVirtualMachines().get("VM1"));
            System.err.println(src.getAllVirtualMachines().get("VM2"));
            Assert.assertNotSame(res.getLocation(src.getAllVirtualMachines().get("VM1")),
                    res.getLocation(src.getAllVirtualMachines().get("VM2")));
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
     * Test with VMs that will be running.
     */
    public void testWithSomeRunnings() {
        Configuration src = TestHelper.readConfiguration(RESOURCES_LOCATION + "src2.txt");
        Configuration dst = TestHelper.readConfiguration(RESOURCES_LOCATION + "dst2.txt");
        ManagedElementSet<VirtualMachine> t1 = new DefaultManagedElementSet<VirtualMachine>();
        t1.add(src.getAllVirtualMachines().get("VM1"));
        t1.add(src.getAllVirtualMachines().get("VM2"));
        ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 3, 2, 3, 4, 5, 6, 7, 8));
        List<VJob> vjobs = new ArrayList<VJob>();
        try {
            VJob v = new DefaultVJob("v1");
            v.addVirtualMachines(t1);
            v.addConstraint(new LazySpread(t1));
            vjobs.add(v);
            TimedReconfigurationPlan p = plan.compute(src,
                    dst.getRunnings(),
                    dst.getWaitings(),
                    dst.getSleepings(),
                    new DefaultManagedElementSet<VirtualMachine>(),
                    dst.getOnlines(),
                    dst.getOfflines(),
                    vjobs);
            Assert.assertEquals(p.size(), 0);
            Configuration res = p.getDestination();
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


        Spread s = new LazySpread(cfg.getAllVirtualMachines());
        VJob v = new DefaultVJob("V1");
        v.addConstraint(s);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            ProtobufVJobSerializer.getInstance().write(v, out.getPath());
            ProtobufVJobBuilder vb = new ProtobufVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null));
            vb.getElementBuilder().useConfiguration(cfg);
            DefaultPBConstraintsCatalog cat = new DefaultPBConstraintsCatalog();
            cat.add(new LazySpreadBuilder());
            vb.setConstraintCatalog(cat);
            VJob v2 = vb.build(out);
            Spread s2 = (Spread) v2.getConstraints().iterator().next();
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

        Spread r = new LazySpread(cfg.getAllVirtualMachines());
        VJob v = new DefaultVJob("V1");
        v.addConstraint(r);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            DefaultXMLConstraintsCatalog cat = new DefaultXMLConstraintsCatalog();
            cat.add(new entropy.vjob.builder.xml.LazySpreadBuilder());
            XmlVJobSerializer.getInstance().write(v, out.getPath());
            XMLVJobBuilder vb = new XMLVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null), cat);
            vb.getElementBuilder().useConfiguration(cfg);
            VJob v2 = vb.build(out);
            Spread s2 = (Spread) v2.getConstraints().iterator().next();
            Assert.assertEquals(r, s2);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }
}
