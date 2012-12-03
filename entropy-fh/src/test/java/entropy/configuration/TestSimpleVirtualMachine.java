/*
 * Copyright (c) Fabien Hermenier
 *
 * This file is part of Entropy.
 *
 * Entropy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Entropy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package entropy.configuration;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for SimpleVirtualMachine.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestSimpleVirtualMachine {

    public void testLazyConstructors() {
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1");
        Assert.assertEquals(vm1.getName(), "VM1");
        Assert.assertEquals(vm1.getNbOfCPUs(), 1);
        Assert.assertEquals(vm1.getCPUConsumption(), 0);
        Assert.assertEquals(vm1.getCPUDemand(), 0);
        Assert.assertEquals(vm1.getMemoryConsumption(), 0);
        Assert.assertEquals(vm1.getMemoryDemand(), 0);
        Assert.assertEquals(vm1.getCPUMax(), Integer.MAX_VALUE);
        Assert.assertEquals(vm1.getHostingPlatform(), null);
        Assert.assertEquals(vm1.getTemplate(), null);

        vm1 = new SimpleVirtualMachine("VM1", 1, 2, 3);
        Assert.assertEquals(vm1.getName(), "VM1");
        Assert.assertEquals(vm1.getNbOfCPUs(), 1);
        Assert.assertEquals(vm1.getCPUConsumption(), 2);
        Assert.assertEquals(vm1.getMemoryConsumption(), 3);
        Assert.assertEquals(vm1.getMemoryDemand(), 3);
        Assert.assertEquals(vm1.getCPUDemand(), 2);
    }

    /**
     * Test get/set memoryTotal.
     */
    public void testMemoryTotal() {
        VirtualMachine n = new SimpleVirtualMachine("V1", 1, 3, 1024);
        // Test default value
        Assert.assertEquals(n.getMemoryConsumption(), 1024);

        // Test the binding
        n.setMemoryConsumption(2048);
        Assert.assertEquals(n.getMemoryConsumption(), 2048);
    }

    /**
     * Unit tests for get/set nbOfCPUs.
     */
    public void testNbOfCPUs() {
        VirtualMachine n = new SimpleVirtualMachine("N1", 1, 3, 1024);
        Assert.assertEquals(n.getNbOfCPUs(), 1);

        // Test the binding
        n.setNbOfCPUs(2);
        Assert.assertEquals(n.getNbOfCPUs(), 2);
    }

    /**
     * Unit tests for get/set cpuConsumption.
     */
    public void testCPUConsumption() {
        final VirtualMachine n = new SimpleVirtualMachine("N1", 1, 3, 1024);
        // Test default value
        Assert.assertEquals(n.getCPUConsumption(), 3);

        // Test the binding
        n.setCPUConsumption(2);
        Assert.assertEquals(n.getCPUConsumption(), 2);
    }

    public void testCPUMax() {
        final VirtualMachine n = new SimpleVirtualMachine("N1", 1, 3, 1024);
        // Test default value
        Assert.assertEquals(n.getCPUMax(), Integer.MAX_VALUE);

        // Test the binding
        n.setCPUMax(3);
        Assert.assertEquals(n.getCPUMax(), 3);
    }

    public void testPlatform() {
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1");
        vm1.setHostingPlatform("foo");
        Assert.assertEquals(vm1.getHostingPlatform(), "foo");
    }

    public void testTemplate() {
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1");
        vm1.setTemplate("foo");
        Assert.assertEquals(vm1.getTemplate(), "foo");
    }

    /**
     * Test the setters for resources needs.
     */
    public void testWithNeeds() {
        VirtualMachine n = new SimpleVirtualMachine("N1", 1, 3, 1024, 5, 2048);
        Assert.assertEquals(n.getCPUDemand(), 5);
        Assert.assertEquals(n.getMemoryDemand(), 2048);
        n.setMemoryDemand(1024);
        n.setCPUDemand(12);
        Assert.assertEquals(n.getCPUDemand(), 12);
        Assert.assertEquals(n.getMemoryDemand(), 1024);
    }

    /**
     * Test the getters for resources needs when not known.
     */
    public void testWithNoNeeds() {
        VirtualMachine n = new SimpleVirtualMachine("N1", 1, 3, 1024);
        Assert.assertEquals(n.getCPUDemand(), 3);
        Assert.assertEquals(n.getMemoryDemand(), 1024);
    }

    /**
     * Test the copy constructor and check if there is no side-effects.
     */
    public void testCopyConstructor() {
        final VirtualMachine n = new SimpleVirtualMachine("N1", 1, 3, 1024);
        n.setCPUMax(15);
        n.setCPUDemand(32);
        n.setHostingPlatform("foo");
        n.setTemplate("bar");
        n.addOption("baz");
        n.addOption("o2", "v2");
        VirtualMachine n2 = n.clone();
        Assert.assertEquals(n2.getHostingPlatform(), "foo");
        Assert.assertEquals(n2.getTemplate(), "bar");
        Assert.assertEquals(n2.getMemoryConsumption(), 1024);
        Assert.assertEquals(n2.getMemoryDemand(), 1024);
        Assert.assertEquals(n2.getNbOfCPUs(), 1);
        Assert.assertEquals(n2.getCPUConsumption(), 3);
        Assert.assertEquals(n2.getCPUDemand(), 32);
        Assert.assertEquals(n2.getCPUMax(), 15);
        Assert.assertEquals(n2.getOptions().size(), 2);
        Assert.assertTrue(n2.checkOption("baz"));
        Assert.assertEquals(n2.getOption("o2"), "v2");

        n.setNbOfCPUs(2);
        n.setMemoryConsumption(2048);
        n.setCPUConsumption(5);
        n.setCPUDemand(18);
        n.addOption("o2", "v3");
        Assert.assertEquals(n2.getOption("o2"), "v2");
        Assert.assertEquals(n2.getMemoryConsumption(), 1024);
        Assert.assertEquals(n2.getNbOfCPUs(), 1);
        Assert.assertEquals(n2.getCPUConsumption(), 3);
        Assert.assertEquals(n2.getCPUMax(), 15);
    }

    /**
     * Test equals() in various conditions.
     */
    public void testEquals() {
        VirtualMachine n = new SimpleVirtualMachine("N1", 1, 3, 1024);
        VirtualMachine n2 = new SimpleVirtualMachine("N1", 2, 3, 4096);
        Assert.assertTrue(n.equals(n2));
        Assert.assertEquals(n, n);
        Assert.assertFalse(n.equals(null));
        Assert.assertEquals(n.hashCode(), n2.hashCode());
        VirtualMachine n3 = new SimpleVirtualMachine("N2", 2, 3, 4096);
        Assert.assertFalse(n.equals(n3));
        Assert.assertFalse(n.equals(new Object()));
    }

    /**
     * Dummy test to prevent NullPointerException
     */
    public void testToString() {
        VirtualMachine vm = new SimpleVirtualMachine("VM1", 1, 2, 3, 4, 5);
        Assert.assertNotNull(vm.toString());
    }

}
