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
package entropy.plan.choco.actionModel;

import choco.kernel.common.logging.ChocoLogging;
import entropy.configuration.*;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.action.Instantiate;
import entropy.plan.action.Run;
import entropy.plan.action.Stop;
import entropy.plan.action.VirtualMachineRename;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.durationEvaluator.DurationEvaluator;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for MigratableActionModel.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit", "RP-core"})
public class TestReInstantiateActionModel {

    private static DurationEvaluator ev = new MockDurationEvaluator(3, 10, 2, 4, 4, 5, 6, 7, 8);

    public void testActionModelisation() {
        Configuration src = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 5, 5);
        Node n2 = new SimpleNode("N2", 1, 5, 5);
        src.addOnline(n1);
        src.addOnline(n2);
        VirtualMachine vm = new SimpleVirtualMachine("vm", 1, 1, 1);
        vm.addOption("clone");
        vm.setCPUDemand(3);
        vm.setMemoryDemand(4);
        src.setRunOn(vm, n1);
        Configuration dst = src.clone();
        ReconfigurationProblem model = TimedReconfigurationPlanModelHelper.makeBasicModel(src, dst, ev);
        ReInstantiateActionModel a = (ReInstantiateActionModel) model.getAssociatedAction(vm);
        Assert.assertEquals(a.getVirtualMachine(), vm);
        Assert.assertEquals(a.getDemandingSlice().getCPUheight(), vm.getCPUDemand());
        Assert.assertEquals(a.getDemandingSlice().getMemoryheight(), vm.getMemoryDemand());
        Assert.assertEquals(a.getConsumingSlice().getCPUheight(), vm.getCPUConsumption());
        Assert.assertEquals(a.getConsumingSlice().getMemoryheight(), vm.getMemoryConsumption());
        Assert.assertNotNull(a.getGlobalCost());
        Assert.assertEquals(a.getDuration().getInf(), 0);
        Assert.assertEquals(a.getDuration().getSup(), 7);
        Assert.assertEquals(a.getDuration().getDomainSize(), 2);
    }

    /**
     * Test with a VM that stay at the same node.
     */
    public void testWithNoResultingMigration() {
        Configuration src = new SimpleConfiguration();
        Node n = new SimpleNode("N1", 1, 1, 1);
        src.addOnline(n);
        VirtualMachine vm = new SimpleVirtualMachine("vm", 1, 1, 1);
        src.setRunOn(vm, n);
        Configuration dst = src.clone();
        ReconfigurationProblem model = TimedReconfigurationPlanModelHelper.makeBasicModel(src, dst);
        MigratableActionModel a = (MigratableActionModel) model.getAssociatedAction(vm);
        try {
            Assert.assertTrue(model.solve());
            Assert.assertTrue(a.getDefinedAction(model).isEmpty());
            Assert.assertEquals(a.getGlobalCost().getVal(), 0);
            Assert.assertEquals(a.getDuration().getVal(), 0);
        } finally {
            ChocoLogging.flushLogs();
        }
    }

    /**
     * Test the detection of a migration.
     * The VM has to be migrated to satisfy its resources demand
     */
    public void testWithResultingMovement() {
        Configuration src = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 1, 1);
        Node n2 = new SimpleNode("N2", 1, 2, 2);
        src.addOnline(n1);
        src.addOnline(n2);
        VirtualMachine vm = new SimpleVirtualMachine("vm", 1, 1, 1);
        vm.addOption("clone");
        vm.setCPUDemand(2);
        vm.setMemoryDemand(1);
        src.setRunOn(vm, n1);
        Configuration dst = src.clone();
        ReconfigurationProblem model = TimedReconfigurationPlanModelHelper.makeBasicModel(src, dst, ev);
        Assert.assertTrue(model.solve(false));
        TimedReconfigurationPlan plan = model.extractSolution();
        System.err.println(plan);
        ReInstantiateActionModel a = (ReInstantiateActionModel) model.getAssociatedAction(vm);
        Instantiate i = (Instantiate) a.getDefinedAction(model).get(0);
        Run r = (Run) a.getDefinedAction(model).get(1);
        Stop s = (Stop) a.getDefinedAction(model).get(2);
        VirtualMachineRename rem = (VirtualMachineRename) a.getDefinedAction(model).get(3);
        Assert.assertEquals(i.getStartMoment(), 0);
        Assert.assertEquals(i.getFinishMoment(), 3);

        Assert.assertEquals(r.getStartMoment(), 3);
        Assert.assertEquals(r.getFinishMoment(), 7);

        Assert.assertEquals(s.getStartMoment(), 7);
        Assert.assertEquals(s.getFinishMoment(), 9);

        Assert.assertEquals(rem.getStartMoment(), 9);
        Assert.assertEquals(rem.getFinishMoment(), 10);

        TimedReconfigurationPlan p = model.extractSolution();
        Configuration res = p.getDestination();
        Assert.assertEquals(res.getLocation(vm), n2);
    }

    /**
     * Test the detection of a migration.
     * The VM has to be migrated to satisfy its resources demand
     */
    public void testReInstantiateWithDependency() {
        Configuration src = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 1, 1);
        Node n2 = new SimpleNode("N2", 1, 2, 2);
        src.addOnline(n1);
        src.addOffline(n2);
        VirtualMachine vm = new SimpleVirtualMachine("vm", 1, 1, 1);
        vm.addOption("clone");
        vm.setCPUDemand(2);
        vm.setMemoryDemand(1);
        src.setRunOn(vm, n1);
        Configuration dst = src.clone();
        dst.addOnline(n2);
        ReconfigurationProblem model = TimedReconfigurationPlanModelHelper.makeBasicModel(src, dst, ev);
        Assert.assertTrue(model.solve(false));
        ReInstantiateActionModel a = (ReInstantiateActionModel) model.getAssociatedAction(vm);
        Instantiate i = (Instantiate) a.getDefinedAction(model).get(0);
        Run r = (Run) a.getDefinedAction(model).get(1);
        Stop s = (Stop) a.getDefinedAction(model).get(2);
        VirtualMachineRename rem = (VirtualMachineRename) a.getDefinedAction(model).get(3);

        Assert.assertEquals(i.getStartMoment(), 0);
        Assert.assertEquals(i.getFinishMoment(), 3);

        Assert.assertEquals(r.getStartMoment(), 7);
        Assert.assertEquals(r.getFinishMoment(), 11);

        Assert.assertEquals(s.getStartMoment(), 11);
        Assert.assertEquals(s.getFinishMoment(), 13);

        Assert.assertEquals(rem.getStartMoment(), 13);
        Assert.assertEquals(rem.getFinishMoment(), 14);

        Assert.assertEquals(a.getGlobalCost().getVal(), 12);
    }
}
