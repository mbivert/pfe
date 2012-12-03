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
import entropy.plan.choco.ReconfigurationProblem;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestInstantiateActionModel {

    public void testActionModelisation() {
        Configuration src = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 5, 5);
        Node n2 = new SimpleNode("N2", 1, 5, 5);
        src.addOnline(n1);
        src.addOnline(n2);
        VirtualMachine vm = new SimpleVirtualMachine("vm", 1, 1, 1);
        vm.setCPUDemand(3);
        vm.setMemoryDemand(4);
        Configuration dst = src.clone();
        dst.addWaiting(vm);
        ReconfigurationProblem model = TimedReconfigurationPlanModelHelper.makeBasicModel(src, dst);
        InstantiateActionModel a = (InstantiateActionModel) model.getAssociatedAction(vm);
        Assert.assertEquals(a.getVirtualMachine(), vm);
        Assert.assertNull(a.getDemandingSlice());
        Assert.assertNull(a.getConsumingSlice());
        Assert.assertEquals(a.getGlobalCost().getVal(), 9);
        Assert.assertEquals(a.start().getVal(), 0);
        Assert.assertEquals(a.end().getVal(), 9);
        Assert.assertEquals(a.getDuration().getVal(), 9);
    }

    /**
     * Test with the generation of an instantiation action
     */
    public void testSolve() {
        Configuration src = new SimpleConfiguration();
        Node n = new SimpleNode("N1", 1, 1, 1);
        src.addOnline(n);
        VirtualMachine vm = new SimpleVirtualMachine("vm", 1, 1, 1);
        Configuration dst = src.clone();
        dst.addWaiting(vm);
        ReconfigurationProblem model = TimedReconfigurationPlanModelHelper.makeBasicModel(src, dst);
        InstantiateActionModel a = (InstantiateActionModel) model.getAssociatedAction(vm);
        try {
            Assert.assertTrue(model.solve());
            Instantiate inst = (Instantiate) a.getDefinedAction(model).get(0);
            TimedReconfigurationPlan p = model.extractSolution();
            Assert.assertEquals(p.getDuration(), inst.getFinishMoment());
            Assert.assertTrue(p.getActions().contains(inst) && p.getActions().size() == 1);
            Assert.assertEquals(a.getGlobalCost().getVal(), 9);
            Assert.assertEquals(a.getDuration().getVal(), 9);
        } finally {
            ChocoLogging.flushLogs();
        }
    }
}
