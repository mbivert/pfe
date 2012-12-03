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
import entropy.plan.choco.constraint.pack.SatisfyDemandingSlicesHeightsFastBP;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.template.MockVirtualMachineTemplateFactory;
import entropy.vjob.DefaultVJob;
import entropy.vjob.Offline;
import entropy.vjob.VJob;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.protobuf.DefaultPBConstraintsCatalog;
import entropy.vjob.builder.protobuf.ProtobufVJobBuilder;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.DefaultXMLConstraintsCatalog;
import entropy.vjob.builder.xml.OfflineBuilder;
import entropy.vjob.builder.xml.XMLVJobBuilder;
import entropy.vjob.builder.xml.XmlVJobSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the {@code offline} constraint
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestOffline {

    /**
     * Test instantiation, getters, hashCode and equals
     */
    public void testBasics() {
        ManagedElementSet<Node> ns = new SimpleManagedElementSet<Node>();
        ns.add(new SimpleNode("N1", 1, 2, 3));
        ns.add(new SimpleNode("N2", 1, 2, 3));
        Offline c = new Offline(ns);
        Assert.assertFalse(c.toString().contains("null"));
        Assert.assertEquals(c.getNodes(), ns);
        Assert.assertEquals(c.getAllVirtualMachines().size(), 0);

        Offline c2 = new Offline(ns);
        Assert.assertEquals(c, c2);
        Assert.assertEquals(c.hashCode(), c2.hashCode());
        ManagedElementSet<Node> ns2 = ns.clone();
        ns2.remove(ns2.get("N2"));
        c2 = new Offline(ns2);
        Assert.assertNotEquals(c, c2);
        Assert.assertNotEquals(c.hashCode(), c2.hashCode());

        c2 = new Offline(ns2);
        Assert.assertNotEquals(c, c2);
        Assert.assertNotEquals(c.hashCode(), c2.hashCode());

    }

    public void testIsSatisfied() {
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i <= 6; i++) {
            if (i <= 3) {
                Node n = new SimpleNode("N" + i, 10, 10, 10);
                cfg.addOnline(n);
            } else {
                Node n = new SimpleNode("N" + i, 5, 5, 5);
                cfg.addOffline(n);
            }
        }
        Offline c = new Offline(cfg.getAllNodes());
        Assert.assertFalse(c.isSatisfied(cfg));
        c = new Offline(cfg.getOfflines());
        Assert.assertTrue(c.isSatisfied(cfg));
        c = new Offline(cfg.getOnlines());
        Assert.assertFalse(c.isSatisfied(cfg));

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
        Offline c1 = new Offline(ns);
        VJob v = new DefaultVJob("V1");
        v.addConstraint(c1);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            ProtobufVJobSerializer.getInstance().write(v, out.getPath());
            ProtobufVJobBuilder vb = new ProtobufVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null));
            vb.getElementBuilder().useConfiguration(cfg);
            DefaultPBConstraintsCatalog cat = new DefaultPBConstraintsCatalog();
            cat.add(new entropy.vjob.builder.protobuf.OfflineBuilder());
            vb.setConstraintCatalog(cat);
            VJob v2 = vb.build(out);
            Offline c2 = (Offline) v2.getConstraints().iterator().next();
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
        Offline c1 = new Offline(ns);
        VJob v = new DefaultVJob("V1");
        v.addConstraint(c1);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            DefaultXMLConstraintsCatalog cat = new DefaultXMLConstraintsCatalog();
            cat.add(new OfflineBuilder());
            XmlVJobSerializer.getInstance().write(v, out.getPath());
            XMLVJobBuilder vb = new XMLVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null), cat);
            vb.getElementBuilder().useConfiguration(cfg);
            VJob v2 = vb.build(out);
            Offline c2 = (Offline) v2.getConstraints().iterator().next();
            Assert.assertEquals(c1, c2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    public void testOfflineOnOfflineNodes() {
        ManagedElementSet<Node> ns = new SimpleManagedElementSet<Node>();
        Configuration src = new SimpleConfiguration();
        for (int i = 1; i < 5; i++) {
            Node n = new SimpleNode("N" + i, 1, 5 - i, 5 - i);
            ns.add(n);
            src.addOffline(n);
        }
        Offline c1 = new Offline(ns);
        VJob v = new DefaultVJob("V1");
        v.addConstraint(c1);

        List<VJob> vjobs = new ArrayList<VJob>();
        vjobs.add(v);
        try {

            ChocoCustomRP planner = new ChocoCustomRP(new MockDurationEvaluator(2, 5, 1, 1, 7, 14, 7, 2, 4));
            planner.setPackingConstraintClass(new SatisfyDemandingSlicesHeightsFastBP());
            planner.setRepairMode(false);


            TimedReconfigurationPlan p = planner.compute(src,
                    src.getAllVirtualMachines(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<Node>(),
                    src.getOfflines(),
                    vjobs);
            Assert.assertTrue(p.getActions().isEmpty());
            Assert.assertEquals(p.getDuration(), 0);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }

    }
}
