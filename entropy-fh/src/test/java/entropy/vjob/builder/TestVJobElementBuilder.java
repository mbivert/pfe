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

package entropy.vjob.builder;

import entropy.configuration.*;
import entropy.template.MockVirtualMachineTemplateFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link DefaultVJobElementBuilder}.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestVJobElementBuilder {

    /**
     * Test the matching of node in various situations
     */
    public void TestNodeMatching() {

        Configuration cfg = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 1, 1);
        Node n2 = new SimpleNode("N2", 1, 1, 1);
        cfg.addOnline(n1);
        cfg.addOnline(n2);
        //VMBuilder is optional here
        VJobElementBuilder eb = new DefaultVJobElementBuilder(null);

        //No configuration, so null
        Assert.assertNull(eb.matchAsNode("N3"));
        Assert.assertNull(eb.matchAsNode("N1"));

        //A configuration is used
        eb.useConfiguration(cfg);
        Assert.assertNull(eb.matchAsNode("N3"));
        Assert.assertEquals(eb.matchAsNode("N1"), n1);
    }

    /**
     * Test VM matching is several conditions.
     */
    public void testVirtualMachineMatching() {
        Configuration cfg = new SimpleConfiguration();
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 1);
        MockVirtualMachineTemplateFactory m = new MockVirtualMachineTemplateFactory();
        cfg.addWaiting(vm1);
        m.getMockBuilder().farm.add("VM2");
        VJobElementBuilder eb = new DefaultVJobElementBuilder(null);

        //No vmBuilder nor configuration, so null
        Assert.assertNull(eb.matchVirtualMachine("VM1"));
        Assert.assertNull(eb.matchVirtualMachine("VM2"));

        //A configuration, so only vm1
        eb.useConfiguration(cfg);
        Assert.assertEquals(eb.matchVirtualMachine("VM1"), vm1);
        Assert.assertNull(eb.matchVirtualMachine("VM2"));

        //A vmBuilder, so only vm2
        m = new MockVirtualMachineTemplateFactory();
        m.getMockBuilder().farm.add("VM2");
        eb = new DefaultVJobElementBuilder(m);
        Assert.assertEquals(eb.matchVirtualMachine("VM2", null, null), vm2);
    }
}
