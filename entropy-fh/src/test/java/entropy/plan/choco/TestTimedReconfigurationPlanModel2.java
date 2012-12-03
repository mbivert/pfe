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

package entropy.plan.choco;

import entropy.configuration.*;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.choco.actionModel.TimedReconfigurationPlanModelHelper;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestTimedReconfigurationPlanModel2 {

    /**
     * An offline node have to be booted to host a VM.
     * TODO: make new reconfiguration problem infering about the state of the nodes
     */
    public void testWithNodeToBoot() {
        /*Configuration src = new Configuration();
        Node n1 = new Node("N1", 1, 1, 1);
        Node n2 = new Node("N2", 2, 2, 2);
        VirtualMachine vm1 = new VirtualMachine("VM1", 1, 1, 1);
        vm1.setCPUNeed(2);
        src.addOnline(n1);
        src.addOffline(n2);
        src.setRunOn(vm1, n1);
        ReconfigurationProblem m = TimedReconfigurationPlanModelHelper.makeBasicModel(src, src);
        Assert.assertTrue(m.solve());
        TimedReconfigurationPlan p = m.extractSolution();
        Configuration c = p.getDestination();
        Assert.assertEquals(p.size(), 2);
        Assert.assertTrue(c.getOnlines().contains(n2));  */
    }

    /**
     * A node is offline, but not required to boot to host a VM.
     */
    public void testIdle() {
        Configuration src = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 1, 1);
        Node n2 = new SimpleNode("N2", 2, 2, 2);
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        src.addOnline(n1);
        src.addOffline(n2);
        src.setRunOn(vm1, n1);
        ReconfigurationProblem m = TimedReconfigurationPlanModelHelper.makeBasicModel(src, src);
        Assert.assertTrue(m.solve());
        TimedReconfigurationPlan p = m.extractSolution();
        System.out.println(p);
        Assert.assertEquals(p.size(), 0);
    }

    /**
     * A node is offline, but not required to boot to host a VM as another online node is available
     */
    public void testIdle2() {
        Configuration src = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 1, 1);
        Node n2 = new SimpleNode("N2", 2, 2, 2);
        Node n3 = new SimpleNode("N3", 2, 2, 2);
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        vm1.setCPUDemand(2);
        src.addOnline(n1);
        src.addOffline(n2);
        src.addOnline(n3);
        src.setRunOn(vm1, n1);
        ReconfigurationProblem m = TimedReconfigurationPlanModelHelper.makeBasicModel(src, src);
        Assert.assertTrue(m.solve());
        TimedReconfigurationPlan p = m.extractSolution();
        Assert.assertEquals(p.size(), 1);
    }

    /*public void testIdleWithShutdown() {
        Assert.fail();
        Configuration src = new Configuration();
        Node n1 = new Node("N1", 1, 1, 1);
        Node n2 = new Node("N2", 2, 2, 2);
        Node n3 = new Node("N3", 2, 2, 2);
        VirtualMachine vm1 = new VirtualMachine("VM1", 1, 1, 1);
        vm1.setCPUNeed(2);
        src.addOnline(n1);
        src.addOffline(n2);
        src.addOnline(n3);
        src.setRunOn(vm1, n1);
        DefaultReconfigurationProblem m = TimedReconfigurationPlanModelHelper.makeBasicModel(src, src);
        try {
            ShutdownNodeActionModel a = new ShutdownNodeActionModel(m, n1, 8);
            m.addAction(a);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
        new SatisfyDemandingSlicesHeightsBP().addToModel(m);
        new SlicesPlanner().addToModel(m);

        TimedReconfigurationPlanSolver s = new TimedReconfigurationPlanSolver2();
        s.read(m);
        Assert.assertTrue(s.solve());
        s.setTimeLimit(5000);
        DefaultTimedReconfigurationPlan p = s.getResultingTimedReconfigurationPlan();
        System.out.println(p);
        Assert.assertEquals(p.size(), 2);
    }

    public void testShutdownAsap() {
//        Assert.fail();
        BasicConfigurator.configure();
        ChocoLogging.setVerbosity(Verbosity.SEARCH);
        Configuration src = new Configuration();
        Node n1 = new Node("N1", 1, 1, 1);
        Node n2 = new Node("N2", 2, 2, 2);
        Node n3 = new Node("N3", 2, 2, 2);
        VirtualMachine vm1 = new VirtualMachine("VM1", 1, 1, 1);
        vm1.setCPUNeed(2);
        src.addOnline(n1);
        src.addOnline(n2);
        src.addOnline(n3);
        src.setRunOn(vm1, n1);
        TimedReconfigurationPlanModel2 m = makeModel(src, src);
        //m.getEnd().setUppB(10);
        try {
            ShutdownNodeActionModel a = new ShutdownNodeActionModel(m, n1, 8);
            m.addAction(a);

            a = new ShutdownNodeActionModel(m, n2, 8);
            m.addAction(a);

        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
        new SatisfyDemandingSlicesHeightsBP().addToModel(m);
        new SlicesPlanner().addToModel(m);
        m.linkModels();
        TimedReconfigurationPlanSolver s = new TimedReconfigurationPlanSolver2();
        s.read(m);
        Assert.assertTrue(s.solve());
        DefaultTimedReconfigurationPlan p = s.getResultingTimedReconfigurationPlan();
        System.out.println(p);
        Assert.assertEquals(p.size(), 3);
    }   */

}
