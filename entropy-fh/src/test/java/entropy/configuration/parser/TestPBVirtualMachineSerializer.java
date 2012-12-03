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

import entropy.configuration.SimpleVirtualMachine;
import entropy.configuration.VirtualMachine;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for PBVirtualMachineSerialize
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestPBVirtualMachineSerializer {

    public void testRead() {
        PBVirtualMachine.VirtualMachine.Builder b = PBVirtualMachine.VirtualMachine.newBuilder();
        b.setName("foo");

        VirtualMachine vm = PBVirtualMachineSerializer.read(b.build());
        Assert.assertEquals(vm.getName(), "foo");
        //Test for the optional values. Boring but safer
        Assert.assertEquals(vm.getCPUMax(), Integer.MAX_VALUE);
        Assert.assertEquals(vm.getCPUConsumption(), 0);
        Assert.assertEquals(vm.getCPUDemand(), 0);
        Assert.assertEquals(vm.getMemoryConsumption(), 0);
        Assert.assertEquals(vm.getMemoryDemand(), 0);
        Assert.assertEquals(vm.getNbOfCPUs(), 1);
        Assert.assertNull(vm.getTemplate());
        Assert.assertTrue(vm.getOptions().isEmpty());
        Assert.assertNull(vm.getHostingPlatform());

        vm = PBVirtualMachineSerializer.read(b.setCpuMax(12).build());
        Assert.assertEquals(vm.getCPUMax(), 12);

        vm = PBVirtualMachineSerializer.read(b.setCpuConsumption(5).build());
        Assert.assertEquals(vm.getCPUConsumption(), 5);

        vm = PBVirtualMachineSerializer.read(b.setCpuDemand(7).build());
        Assert.assertEquals(vm.getCPUDemand(), 7);

        vm = PBVirtualMachineSerializer.read(b.setMemoryConsumption(18).build());
        Assert.assertEquals(vm.getMemoryConsumption(), 18);


        vm = PBVirtualMachineSerializer.read(b.setMemoryDemand(24).build());
        Assert.assertEquals(vm.getMemoryDemand(), 24);

        vm = PBVirtualMachineSerializer.read(b.setNbOfCPUs(3).build());
        Assert.assertEquals(vm.getNbOfCPUs(), 3);

        vm = PBVirtualMachineSerializer.read(b.setTemplate("bar").build());
        Assert.assertEquals(vm.getTemplate(), "bar");

        vm = PBVirtualMachineSerializer.read(b.setHostingPlatform("baz").build());
        Assert.assertEquals(vm.getHostingPlatform(), "baz");

        PBVirtualMachine.VirtualMachine.Option.Builder ob = PBVirtualMachine.VirtualMachine.Option.newBuilder();
        ob.setKey("o1");

        vm = PBVirtualMachineSerializer.read(b.addOptions(ob.build()).build());
        Assert.assertTrue(vm.checkOption("o1"));

        ob.clear();
        ob.setKey("o2").setValue("v2");
        vm = PBVirtualMachineSerializer.read(b.addOptions(ob.build()).build());
        Assert.assertEquals(vm.getOption("o2"), "v2");

        //One last for the road. Just to prevent possible override
        Assert.assertEquals(vm.getCPUMax(), 12);
        Assert.assertEquals(vm.getCPUConsumption(), 5);
        Assert.assertEquals(vm.getCPUDemand(), 7);
        Assert.assertEquals(vm.getMemoryConsumption(), 18);
        Assert.assertEquals(vm.getMemoryDemand(), 24);
        Assert.assertEquals(vm.getNbOfCPUs(), 3);
        Assert.assertEquals(vm.getTemplate(), "bar");
        Assert.assertEquals(vm.getHostingPlatform(), "baz");
        Assert.assertTrue(vm.checkOption("o1"));
        Assert.assertEquals(vm.getOption("o2"), "v2");
    }

    public void testWrite() {
        VirtualMachine vm = new SimpleVirtualMachine("foo");
        PBVirtualMachine.VirtualMachine pbv = PBVirtualMachineSerializer.write(vm);

        Assert.assertEquals(pbv.getName(), "foo");
        Assert.assertEquals(pbv.getNbOfCPUs(), 1);
        Assert.assertEquals(pbv.getCpuConsumption(), 0);
        Assert.assertEquals(pbv.getCpuDemand(), 0);
        Assert.assertEquals(pbv.getMemoryConsumption(), 0);
        Assert.assertEquals(pbv.getMemoryDemand(), 0);
        Assert.assertEquals(pbv.getCpuMax(), Integer.MAX_VALUE);
        Assert.assertFalse(pbv.hasTemplate());
        Assert.assertFalse(pbv.hasHostingPlatform());
        Assert.assertTrue(pbv.getOptionsList().isEmpty());

        vm.setCPUMax(12);
        pbv = PBVirtualMachineSerializer.write(vm);
        Assert.assertEquals(pbv.getCpuMax(), 12);

        vm.setCPUConsumption(5);
        pbv = PBVirtualMachineSerializer.write(vm);
        Assert.assertEquals(pbv.getCpuConsumption(), 5);

        vm.setCPUDemand(11);
        pbv = PBVirtualMachineSerializer.write(vm);
        Assert.assertEquals(pbv.getCpuDemand(), 11);

        vm.setMemoryConsumption(24);
        pbv = PBVirtualMachineSerializer.write(vm);
        Assert.assertEquals(pbv.getMemoryConsumption(), 24);

        vm.setMemoryDemand(36);
        pbv = PBVirtualMachineSerializer.write(vm);
        Assert.assertEquals(pbv.getMemoryDemand(), 36);

        vm.setNbOfCPUs(3);
        pbv = PBVirtualMachineSerializer.write(vm);
        Assert.assertEquals(pbv.getNbOfCPUs(), 3);

        vm.setTemplate("bar");
        pbv = PBVirtualMachineSerializer.write(vm);
        Assert.assertEquals(pbv.getTemplate(), "bar");

        vm.setHostingPlatform("baz");
        pbv = PBVirtualMachineSerializer.write(vm);
        Assert.assertEquals(pbv.getHostingPlatform(), "baz");

        vm.addOption("o1");
        pbv = PBVirtualMachineSerializer.write(vm);
        Assert.assertEquals(pbv.getOptionsList().size(), 1);
        Assert.assertEquals(pbv.getOptionsList().get(0).getKey(), "o1");
        Assert.assertFalse(pbv.getOptionsList().get(0).hasValue());

        vm.addOption("o2", "v2");
        pbv = PBVirtualMachineSerializer.write(vm);
        Assert.assertEquals(pbv.getOptionsList().size(), 2);
        Assert.assertEquals(pbv.getOptionsList().get(0).getKey(), "o2");
        Assert.assertEquals(pbv.getOptionsList().get(0).getValue(), "v2");
        Assert.assertEquals(pbv.getOptionsList().get(1).getKey(), "o1");
        Assert.assertFalse(pbv.getOptionsList().get(1).hasValue());


        Assert.assertEquals(pbv.getCpuMax(), 12);
        Assert.assertEquals(pbv.getCpuConsumption(), 5);
        Assert.assertEquals(pbv.getCpuDemand(), 11);
        Assert.assertEquals(pbv.getMemoryConsumption(), 24);
        Assert.assertEquals(pbv.getMemoryDemand(), 36);
        Assert.assertEquals(pbv.getNbOfCPUs(), 3);
        Assert.assertEquals(pbv.getTemplate(), "bar");
        Assert.assertEquals(pbv.getHostingPlatform(), "baz");
        Assert.assertEquals(pbv.getOptionsList().size(), 2);
        Assert.assertEquals(pbv.getOptionsList().get(1).getKey(), "o1");
        Assert.assertFalse(pbv.getOptionsList().get(1).hasValue());
        Assert.assertEquals(pbv.getOptionsList().get(0).getKey(), "o2");
        Assert.assertEquals(pbv.getOptionsList().get(0).getValue(), "v2");
    }

    public void testRoundTripConversion() {
        VirtualMachine vm = new SimpleVirtualMachine("foo");
        vm.setCPUMax(12);
        vm.setCPUConsumption(5);
        vm.setCPUDemand(11);
        vm.setMemoryConsumption(24);
        vm.setMemoryDemand(36);
        vm.setNbOfCPUs(3);
        vm.setTemplate("bar");
        vm.setHostingPlatform("baz");
        vm.addOption("o1");
        vm.addOption("o2", "v2");
        VirtualMachine vm2 = PBVirtualMachineSerializer.read(PBVirtualMachineSerializer.write(vm));
        Assert.assertEquals(vm, vm2);
        Assert.assertEquals(vm.getCPUConsumption(), vm2.getCPUConsumption());
        Assert.assertEquals(vm.getCPUDemand(), vm2.getCPUDemand());
        Assert.assertEquals(vm.getCPUMax(), vm2.getCPUMax());
        Assert.assertEquals(vm.getMemoryConsumption(), vm2.getMemoryConsumption());
        Assert.assertEquals(vm.getMemoryDemand(), vm2.getMemoryDemand());
        Assert.assertEquals(vm.getNbOfCPUs(), vm2.getNbOfCPUs());
        Assert.assertEquals(vm.getTemplate(), vm2.getTemplate());
        Assert.assertEquals(vm.getHostingPlatform(), vm2.getHostingPlatform());
        Assert.assertEquals(vm.getOptions(), vm2.getOptions());
        Assert.assertEquals(vm.getOption("o2"), vm2.getOption("o2"));

    }
}
