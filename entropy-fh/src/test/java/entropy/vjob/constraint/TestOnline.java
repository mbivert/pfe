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
import entropy.template.MockVirtualMachineTemplateFactory;
import entropy.vjob.DefaultVJob;
import entropy.vjob.Online;
import entropy.vjob.VJob;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.protobuf.DefaultPBConstraintsCatalog;
import entropy.vjob.builder.protobuf.ProtobufVJobBuilder;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.DefaultXMLConstraintsCatalog;
import entropy.vjob.builder.xml.OnlineBuilder;
import entropy.vjob.builder.xml.XMLVJobBuilder;
import entropy.vjob.builder.xml.XmlVJobSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Unit tests for the {@code online} constraint
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestOnline {

    /**
     * Test instantiation, getters, hashCode and equals
     */
    public void testBasics() {
        ManagedElementSet<Node> ns = new SimpleManagedElementSet<Node>();
        ns.add(new SimpleNode("N1", 1, 2, 3));
        ns.add(new SimpleNode("N2", 1, 2, 3));
        Online c = new Online(ns);
        Assert.assertFalse(c.toString().contains("null"));
        Assert.assertEquals(c.getNodes(), ns);
        Assert.assertEquals(c.getAllVirtualMachines().size(), 0);

        Online c2 = new Online(ns);
        Assert.assertEquals(c, c2);
        Assert.assertEquals(c.hashCode(), c2.hashCode());
        ManagedElementSet<Node> ns2 = ns.clone();
        ns2.remove(ns2.get("N2"));
        c2 = new Online(ns2);
        Assert.assertNotEquals(c, c2);
        Assert.assertNotEquals(c.hashCode(), c2.hashCode());

        c2 = new Online(ns2);
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
        Online c = new Online(cfg.getAllNodes());
        Assert.assertFalse(c.isSatisfied(cfg));
        c = new Online(cfg.getOfflines());
        Assert.assertFalse(c.isSatisfied(cfg));
        c = new Online(cfg.getOnlines());
        Assert.assertTrue(c.isSatisfied(cfg));

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
        Online c1 = new Online(ns);
        VJob v = new DefaultVJob("V1");
        v.addConstraint(c1);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            ProtobufVJobSerializer.getInstance().write(v, out.getPath());
            ProtobufVJobBuilder vb = new ProtobufVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null));
            vb.getElementBuilder().useConfiguration(cfg);
            DefaultPBConstraintsCatalog cat = new DefaultPBConstraintsCatalog();
            cat.add(new entropy.vjob.builder.protobuf.OnlineBuilder());
            vb.setConstraintCatalog(cat);
            VJob v2 = vb.build(out);
            Online c2 = (Online) v2.getConstraints().iterator().next();
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
        Online c1 = new Online(ns);
        VJob v = new DefaultVJob("V1");
        v.addConstraint(c1);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            DefaultXMLConstraintsCatalog cat = new DefaultXMLConstraintsCatalog();
            cat.add(new OnlineBuilder());
            XmlVJobSerializer.getInstance().write(v, out.getPath());
            XMLVJobBuilder vb = new XMLVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null), cat);
            vb.getElementBuilder().useConfiguration(cfg);
            VJob v2 = vb.build(out);
            Online c2 = (Online) v2.getConstraints().iterator().next();
            Assert.assertEquals(c1, c2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
