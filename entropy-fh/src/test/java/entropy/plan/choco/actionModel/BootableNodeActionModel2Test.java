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

import entropy.configuration.*;
import entropy.plan.PlanException;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.action.Startup;
import entropy.plan.choco.DefaultReconfigurationProblem;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSlicesHeightsFastBP;
import entropy.plan.choco.constraint.sliceScheduling.SlicesPlanner;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Unit tests for {@link BootableNodeActionModel2}
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class BootableNodeActionModel2Test {

    public void testBasics() {
        Node n = new SimpleNode("N1", 5, 5, 5);
        Configuration cfg = new SimpleConfiguration();
        cfg.addOffline(n);
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

            BootableNodeActionModel2 a = new BootableNodeActionModel2(rp, n, 7, true);
            Assert.assertEquals(a.getNode(), n);
            Assert.assertTrue(a.getGlobalCost().isInstantiated() && a.getGlobalCost().getVal() == 7);
            Assert.assertTrue(a.end().isInstantiated() && a.end().getVal() == 7);
            Assert.assertTrue(a.getDuration().isInstantiated() && a.getDuration().getVal() == 7);
            Assert.assertTrue(a.getState().isInstantiated() && a.getState().getVal() == 1);
            Assert.assertTrue(a.getConsumingSlice().hoster().isInstantiated() && a.getConsumingSlice().hoster().getVal() == rp.getNode(n));
            Assert.assertEquals(a.getDefinedAction(rp).size(), 1);
            Startup st = (Startup) a.getDefinedAction(rp).get(0);
            Assert.assertEquals(st.getStartMoment(), 0);
            Assert.assertEquals(st.getFinishMoment(), 7);
            Assert.assertEquals(st.getNode(), n);

            a = new BootableNodeActionModel2(rp, n, 7, false);
            Assert.assertEquals(a.getNode(), n);
            Assert.assertFalse(a.getGlobalCost().isInstantiated());
            Assert.assertEquals(a.end(), a.getGlobalCost());
            Assert.assertEquals(a.end(), a.getDuration());
            Assert.assertFalse(a.getState().isInstantiated());
            Assert.assertTrue(a.getConsumingSlice().hoster().isInstantiated() && a.getConsumingSlice().hoster().getVal() == rp.getNode(n));
            Assert.assertEquals(a.getDefinedAction(rp).size(), 0);
        } catch (PlanException e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    public void testBootForFun() {
        Node n = new SimpleNode("N1", 5, 5, 5);
        Configuration cfg = new SimpleConfiguration();
        cfg.addOffline(n);
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

            BootableNodeActionModel2 a = (BootableNodeActionModel2) rp.getAssociatedAction(n);
            a.getState().setVal(1);
            Assert.assertTrue(rp.solve());
            TimedReconfigurationPlan p = rp.extractSolution();
            System.err.println(p);
            Assert.assertEquals(p.getActions().size(), 1);
            Assert.assertEquals(p.getDuration(), 8);
            Assert.assertTrue(p.getDestination().isOnline(n));
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    public void testBootForbiddenForFun() {
        Node n = new SimpleNode("N1", 5, 5, 5);
        Configuration cfg = new SimpleConfiguration();
        cfg.addOffline(n);
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
            BootableNodeActionModel2 a = (BootableNodeActionModel2) rp.getAssociatedAction(n);
            a.getState().setVal(0);
            Assert.assertTrue(rp.solve());
            TimedReconfigurationPlan p = rp.extractSolution();
            System.err.println(p);
            Assert.assertEquals(p.getActions().size(), 0);
            Assert.assertEquals(p.getDuration(), 0);
            Assert.assertTrue(p.getDestination().isOffline(n));

        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    public void testBootRequiredForVM() {
        Node n = new SimpleNode("N1", 5, 5, 5);
        VirtualMachine vm = new SimpleVirtualMachine("VM1", 1, 1, 1);
        Configuration cfg = new SimpleConfiguration();
        cfg.addOffline(n);
        cfg.addWaiting(vm);
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

            Assert.assertTrue(rp.solve());
            TimedReconfigurationPlan p = rp.extractSolution();
            System.err.println(p);
            Assert.assertEquals(p.getActions().size(), 2);
            Assert.assertEquals(p.getDuration(), 12);
            Assert.assertTrue(p.getDestination().isOnline(n));

        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }
}
