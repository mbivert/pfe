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

import java.util.Collections;


/**
 * Unit tests related to ManagedElementSet.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestSimpleManagedElementSet {

    /**
     * Make a default set of elements.
     *
     * @return a proper set
     */
    private static ManagedElementSet<MockManagedElement> makeDefaultSet() {
        ManagedElementSet<MockManagedElement> set = new SimpleManagedElementSet<MockManagedElement>();
        MockManagedElement n = new MockManagedElement("N0");
        set.add(n);

        n = new MockManagedElement("N1");
        set.add(n);

        n = new MockManagedElement("N2");
        set.add(n);

        n = new MockManagedElement("N3");
        set.add(n);

        return set;
    }

    public void testBadContains() {
        MockManagedElement n = new MockManagedElement("N0");
        ManagedElementSet<MockManagedElement> set = new SimpleManagedElementSet<MockManagedElement>(n);
        set.add(n);
        Assert.assertTrue(set.contains(n));
        Object o = new Object();
        Assert.assertFalse(set.contains(o));
    }

    public void testSingleton() {
        MockManagedElement n = new MockManagedElement("N0");
        ManagedElementSet<MockManagedElement> set = new SimpleManagedElementSet<MockManagedElement>(n);
        Assert.assertTrue(set.contains(n));
        Assert.assertEquals(set.size(), 1);
    }

    public void testGetFromName() {
        ManagedElementSet<MockManagedElement> orig = TestSimpleManagedElementSet.makeDefaultSet();
        Assert.assertEquals(orig.get("N0"), new MockManagedElement("N0"));
        Assert.assertNull(orig.get("N7"));
    }

    /**
     * Check that the copy constructor makes a deep copy.
     */
    public void testCopyConstructor() {
        ManagedElementSet<MockManagedElement> orig = TestSimpleManagedElementSet.makeDefaultSet();
        ManagedElementSet<MockManagedElement> copy = orig.clone();

        // Not the same reference
        Assert.assertFalse(orig == copy,
                "The copy should not have the same reference");

        // But the same content
        Assert.assertEquals(copy, orig);
    }

    /**
     * Test the non-possibility of having 2 elements with the same name.
     */
    public void testAdd() {
        ManagedElementSet<MockManagedElement> orig = TestSimpleManagedElementSet.makeDefaultSet();
        Assert.assertFalse(orig.add(new MockManagedElement("N0")));
        Assert.assertEquals(orig.size(), 4);
    }

    public void testRemove() {
        ManagedElementSet<MockManagedElement> orig = TestSimpleManagedElementSet.makeDefaultSet();
        MockManagedElement m = orig.get("N1");
        Assert.assertTrue(orig.remove(m));
        Assert.assertFalse(orig.contains(m));
        MockManagedElement m2 = new MockManagedElement("hop");
        Assert.assertFalse(orig.remove(m2));
    }

    public void testClear() {
        ManagedElementSet<MockManagedElement> orig = TestSimpleManagedElementSet.makeDefaultSet();
        ManagedElementSet<MockManagedElement> orig2 = orig.clone();
        orig2.clear();
        Assert.assertEquals(orig2.size(), 0);
        for (MockManagedElement e : orig) {
            Assert.assertFalse(orig2.contains(e));
        }

    }

    /**
     * Test the equals() method.
     */
    public void testEquals() {
        ManagedElementSet<MockManagedElement> orig = TestSimpleManagedElementSet.makeDefaultSet();
        ManagedElementSet<MockManagedElement> clone = TestSimpleManagedElementSet.makeDefaultSet();
        Assert.assertEquals(clone, orig);
        clone.remove(clone.size() - 1);
        Assert.assertNotSame(clone, orig);

        clone = TestSimpleManagedElementSet.makeDefaultSet();
        orig.remove(clone.size() - 1);
        Assert.assertNotSame(clone, orig);

        Assert.assertFalse(orig.equals(new Object()));
    }

    /**
     * Test addAll().
     */
    public void testAddAll() {
        ManagedElementSet<MockManagedElement> set = makeDefaultSet();
        ManagedElementSet<MockManagedElement> s2 = new SimpleManagedElementSet<MockManagedElement>();
        Assert.assertFalse(set.addAll(s2));
        s2.add(new MockManagedElement("N0"));
        Assert.assertFalse(set.addAll(s2));
        s2.add(new MockManagedElement("aaaa"));
        Assert.assertTrue(set.addAll(s2));
    }

    public void testRetainAll() {
        ManagedElementSet<MockManagedElement> set = makeDefaultSet();
        ManagedElementSet<MockManagedElement> toKeep = new SimpleManagedElementSet<MockManagedElement>();
        MockManagedElement n1 = set.get("N1");
        MockManagedElement n2 = set.get("N2");
        MockManagedElement n3 = set.get("N3");
        MockManagedElement n7 = new MockManagedElement("N7");
        toKeep.add(n1);
        toKeep.add(n2);
        toKeep.add(n7);
        Assert.assertTrue(set.retainAll(toKeep));
        Assert.assertTrue(set.contains(n1));
        Assert.assertTrue(set.contains(n2));
        Assert.assertFalse(set.contains(n3));
        Assert.assertNull(set.get("N0"));
    }

    public void testToString() {
        ManagedElementSet<MockManagedElement> set = makeDefaultSet();
        Assert.assertEquals(set.toString(), "{N0, N1, N2, N3}");
    }

    public void testSet() {
        ManagedElementSet<MockManagedElement> set = makeDefaultSet();
        MockManagedElement n0 = set.get(0);
        MockManagedElement n3 = set.get(3);
        set.set(3, n0);
        set.set(0, n3);
        Assert.assertEquals(set.toString(), "{N3, N1, N2, N0}");
        Assert.assertTrue(set.contains(n0));
        Assert.assertTrue(set.contains(n3));
        Assert.assertEquals(set.get("N3"), n3);
        Assert.assertEquals(set.get("N0"), n0);
    }

    /**
     * Bug revealed by Corentin Dupont @ Fit4Green.
     */
    public void testSet2() {

        ManagedElementSet<MockManagedElement> s = new SimpleManagedElementSet<MockManagedElement>();
        MockManagedElement e1 = new MockManagedElement("e1");
        MockManagedElement e2 = new MockManagedElement("e2");

        Assert.assertTrue(s.add(e1));
        s.set(0, e1);
        Assert.assertTrue(s.contains(e1));

        s.set(0, e1);
        Assert.assertTrue(s.contains(e1));


        ManagedElementSet<VirtualMachine> s1 = new SimpleManagedElementSet<VirtualMachine>();
        VirtualMachine vm = new SimpleVirtualMachine("i-c2b4711d", 1, 40, 1024);
        s1.add(vm);
        Assert.assertTrue(s1.contains(vm));
        System.err.println(s1);
        Collections.sort(s1, new VirtualMachineComparator(false, ResourcePicker.VMRc.nbOfCPUs));
        System.err.println(s1);
        Assert.assertTrue(s1.contains(vm));
    }

    public void testCollision() {
        ManagedElementSet<ManagedElement> vms = new SimpleManagedElementSet<ManagedElement>();
        //Both VMs have the same hashCode, leading to a collision when the hashCode was used as a key
        //inside the set.
        ManagedElement vm1 = new MockDefaultManagedElement("VMa2697_id30300000b");
        ManagedElement vm2 = new MockDefaultManagedElement("VMa4141_id19400000");
        Assert.assertTrue(vms.add(vm1));
        Assert.assertTrue(vms.add(vm2));
        Assert.assertEquals(vms.size(), 2);

    }
}
