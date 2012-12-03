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
package entropy.plan.action;

import entropy.configuration.*;
import entropy.execution.TimedExecutionGraph;
import entropy.plan.MockPlanVisualizer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests for Instantiate.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestInstantiate {

    /**
     * Dummy test to prevent NullPointerException.
     */
    public void testToString() {
        Instantiate s = new Instantiate(new SimpleVirtualMachine("VM1", 1, 2, 3));
        Assert.assertNotNull(s.toString());
    }


    /**
     * Dummy test to check call to visu.inject(this).
     */
    public void testVisualizationInjection() {
        MockPlanVisualizer visu = new MockPlanVisualizer();
        Instantiate s = new Instantiate(new SimpleVirtualMachine("VM1", 1, 2, 3));
        s.injectToVisualizer(visu);
        Assert.assertTrue(visu.isInjected(s));
    }

    /**
     * Test apply().
     */
    public void testApplyWithAlreadyOnline() {
        Configuration c = new SimpleConfiguration();
        VirtualMachine vm1 = new SimpleVirtualMachine("vm1", 1, 1, 1);
        Node n1 = new SimpleNode("n1", 1, 2, 3);
        c.addOnline(n1);
        c.setRunOn(vm1, n1);
        Instantiate r = new Instantiate(vm1);
        r.apply(c);
        Assert.assertEquals(c.getLocation(vm1), n1);
    }

    /**
     * Test apply().
     */
    public void testApply() {
        Configuration c = new SimpleConfiguration();
        VirtualMachine vm1 = new SimpleVirtualMachine("vm1", 1, 1, 1);
        Instantiate r = new Instantiate(vm1);
        r.apply(c);
        Assert.assertTrue(c.isWaiting(vm1));
    }

    /**
     * Test isIncompatibleWith() when the action is not right.
     *
     * @param r the bad action
     */
    @Test(dataProvider = "getWrongInstantiates")
    public void testIsIncompatibleWith(Instantiate r) {
        Configuration c = new SimpleConfiguration();
        Node n1 = new SimpleNode("n1", 1, 2, 3);
        Node n2 = new SimpleNode("n2", 1, 2, 3);
        c.addOnline(n1);
        c.addOffline(n2);
        c.addWaiting(new SimpleVirtualMachine("VM1", 1, 2, 3));
        c.setSleepOn(new SimpleVirtualMachine("VM10", 1, 2, 3), n1);
        Assert.assertFalse(r.isCompatibleWith(c));
    }

    /**
     * Data provider for testWrongMigrationAppends(...).
     *
     * @return sample datas.
     */
    @DataProvider(name = "getWrongInstantiates")
    public Object[][] getWrongInstantiates() {
        return new Object[][]{
                {new Instantiate(new SimpleVirtualMachine("VM10", 1, 1, 1))}, //bad state
                {new Instantiate(new SimpleVirtualMachine("VM1", 1, 1, 1))}, //migration on a unknown node
        };
    }

    public void testInsertIntoGraph() {
        TimedExecutionGraph graph = new TimedExecutionGraph();
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 2, 3);
        Instantiate ma = new Instantiate(vm1);
        Assert.assertTrue(ma.insertIntoGraph(graph));
    }

    public void testVisuInject() {
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 2, 3);
        Instantiate ma = new Instantiate(vm1);
        MockPlanVisualizer v = new MockPlanVisualizer();
        ma.injectToVisualizer(v);
        Assert.assertTrue(v.isInjected(ma));
    }
}
