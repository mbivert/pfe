/*
 * Copyright (c) 2010 Fabien Hermenier
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

package entropy.plan.choco.actionModel;

import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.PlanException;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.choco.DefaultReconfigurationProblem;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSlicesHeightsFastBP;
import entropy.plan.choco.constraint.sliceScheduling.SlicesPlanner;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Unit tests for {@link entropy.plan.choco.actionModel.ShutdownableNodeActionModel}
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class ShutdownableNodeActionModelTest {

    public void testBasics() {
        Node n = new SimpleNode("N1", 5, 5, 5);
        Configuration cfg = new SimpleConfiguration();
        cfg.addOnline(n);
        try {
            ReconfigurationProblem rp = new DefaultReconfigurationProblem(cfg,
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<Node>(),
                    new SimpleManagedElementSet<Node>(),
                    new MockDurationEvaluator(1, 2, 3, 4, 5, 6, 7, 8, 9)
            );
            new SatisfyDemandingSlicesHeightsFastBP().add(rp);
            new SlicesPlanner().add(rp);

            ShutdownableNodeActionModel a = new ShutdownableNodeActionModel(rp, n, 7, true);
            Assert.assertEquals(a.getNode(), n);
            Assert.assertEquals(a.getGlobalCost().getInf(), 7);
            Assert.assertEquals(a.getDuration().getInf(), 7);
            Assert.assertTrue(a.getState().isInstantiated() && a.getState().getVal() == 0);
            Assert.assertTrue(a.getDemandingSlice().hoster().isInstantiated() && a.getDemandingSlice().hoster().getVal() == rp.getNode(n));
            Assert.assertEquals(a.getDefinedAction(rp).size(), 1);

            a = new ShutdownableNodeActionModel(rp, n, 7, false);
            Assert.assertEquals(a.getNode(), n);
            Assert.assertFalse(a.getGlobalCost().isInstantiated());
            Assert.assertFalse(a.getState().isInstantiated());
            Assert.assertTrue(a.getDemandingSlice().hoster().isInstantiated() && a.getDemandingSlice().hoster().getVal() == rp.getNode(n));
            Assert.assertEquals(a.getDefinedAction(rp).size(), 0);
        } catch (PlanException e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    public void testShutdownForFun() {
        Node n = new SimpleNode("N1", 5, 5, 5);
        Configuration cfg = new SimpleConfiguration();
        cfg.addOnline(n);
        try {
            ReconfigurationProblem rp = new DefaultReconfigurationProblem(cfg,
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<Node>(),
                    new SimpleManagedElementSet<Node>(),
                    new MockDurationEvaluator(1, 2, 3, 4, 5, 6, 7, 8, 9)
            );
            new SatisfyDemandingSlicesHeightsFastBP().add(rp);
            new SlicesPlanner().add(rp);

            ShutdownableNodeActionModel a = (ShutdownableNodeActionModel) rp.getAssociatedAction(n);
            a.getState().setVal(0);
            Assert.assertTrue(rp.solve());
            TimedReconfigurationPlan p = rp.extractSolution();
            System.err.println(p);
            Assert.assertEquals(p.getActions().size(), 1);
            Assert.assertEquals(p.getDuration(), 9);
            Assert.assertTrue(p.getDestination().isOffline(n));
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    public void testShutdownForbiddenForFun() {
        Node n = new SimpleNode("N1", 5, 5, 5);
        Configuration cfg = new SimpleConfiguration();
        cfg.addOnline(n);
        try {
            ReconfigurationProblem rp = new DefaultReconfigurationProblem(cfg,
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<Node>(),
                    new SimpleManagedElementSet<Node>(),
                    new MockDurationEvaluator(1, 2, 3, 4, 5, 6, 7, 8, 9)
            );
            new SatisfyDemandingSlicesHeightsFastBP().add(rp);
            new SlicesPlanner().add(rp);
            ShutdownableNodeActionModel a = (ShutdownableNodeActionModel) rp.getAssociatedAction(n);
            a.getState().setVal(1);    //Set online
            boolean ret = rp.solve();
            System.err.println(ret);
            Assert.assertTrue(ret);
            Assert.assertEquals(a.getGlobalCost().getVal(), 0);
            Assert.assertEquals(a.end().getVal(), 0);

            TimedReconfigurationPlan p = rp.extractSolution();
            System.err.println(p);
            Assert.assertEquals(p.getActions().size(), 0);
            Assert.assertEquals(p.getDuration(), 0);
            Assert.assertTrue(p.getDestination().isOnline(n));


        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    public void testShutdownRequiredWithRunningVM() {
        Node n = new SimpleNode("N1", 5, 5, 5);
        Node n2 = new SimpleNode("N2", 5, 5, 5);
        VirtualMachine vm = new SimpleVirtualMachine("VM1", 1, 1, 1);
        Configuration cfg = new SimpleConfiguration();
        cfg.addOnline(n);
        cfg.addOnline(n2);
        cfg.setRunOn(vm, n);
        try {
            ReconfigurationProblem rp = new DefaultReconfigurationProblem(cfg,
                    cfg.getAllVirtualMachines(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<Node>(),
                    new SimpleManagedElementSet<Node>(),
                    new MockDurationEvaluator(1, 2, 3, 4, 5, 6, 7, 8, 9)
            );
            new SatisfyDemandingSlicesHeightsFastBP().add(rp);
            new SlicesPlanner().add(rp);

            ShutdownableNodeActionModel a = (ShutdownableNodeActionModel) rp.getAssociatedAction(n);
            a.getState().setVal(0);

            Assert.assertTrue(rp.solve());
            TimedReconfigurationPlan p = rp.extractSolution();
            System.err.println(p);
            Assert.assertEquals(p.getActions().size(), 2);
            Assert.assertEquals(p.getDuration(), 11);
            Assert.assertTrue(p.getDestination().isOffline(n));

        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }


    public void testShutdownConstrained() {
        Node n = new SimpleNode("N1", 5, 5, 5);
        Node n2 = new SimpleNode("N2", 5, 5, 5);
        VirtualMachine vm = new SimpleVirtualMachine("VM1", 1, 1, 1);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 1);
        Configuration cfg = new SimpleConfiguration();
        cfg.addOnline(n);
        cfg.addOnline(n2);
        cfg.setRunOn(vm, n);
        cfg.setRunOn(vm2, n2);
        try {
            ReconfigurationProblem rp = new DefaultReconfigurationProblem(cfg,
                    cfg.getAllVirtualMachines(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<Node>(),
                    new SimpleManagedElementSet<Node>(),
                    new MockDurationEvaluator(1, 2, 3, 4, 5, 6, 7, 8, 9)
            );
            new SatisfyDemandingSlicesHeightsFastBP().add(rp);
            new SlicesPlanner().add(rp);

            IntDomainVar st1 = ((ShutdownableNodeActionModel) rp.getAssociatedAction(n)).getState();
            IntDomainVar st2 = ((ShutdownableNodeActionModel) rp.getAssociatedAction(n)).getState();
            rp.post(rp.leq(rp.plus(st1, st2), 1));

            Assert.assertTrue(rp.solve());
            TimedReconfigurationPlan p = rp.extractSolution();
            System.err.println(p);
            Assert.assertEquals(p.getActions().size(), 2);
            Assert.assertEquals(p.getDuration(), 11);
            Assert.assertTrue(p.getDestination().isOffline(n));
            System.err.println(p);

        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }
}
