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

package entropy.configuration.parser;

import entropy.configuration.Node;
import entropy.configuration.SimpleNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for PBNodeSerializer.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestPBNodeSerializer {

    public void testRead() {
        PBNode.Node.Builder b = PBNode.Node.newBuilder();
        b.setName("foo");

        Node n = PBNodeSerializer.read(b.build());
        Assert.assertEquals(n.getName(), "foo");
        //Boring, but safer
        Assert.assertEquals(n.getCPUCapacity(), 0);
        Assert.assertEquals(n.getMemoryCapacity(), 0);
        Assert.assertEquals(n.getNbOfCPUs(), 1);
        Assert.assertNull(n.getMACAddress());
        Assert.assertNull(n.getIPAddress());
        Assert.assertNull(n.getCurrentPlatform());
        Assert.assertEquals(n.getAvailablePlatforms().size(), 0);

        n = PBNodeSerializer.read(b.setCpuCapacity(12).build());
        Assert.assertEquals(n.getCPUCapacity(), 12);

        n = PBNodeSerializer.read(b.setMemoryCapacity(7).build());
        Assert.assertEquals(n.getMemoryCapacity(), 7);

        n = PBNodeSerializer.read(b.setNbOfCPUs(3).build());
        Assert.assertEquals(n.getNbOfCPUs(), 3);

        n = PBNodeSerializer.read(b.setMac("00:AA:45:FF:23:1F").build());
        Assert.assertEquals(n.getMACAddress(), "00:AA:45:FF:23:1F");

        n = PBNodeSerializer.read(b.setIp("192.168.0.17").build());
        Assert.assertEquals(n.getIPAddress(), "192.168.0.17");


        Assert.assertEquals(n.getCPUCapacity(), 12);
        Assert.assertEquals(n.getMemoryCapacity(), 7);
        Assert.assertEquals(n.getNbOfCPUs(), 3);
        Assert.assertEquals(n.getMACAddress(), "00:AA:45:FF:23:1F");
        Assert.assertEquals(n.getIPAddress(), "192.168.0.17");

        PBNode.Node.Platform.Builder pb = PBNode.Node.Platform.newBuilder();
        pb.setName("pl1");
        PBNode.Node.Platform.Option.Builder ob = PBNode.Node.Platform.Option.newBuilder();
        ob.setKey("o1");
        ob.setValue("v1");
        pb.addOptions(ob.build());
        ob.clear();
        ob.setKey("o2");
        pb.addOptions(ob.build());
        b.addPlatforms(pb.build());

        //TODO: Check the platform
        n = PBNodeSerializer.read(b.build());
        Assert.assertTrue(n.getAvailablePlatforms().size() == 1 && n.getCurrentPlatform().equals("pl1"));
        Map<String, String> opts = n.getPlatformOptions("pl1");
        Assert.assertEquals(opts.get("o1"), "v1");
        Assert.assertTrue(opts.containsKey("o2"));

    }

    public void testWrite() {
        Node n = new SimpleNode("foo");
        PBNode.Node pbn = PBNodeSerializer.write(n);
        Assert.assertEquals(pbn.getName(), "foo");
        Assert.assertEquals(pbn.getNbOfCPUs(), 1);
        Assert.assertEquals(pbn.getCpuCapacity(), 0);
        Assert.assertEquals(pbn.getMemoryCapacity(), 0);
        Assert.assertFalse(pbn.hasIp());
        Assert.assertFalse(pbn.hasMac());

        Assert.assertFalse(pbn.hasCurrentPlatform());
        Assert.assertEquals(pbn.getPlatformsList().size(), 0);

        n.setCPUCapacity(12);
        Assert.assertEquals(PBNodeSerializer.write(n).getCpuCapacity(), 12);

        n.setMemoryCapacity(18);
        Assert.assertEquals(PBNodeSerializer.write(n).getMemoryCapacity(), 18);

        n.setNbOfCPUs(3);
        Assert.assertEquals(PBNodeSerializer.write(n).getNbOfCPUs(), 3);

        n.setIPAddress("127.0.0.1");
        Assert.assertEquals(PBNodeSerializer.write(n).getIp(), "127.0.0.1");

        HashMap<String, String> opts1 = new HashMap<String, String>();
        opts1.put("o1", "v1");
        opts1.put("o2", null);
        n.addPlatform("any", opts1);

        HashMap<String, String> opts2 = new HashMap<String, String>();
        n.addPlatform("bar", opts2);

        pbn = PBNodeSerializer.write(n);
        Assert.assertEquals(pbn.getPlatformsList().size(), 2);
        PBNode.Node.Platform p1 = pbn.getPlatforms(0);
        Assert.assertEquals(p1.getName(), "any");
        Assert.assertEquals(p1.getOptionsList().size(), 2);
        Assert.assertEquals(p1.getOptions(0).getKey(), "o2");
        Assert.assertFalse(p1.getOptions(0).hasValue());
        Assert.assertEquals(p1.getOptions(1).getKey(), "o1");
        Assert.assertEquals(p1.getOptions(1).getValue(), "v1");

    }

    public void testRoundTripConversion() {
        Node n = new SimpleNode("foo");
        n.setCPUCapacity(12);
        n.setMemoryCapacity(18);
        n.setNbOfCPUs(3);
        n.setIPAddress("127.0.0.1");
        HashMap<String, String> opts1 = new HashMap<String, String>();
        opts1.put("o1", "v1");
        opts1.put("o2", null);
        n.addPlatform("any", opts1);

        HashMap<String, String> opts2 = new HashMap<String, String>();
        opts2.put("o3", "v3");
        opts2.put("o4", null);
        n.addPlatform("bar", opts2);


        Node n2 = PBNodeSerializer.read(PBNodeSerializer.write(n));
        Assert.assertEquals(n, n2);
        Assert.assertEquals(n.getName(), n2.getName());
        Assert.assertEquals(n.getCPUCapacity(), n2.getCPUCapacity());
        Assert.assertEquals(n.getMemoryCapacity(), n2.getMemoryCapacity());
        Assert.assertEquals(n.getIPAddress(), n2.getIPAddress());
        Assert.assertEquals(n.getNbOfCPUs(), n2.getNbOfCPUs());
        Assert.assertTrue(n.getAvailablePlatforms().size() == 2 && n.getAvailablePlatforms().contains("any") && n.getAvailablePlatforms().contains("bar"));
        Assert.assertEquals(n.getPlatformOptions("any"), n2.getPlatformOptions("any"));
        Assert.assertEquals(n.getPlatformOptions("bar"), n2.getPlatformOptions("bar"));
    }
}
