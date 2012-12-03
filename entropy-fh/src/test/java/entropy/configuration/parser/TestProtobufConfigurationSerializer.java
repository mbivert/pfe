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

package entropy.configuration.parser;

import entropy.configuration.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Unit tests for ProtobufConfigurationSerialized.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestProtobufConfigurationSerializer {

    /**
     * Test serialization/unserialization
     */
    public void test() {
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i <= 10; i++) {
            Node n = new SimpleNode("N" + i, i, i + 1, i + 2);
            if (i % 3 == 0) {
                cfg.addOffline(n);
            } else {
                cfg.addOnline(n);
            }
        }
        Random rnd = new Random();
        for (int i = 1; i <= 20; i++) {
            VirtualMachine vm = new SimpleVirtualMachine("VM" + i, i, i + 1, i + 2);
            vm.setCPUDemand(i + 3);
            vm.setCPUMax(i + 3);
            Node n = cfg.getOnlines().get(rnd.nextInt(cfg.getOnlines().size()));
            if (i % 3 == 0) {
                cfg.setSleepOn(vm, n);
            } else if (i % 5 == 0) {
                cfg.addWaiting(vm);
            } else {
                cfg.setRunOn(vm, n);
            }
        }
        FileConfigurationSerializer s = ProtobufConfigurationSerializer.getInstance();
        File tmpF = null;
        try {
            tmpF = File.createTempFile("out", "out");
            s.write(cfg, tmpF.getAbsolutePath());
            Configuration r = s.read(tmpF.getAbsolutePath());
            //System.out.println(cfg.getAllVirtualMachines().get("VM1"));
            //System.out.println(r.getAllVirtualMachines().get("VM1"));
            Assert.assertEquals(r, cfg);

            //Detailed comparison as VM and node comparison is just about the name
            for (VirtualMachine vm : cfg.getAllVirtualMachines()) {
                VirtualMachine vm2 = r.getAllVirtualMachines().get(vm.getName());
                Assert.assertEquals(vm.getCPUDemand(), vm2.getCPUDemand());
                Assert.assertEquals(vm.getCPUConsumption(), vm2.getCPUConsumption());
                Assert.assertEquals(vm.getCPUMax(), vm2.getCPUMax());
                Assert.assertEquals(vm.getMemoryConsumption(), vm2.getMemoryConsumption());
                Assert.assertEquals(vm.getMemoryDemand(), vm2.getMemoryDemand());
                Assert.assertEquals(vm.getTemplate(), vm2.getTemplate());
                Assert.assertEquals(vm.getOptions(), vm2.getOptions());
            }

            for (Node n : cfg.getAllNodes()) {
                Node n2 = r.getAllNodes().get(n.getName());
                Assert.assertEquals(n.getCPUCapacity(), n2.getCPUCapacity());
                Assert.assertEquals(n.getMemoryCapacity(), n2.getMemoryCapacity());
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage(), e);
        } catch (ConfigurationSerializerException e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            if (tmpF != null && tmpF.exists()) {
                tmpF.delete();
            }
        }
    }

    @Test(expectedExceptions = {IOException.class})
    public void testUnknownFile() throws IOException {

        File tmpF = null;
        try {
            tmpF = File.createTempFile("out", "out");
        } catch (IOException e) {
            Assert.fail(e.getMessage(), e);
        }
        String name = tmpF.getAbsolutePath();
        Assert.assertTrue(tmpF.delete());
        try {
            Configuration r = ProtobufConfigurationSerializer.getInstance().read(name);
        } catch (ConfigurationSerializerException e) {
            Assert.fail(e.getMessage(), e);
        }

    }

    @Test(expectedExceptions = {ConfigurationSerializerException.class})
    public void testBadContent() throws ConfigurationSerializerException {

        File tmpF = null;
        PrintWriter out = null;
        try {
            tmpF = File.createTempFile("out", "out");
            out = new PrintWriter(new FileWriter(tmpF));
            out.println("toto");

        } catch (IOException e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            if (out != null) {
                out.close();
            }
        }

        String name = tmpF.getAbsolutePath();
        try {
            Configuration r = ProtobufConfigurationSerializer.getInstance().read(name);
        } catch (IOException e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            tmpF.delete();
        }

    }
}
