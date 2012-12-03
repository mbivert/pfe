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

import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;
import entropy.TestHelper;
import entropy.configuration.*;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.action.Action;
import entropy.plan.choco.ChocoCustomRP;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.template.MockVirtualMachineTemplateFactory;
import entropy.vjob.*;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.protobuf.ContinuousSpreadBuilder;
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
 * Unit tests for ContinuousSpread.
 *
 * @author Fabien Hermenier
 */
@Test(groups = "unit")
public class TestContinuousSpread {

    /**
     * Location of resources used for tests.
     */
    public static final String RESOURCES_LOCATION = "src/test/resources/entropy/vjob/constraint/TestContinuousSpread.";

    /**
     * Instantiation, equals, hashcode, ...
     */
    public void testBasics() {
        ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
        vms.add(new SimpleVirtualMachine("VM1", 1, 2, 3));
        vms.add(new SimpleVirtualMachine("VM2", 1, 2, 3));
        Spread s = new ContinuousSpread(vms);
        Assert.assertFalse(s.toString().contains("null"));
        Assert.assertEquals(s.getNodes().size(), 0);
        Assert.assertEquals(s.getAllVirtualMachines(), vms);

        Spread s2 = new ContinuousSpread(vms);
        Assert.assertEquals(s, s2);
        Assert.assertEquals(s.hashCode(), s2.hashCode());
        ManagedElementSet<VirtualMachine> vms2 = vms.clone();
        vms2.remove(vms2.get("VM2"));
        s2 = new ContinuousSpread(vms2);
        Assert.assertNotEquals(s, s2);
        Assert.assertNotEquals(s.hashCode(), s2.hashCode());
    }

    /**
     * A basic test on spread.
     * VM1 have to be hosted on N2 for resources issues. With the constraint, VM2 will have to be migrated first.
     */
    public void basicTest() {
        //ChocoLogging.setVerbosity(Verbosity.FINEST);
        Configuration src = TestHelper.readConfiguration(RESOURCES_LOCATION + "src.txt");
        Configuration dst = TestHelper.readConfiguration(RESOURCES_LOCATION + "dst.txt");
        ManagedElementSet<VirtualMachine> t1 = new SimpleManagedElementSet<VirtualMachine>();
        t1.add(src.getAllVirtualMachines().get("VM1"));
        t1.add(src.getAllVirtualMachines().get("VM2"));
        ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 3, 2, 3, 4, 5, 6, 7, 8));
        plan.setRepairMode(false);
        List<VJob> vjobs = new ArrayList<VJob>();
        try {
            VJob v = new DefaultVJob("v1");
            v.addVirtualMachines(t1);
            v.addConstraint(new ContinuousSpread(t1));
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
            ReconfigurationProblem rp = plan.getModel();
            Action m1 = plan.getModel().getAssociatedAction(src.getAllVirtualMachines().get("VM1")).getDefinedAction(rp).get(0);
            Action m2 = plan.getModel().getAssociatedAction(src.getAllVirtualMachines().get("VM2")).getDefinedAction(rp).get(0);
            Assert.assertTrue(m1.getStartMoment() >= m2.getFinishMoment());

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

    public void testWithNonRunningVMs() {
        Configuration src = TestHelper.readConfiguration(RESOURCES_LOCATION + "src2.txt");
        Configuration dst = TestHelper.readConfiguration(RESOURCES_LOCATION + "dst2.txt");
        ManagedElementSet<VirtualMachine> t1 = new SimpleManagedElementSet<VirtualMachine>();
        t1.addAll(src.getAllVirtualMachines());
        ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 1, 2, 3, 4, 5, 6, 7, 8));
        plan.setRepairMode(false);
        List<VJob> vjobs = new ArrayList<VJob>();
        try {
            VJob v = new DefaultVJob("v1");
            v.addVirtualMachines(t1);
            PlacementConstraint c1 = new ContinuousSpread(t1);
            v.addConstraint(c1);
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


        Spread s = new ContinuousSpread(cfg.getAllVirtualMachines());
        VJob v = new DefaultVJob("V1");
        v.addConstraint(s);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            ProtobufVJobSerializer.getInstance().write(v, out.getPath());
            ProtobufVJobBuilder vb = new ProtobufVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null));
            vb.getElementBuilder().useConfiguration(cfg);
            DefaultPBConstraintsCatalog cat = new DefaultPBConstraintsCatalog();
            cat.add(new ContinuousSpreadBuilder());
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

        Spread r = new ContinuousSpread(cfg.getAllVirtualMachines());
        VJob v = new DefaultVJob("V1");
        v.addConstraint(r);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            DefaultXMLConstraintsCatalog cat = new DefaultXMLConstraintsCatalog();
            cat.add(new entropy.vjob.builder.xml.ContinuousSpreadBuilder());
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

    public void testSplitting() {
        ChocoLogging.setVerbosity(Verbosity.SEARCH);
        Configuration cfg = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 2, 2);
        Node n2 = new SimpleNode("N2", 1, 2, 2);
        Node n3 = new SimpleNode("N3", 1, 2, 2);
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 1);

        cfg.addOnline(n1);
        cfg.addOnline(n2);
        //cfg.addOnline(n3);
        cfg.setRunOn(vm1, n1);
        cfg.setRunOn(vm2, n1);

        Spread r = new ContinuousSpread(cfg.getAllVirtualMachines());
        VJob v = new DefaultVJob("V1");
        v.addConstraint(r);
        List<VJob> vjobs = new ArrayList<VJob>();
        vjobs.add(v);
        try {
            ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 5, 2, 3, 4, 5, 6, 7, 8));
            plan.setRepairMode(false);
            plan.doOptimize(true);
            TimedReconfigurationPlan p = plan.compute(cfg,
                    cfg.getRunnings(),
                    cfg.getWaitings(),
                    cfg.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    cfg.getOnlines(),
                    cfg.getOfflines(),
                    vjobs);
            Assert.assertEquals(p.size(), 1);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }
}
