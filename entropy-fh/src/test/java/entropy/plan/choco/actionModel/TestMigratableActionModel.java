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
package entropy.plan.choco.actionModel;

import choco.kernel.common.logging.ChocoLogging;
import entropy.configuration.*;
import entropy.plan.action.Migration;
import entropy.plan.choco.ReconfigurationProblem;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for MigratableActionModel.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit", "RP-core"})
public class TestMigratableActionModel {

    public void testActionModelisation() {
        Configuration src = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 5, 5);
        Node n2 = new SimpleNode("N2", 1, 5, 5);
        src.addOnline(n1);
        src.addOnline(n2);
        VirtualMachine vm = new SimpleVirtualMachine("vm", 1, 1, 1);
        vm.setCPUDemand(3);
        vm.setMemoryDemand(4);
        src.setRunOn(vm, n1);
        Configuration dst = src.clone();
        ReconfigurationProblem model = TimedReconfigurationPlanModelHelper.makeBasicModel(src, dst);
        //TimedReconfigurationPlanSolver solver = new BasicTimedReconfigurationPlanSolver();
        MigratableActionModel a = (MigratableActionModel) model.getAssociatedAction(vm);
        Assert.assertEquals(a.getVirtualMachine(), vm);
        Assert.assertEquals(a.getDemandingSlice().getCPUheight(), vm.getCPUDemand());
        Assert.assertEquals(a.getDemandingSlice().getMemoryheight(), vm.getMemoryDemand());
        Assert.assertEquals(a.getConsumingSlice().getCPUheight(), vm.getCPUConsumption());
        Assert.assertEquals(a.getConsumingSlice().getMemoryheight(), vm.getMemoryConsumption());
        Assert.assertNotNull(a.getGlobalCost());
        Assert.assertEquals(a.getDuration().getInf(), 0);
        Assert.assertEquals(a.getDuration().getSup(), 1);
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
    public void testWithResultingMigration() {
        Configuration src = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 1, 1);
        Node n2 = new SimpleNode("N2", 1, 2, 2);
        src.addOnline(n1);
        src.addOnline(n2);
        VirtualMachine vm = new SimpleVirtualMachine("vm", 1, 1, 1);
        vm.setCPUDemand(2);
        vm.setMemoryDemand(1);
        src.setRunOn(vm, n1);
        Configuration dst = src.clone();
        ReconfigurationProblem model = TimedReconfigurationPlanModelHelper.makeBasicModel(src, dst);
        Assert.assertTrue(model.solve(false));
        MigratableActionModel a = (MigratableActionModel) model.getAssociatedAction(vm);
        Migration m = (Migration) a.getDefinedAction(model).get(0);
        Assert.assertEquals(m.getStartMoment(), 0);
        Assert.assertEquals(m.getFinishMoment(), 1);
        Assert.assertEquals(m.getHost(), n1);
        Assert.assertEquals(m.getDestination(), n2);
        Assert.assertEquals(a.getGlobalCost().getVal(), 1);
        Assert.assertEquals(a.getDuration().getVal(), 1);
    }
}
