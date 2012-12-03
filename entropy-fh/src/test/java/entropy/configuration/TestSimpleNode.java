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

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for SimpleNode.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestSimpleNode {


    public void testLazyConstructor() {
        Node n = new SimpleNode("N1", 1, 100, 1024);
        Assert.assertNull(n.getIPAddress());
        Assert.assertNull(n.getMACAddress());
        Assert.assertNull(n.getCurrentPlatform());
        Assert.assertEquals(n.getAvailablePlatforms().size(), 0);
        n = new SimpleNode("N1", 1, 100, 1024, "192.168.0.1", "FF:FF:FF:FF:FF:FF");
        Assert.assertEquals(n.getIPAddress(), "192.168.0.1");
        Assert.assertEquals(n.getMACAddress(), "FF:FF:FF:FF:FF:FF");
    }

    public void testIPAddress() {
        Node n = new SimpleNode("N1", 1, 100, 1024);
        n.setIPAddress("192.168.0.1");
        Assert.assertEquals(n.getIPAddress(), "192.168.0.1");
    }

    /**
     * Tests related to MEMORY_TOTAL.
     * Tests on getter and setter
     */
    public void testMemoryTotal() {
        Node n = new SimpleNode("N1", 1, 100, 1024);
        // Test default value
        Assert.assertEquals(n.getMemoryCapacity(), 1024);

        // Test the binding
        n.setMemoryCapacity(2048);
        Assert.assertEquals(n.getMemoryCapacity(), 2048);
    }

    /**
     * Tests related to NbOfCPUs.
     * Tests on getter and setter
     */
    public void testNbOfCPUs() {
        Node n = new SimpleNode("N1", 1, 100, 1024);
        // Test default value
        Assert.assertEquals(n.getNbOfCPUs(), 1);

        // Test the binding
        n.setNbOfCPUs(2);
        Assert.assertEquals(n.getNbOfCPUs(), 2);
    }

    /**
     * Tests related to CPU_CAPACITY.
     * Tests on getter and setter
     */
    public void testCPUCapacity() {
        Node n = new SimpleNode("N1", 1, 100, 1024);
        // Test default value
        Assert.assertEquals(n.getCPUCapacity(), 100);

        // Test the binding
        n.setCPUCapacity(200);
        Assert.assertEquals(n.getCPUCapacity(), 200);
    }


    /**
     * Tests related to the copy constructor.
     */
    public void testCopyConstructor() {
        Node n = new SimpleNode("N1", 1, 100, 1024);
        n.setIPAddress("192.168.0.1");
        Node n2 = n.clone();
        Assert.assertEquals(n2.getMemoryCapacity(), 1024);
        Assert.assertEquals(n2.getNbOfCPUs(), 1);
        Assert.assertEquals(n2.getIPAddress(), "192.168.0.1");
        n.setNbOfCPUs(2);
        n.setMemoryCapacity(2048);


        Assert.assertEquals(n2.getMemoryCapacity(), 1024);
        Assert.assertEquals(n2.getNbOfCPUs(), 1);
    }

    /**
     * Tests related to equals().
     */
    public void testEquals() {
        Node n = new SimpleNode("N1", 1, 100, 1024);
        Node n2 = new SimpleNode("N1", 2, 100, 4096);
        Assert.assertTrue(n.equals(n2));

        Node n3 = new SimpleNode("N2", 2, 100, 4096);
        Assert.assertFalse(n.equals(n3));
        Assert.assertFalse(n.equals(new Object()));

        Assert.assertEquals(n, n);
        Assert.assertFalse(n.equals(null));
        Assert.assertEquals(n.hashCode(), n2.hashCode());
    }

    public void testPlatform() {
        Node n = new SimpleNode("N1", 1, 100, 1024);
        Assert.assertTrue(n.addPlatform("any"));
        Assert.assertEquals(n.getCurrentPlatform(), "any");
        Assert.assertEquals(n.getPlatformOptions("any").size(), 0);
        Assert.assertEquals(n.getAvailablePlatforms().size(), 1);
        Assert.assertFalse(n.addPlatform("any"));
        Map<String, String> opts = n.getPlatformOptions("any");
        opts.put("o1", "v1");
        opts.put("o2", "v2");
        Assert.assertEquals(n.getPlatformOptions("any"), opts);


        Assert.assertFalse(n.setCurrentPlatform("foo"));
        Assert.assertEquals(n.getCurrentPlatform(), "any");
        Assert.assertEquals(n.getAvailablePlatforms().size(), 1);


        opts = new HashMap<String, String>();
        opts.put("o1", "v1");
        opts.put("o2", "v2");
        n.addPlatform("baz", opts);
        Assert.assertEquals(n.getPlatformOptions("baz"), opts);

        Node n2 = n.clone();
        Assert.assertEquals(n2.getCurrentPlatform(), n.getCurrentPlatform());
        Assert.assertEquals(n2.getAvailablePlatforms(), n.getAvailablePlatforms());
    }
}
