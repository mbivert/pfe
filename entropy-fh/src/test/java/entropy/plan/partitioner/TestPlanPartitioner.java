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

package entropy.plan.partitioner;

import entropy.configuration.*;
import entropy.vjob.Among;
import entropy.vjob.Fence;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestPlanPartitioner {

    public void testSimpleFence() {
        Configuration cfg = new SimpleConfiguration();

        //Some nodes
        ManagedElementSet<Node>[] parts = new ManagedElementSet[4];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new SimpleManagedElementSet<Node>();
            for (int j = 0; j < 5; j++) {
                Node n = new SimpleNode("N" + (10 * i + j + 1), 10, 10, 10);
                parts[i].add(n);
                cfg.addOnline(n);
            }
        }

        //Some VMs
        ManagedElementSet<VirtualMachine>[] apps = new ManagedElementSet[5];
        for (int i = 0; i < apps.length; i++) {
            apps[i] = new SimpleManagedElementSet<VirtualMachine>();
            for (int j = 0; j < 10; j++) {
                VirtualMachine vm = new SimpleVirtualMachine("VM" + (i * 10 + j + 1), 1, 1, 1);
                apps[i].add(vm);
                cfg.addWaiting(vm);
            }
        }

        PlanPartitioner part = new OtherPartitioning(cfg);
        for (int i = 0; i < apps.length; i++) {
            Fence f = new Fence(apps[i], parts[i % parts.length]);
            try {
                part.part(f);
            } catch (PartitioningException e) {
                Assert.fail();
            }
        }
        List<Partition> ps = part.getResultingPartitions();
        Assert.assertEquals(ps.size(), 4);
        Assert.assertTrue(ps.get(0).getNodes().equals(parts[0]) && ps.get(0).getVirtualMachines().containsAll(apps[0]) && ps.get(0).getVirtualMachines().containsAll(apps[4]));
        Assert.assertTrue(ps.get(1).getNodes().equals(parts[1]) && ps.get(1).getVirtualMachines().containsAll(apps[1]));
        Assert.assertTrue(ps.get(2).getNodes().equals(parts[2]) && ps.get(2).getVirtualMachines().containsAll(apps[2]));
        Assert.assertTrue(ps.get(3).getNodes().equals(parts[3]) && ps.get(3).getVirtualMachines().containsAll(apps[3]));
    }

    /**
     * Test oneOf in presence of a fence constraint
     */
    public void testFenceOneOf() {
        Configuration cfg = new SimpleConfiguration();

        //Some nodes
        ManagedElementSet<Node>[] parts = new ManagedElementSet[2];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new SimpleManagedElementSet<Node>();
            for (int j = 0; j < 5; j++) {
                Node n = new SimpleNode("N" + (10 * i + j + 1), 10, 10, 10);
                parts[i].add(n);
                cfg.addOnline(n);
            }
        }

        //Some VMs
        ManagedElementSet<VirtualMachine> app = new SimpleManagedElementSet<VirtualMachine>();
        ManagedElementSet<VirtualMachine> sub = new SimpleManagedElementSet<VirtualMachine>();
        for (int i = 0; i < 10; i++) {
            VirtualMachine vm = new SimpleVirtualMachine("VM" + (i + 1), 1, 1, 1);
            cfg.addWaiting(vm);
            if (i < 5) {
                sub.add(vm);
            }
            app.add(vm);
        }

        PlanPartitioner part = new OtherPartitioning(cfg);
        Set<ManagedElementSet<Node>> m = new HashSet<ManagedElementSet<Node>>();
        m.add(parts[0]);
        m.add(parts[1]);
        Among of = new Among(app, m);
        Fence f = new Fence(app, parts[0]);

        try {
            part.part(f);
            part.part(of);
        } catch (PartitioningException e) {
            Assert.fail(e.getMessage(), e);
        }
        List<Partition> ps = part.getResultingPartitions();
    }

    public void testSinglePartition() {

    }
}
