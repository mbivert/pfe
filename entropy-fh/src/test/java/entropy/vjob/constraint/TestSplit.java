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
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestSplit {

    /**
     * Test isSatisfied() in various situations.
     */
    public void testIsSatisfied() {
        Configuration cfg = new DefaultConfiguration();
        Node n1 = new SimpleNode("N1", 1, 1, 1);
        Node n2 = new SimpleNode("N2", 1, 1, 1);
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 1);
        VirtualMachine vm3 = new SimpleVirtualMachine("VM3", 1, 1, 1);
        VirtualMachine vm4 = new SimpleVirtualMachine("VM4", 1, 1, 1);


        ManagedElementSet<VirtualMachine> s1 = new SimpleManagedElementSet<VirtualMachine>();
        ManagedElementSet<VirtualMachine> s2 = new SimpleManagedElementSet<VirtualMachine>();

        s1.add(vm1);
        s2.add(vm3);
        s2.add(vm4);

        cfg.addOnline(n1);
        cfg.addOnline(n2);
        cfg.setRunOn(vm1, n1);
        cfg.setRunOn(vm2, n1);
        cfg.setRunOn(vm4, n2);
        cfg.setSleepOn(vm3, n2);

        Assert.assertFalse(new MockSplit(new SimpleManagedElementSet<VirtualMachine>(), new SimpleManagedElementSet<VirtualMachine>()).isSatisfied(cfg));
        Assert.assertTrue(new MockSplit(s1, s2).isSatisfied(cfg));
        s2.add(vm2);
        Assert.assertFalse(new MockSplit(s1, s2).isSatisfied(cfg));
    }
}
