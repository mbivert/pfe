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
 * Unit tests for Spread.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestSpread {

    public void testMisPlaced() {
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
        vms.add(vm3);
        vms.add(vm4);
        Assert.assertTrue(new MockSpread(new SimpleManagedElementSet<VirtualMachine>()).getMisPlaced(cfg).isEmpty());
        ManagedElementSet<VirtualMachine> s = new MockSpread(cfg.getAllVirtualMachines()).getMisPlaced(cfg);
        Assert.assertEquals(s.size(), 2);
        Assert.assertTrue(s.contains(vm1) && s.contains(vm2));
        Assert.assertTrue(new MockSpread(vms).getMisPlaced(cfg).isEmpty());
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
        vms.add(vm3);
        vms.add(vm4);
        Assert.assertTrue(new MockSpread(new SimpleManagedElementSet<VirtualMachine>()).isSatisfied(cfg));
        Assert.assertFalse(new MockSpread(cfg.getAllVirtualMachines()).isSatisfied(cfg));
        Assert.assertTrue(new MockSpread(vms).isSatisfied(cfg));
    }
}
