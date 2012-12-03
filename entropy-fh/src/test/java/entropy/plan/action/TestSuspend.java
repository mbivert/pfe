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
package entropy.plan.action;

import entropy.configuration.*;
import entropy.execution.TimedExecutionGraph;
import entropy.plan.MockPlanVisualizer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests for Suspend.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestSuspend {

    /**
     * Dummy test to prevent NullPointerException.
     */
    public void testToString() {
        Suspend s = new Suspend(new SimpleVirtualMachine("VM1", 1, 2, 3), new SimpleNode("n1", 1, 2, 3), new SimpleNode("n2", 1, 2, 3));
        Assert.assertNotNull(s.toString());
    }

    /**
     * Dummy test to check call to visu.inject(this).
     */
    public void testVisualizationInjection() {
        MockPlanVisualizer visu = new MockPlanVisualizer();
        Suspend s = new Suspend(new SimpleVirtualMachine("VM1", 1, 2, 3), new SimpleNode("n1", 1, 2, 3), new SimpleNode("n2", 1, 2, 3));
        s.injectToVisualizer(visu);
        Assert.assertTrue(visu.isInjected(s));
    }

    /**
     * Test apply().
     */
    public void testApply() {
        Configuration c = new SimpleConfiguration();
        Node n1 = new SimpleNode("n1", 1, 2, 3);
        Node n2 = new SimpleNode("n2", 1, 2, 3);
        VirtualMachine vm1 = new SimpleVirtualMachine("vm1", 1, 1, 1);
        c.addOnline(n1);
        c.addOnline(n2);
        c.setRunOn(vm1, n1);
        Suspend s = new Suspend(vm1, n1, n2);
        s.apply(c);
        Assert.assertTrue(c.isSleeping(vm1));
        Assert.assertEquals(c.getLocation(vm1), n2);
    }

    /**
     * Test isCompatibleWith() when all is right.
     */
    public void testIsCompatibleWith() {
        Configuration c = new SimpleConfiguration();
        Node n1 = new SimpleNode("n1", 1, 2, 3);
        Node n2 = new SimpleNode("n2", 1, 2, 3);
        VirtualMachine vm1 = new SimpleVirtualMachine("vm1", 1, 1, 1);
        c.addOnline(n1);
        c.addOnline(n2);
        c.setRunOn(vm1, n1);
        Suspend s = new Suspend(vm1, n1, n2);
        Assert.assertTrue(s.apply(c));
    }

    /**
     * Test isIncompatibleWith() when the action is not right.
     *
     * @param s the bad action
     */
    @Test(dataProvider = "getWrongSuspends")
    public void testIsIncompatibleWith(Suspend s) {
        Configuration c = new SimpleConfiguration();
        Node n1 = new SimpleNode("n1", 1, 2, 3);
        Node n2 = new SimpleNode("n2", 1, 2, 3);
        c.addOnline(n1);
        c.addOnline(n2);
        c.setRunOn(new SimpleVirtualMachine("VM1", 1, 2, 3), n1);
        c.setSleepOn(new SimpleVirtualMachine("VM10", 1, 2, 3), n1);
        Assert.assertFalse(s.isCompatibleWith(c));
    }

    /**
     * Data provider for testWrongMigrationAppends(...).
     *
     * @return sample datas.
     */
    @DataProvider(name = "getWrongSuspends")
    public Object[][] wgetWrongSuspends() {
        return new Object[][]{
                {new Suspend(new SimpleVirtualMachine("VM5", 1, 1, 1), new SimpleNode("n1", 1, 2, 3), new SimpleNode("n2", 1, 2, 3))}, // unknown VM
                {new Suspend(new SimpleVirtualMachine("VM10", 1, 1, 1), new SimpleNode("n1", 1, 2, 3), new SimpleNode("n2", 1, 2, 3))}, //bad state
                {new Suspend(new SimpleVirtualMachine("VM1", 1, 1, 1), new SimpleNode("n2", 1, 2, 3), new SimpleNode("n1", 1, 2, 3))}, //VM mislocated.
                {new Suspend(new SimpleVirtualMachine("VM1", 1, 1, 1), new SimpleNode("n1", 1, 2, 3), new SimpleNode("n7", 1, 2, 3))}, // on a unknow node
                {new Suspend(new SimpleVirtualMachine("VM1", 1, 1, 1), new SimpleNode("N55", 1, 1, 1), new SimpleNode("n2", 1, 2, 3))} // from a unknown node
        };
    }

    public void testInsertIntoGraph() {
        TimedExecutionGraph graph = new TimedExecutionGraph();
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 2, 3);
        Node n1 = new SimpleNode("N1", 1, 2, 3);
        Node n2 = new SimpleNode("N2", 1, 2, 3);
        Suspend ma = new Suspend(vm1, n1, n2);
        ma.insertIntoGraph(graph);
        Assert.assertTrue(graph.getUnlockings(n2).contains(ma));
    }
}
