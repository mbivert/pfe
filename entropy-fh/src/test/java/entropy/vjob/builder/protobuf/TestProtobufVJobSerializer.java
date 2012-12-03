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

package entropy.vjob.builder.protobuf;

import entropy.configuration.*;
import entropy.vjob.*;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.VJobElementBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestProtobufVJobSerializer {

    public void test1() {

        Configuration cfg = new SimpleConfiguration();

        VJob v = new DefaultVJob("test");
        VirtualMachine vm1 = new SimpleVirtualMachine("vm1", 5, 50, 500);
        vm1.setCPUDemand(55);
        vm1.setCPUDemand(550);
        vm1.setTemplate("tinyVMs");

        VirtualMachine vm2 = new SimpleVirtualMachine("vm2", 6, 60, 600);
        vm2.setCPUDemand(65);
        vm2.setCPUDemand(650);
        vm1.setTemplate("smallVMs");

        VirtualMachine vm3 = new SimpleVirtualMachine("vm3", 7, 70, 700);
        vm3.setCPUDemand(75);
        vm3.setCPUDemand(750);
        vm3.addOption("preemptible");
        vm3.addOption("start", "7");
        vm3.addOption("stop", "12");

        VirtualMachine vm4 = new SimpleVirtualMachine("vm4", 8, 80, 800);
        vm4.setCPUDemand(85);
        vm4.setCPUDemand(850);

        v.addVirtualMachine(vm1);
        v.addVirtualMachine(vm2);
        v.addVirtualMachine(vm3);
        v.addVirtualMachine(vm4);

        Node n1 = new SimpleNode("N1", 1, 10, 100);
        Node n2 = new SimpleNode("N2", 2, 20, 200);
        Node n3 = new SimpleNode("N3", 3, 30, 300);
        Node n4 = new SimpleNode("N4", 4, 40, 400);

        cfg.addOnline(n1);
        cfg.addOnline(n2);
        cfg.addOnline(n3);
        cfg.addOnline(n4);

        cfg.addWaiting(vm1);
        cfg.addWaiting(vm2);
        cfg.setRunOn(vm3, n1);
        cfg.setSleepOn(vm4, n2);

        ManagedElementSet<Node> s1 = new SimpleManagedElementSet<Node>();
        s1.add(n1);
        s1.add(n2);

        ManagedElementSet<Node> s2 = new SimpleManagedElementSet<Node>();
        s2.add(n3);
        s2.add(n4);

        Set<ManagedElementSet<Node>> ss = new HashSet<ManagedElementSet<Node>>();
        ss.add(s1);
        ss.add(s2);

        ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
        vms.add(vm1);
        vms.add(vm2);
        v.addConstraint(new ContinuousSpread(vms));
        v.addConstraint(new Among(vms, ss));
        v.addConstraint(new Capacity(s1, 15));

        try {
            File f = File.createTempFile("foo", "bar");
            f.deleteOnExit();
            ProtobufVJobSerializer.getInstance().serialize(v, new FileOutputStream(f));
            DefaultPBConstraintsCatalog c = new DefaultPBConstraintsCatalog();
            c.add(new ContinuousSpreadBuilder());
            c.add(new AmongBuilder());
            c.add(new CapacityBuilder());
            VJobElementBuilder eB = new DefaultVJobElementBuilder(null);
            ProtobufVJobBuilder b = new ProtobufVJobBuilder(eB);
            b.setConstraintCatalog(c);
            eB.useConfiguration(cfg);
            VJob v2 = b.build(f);
            Assert.assertEquals(v.getVirtualMachines(), v2.getVirtualMachines());
            for (VirtualMachine vm : v.getVirtualMachines()) {
                VirtualMachine x = v2.getVirtualMachines().get(vm.getName());
                Assert.assertEquals(vm.getName(), x.getName());
                Assert.assertEquals(vm.getNbOfCPUs(), x.getNbOfCPUs());
                Assert.assertEquals(vm.getCPUConsumption(), x.getCPUConsumption());
                Assert.assertEquals(vm.getCPUDemand(), x.getCPUDemand());
                Assert.assertEquals(vm.getCPUMax(), x.getCPUMax());
                Assert.assertEquals(vm.getMemoryConsumption(), x.getMemoryConsumption());
                Assert.assertEquals(vm.getMemoryDemand(), x.getMemoryDemand());
                Assert.assertEquals(vm.getOptions(), x.getOptions());
            }
            Assert.assertEquals(v.getConstraints(), v2.getConstraints());
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }
}
