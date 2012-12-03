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

import choco.kernel.solver.ContradictionException;
import entropy.configuration.*;
import entropy.plan.action.Stop;
import entropy.plan.choco.ReconfigurationProblem;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for StopActionModel.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit", "RP-core"})
public class TestStopActionModel {

    /**
     * Test the creation of a plan composed by a stop action, and
     * the solving process
     */
    public void testSolvingWithStopActionCreation() {
        Configuration src = new SimpleConfiguration();
        Node n = new SimpleNode("n", 1, 1, 1);
        VirtualMachine vm = new SimpleVirtualMachine("VM1", 1, 1, 1);
        src.addOnline(n);
        src.setRunOn(vm, n);

        Configuration dst = src.clone();
        dst.remove(vm);

        ReconfigurationProblem model = TimedReconfigurationPlanModelHelper.makeBasicModel(src, dst);
        StopActionModel a = (StopActionModel) model.getAssociatedAction(vm);
        Assert.assertEquals(a.getDuration().getVal(), 2);
        try {
            a.getConsumingSlice().duration().setInf(20);
        } catch (ContradictionException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(a.getConsumingSlice());
        Assert.assertEquals(a.getVirtualMachine(), vm);
        Assert.assertEquals(a.getConsumingSlice().getCPUheight(), vm.getCPUConsumption());
        Assert.assertEquals(a.getConsumingSlice().getMemoryheight(), vm.getMemoryConsumption());
        Assert.assertTrue(model.solve(false));
        Stop s = (Stop) a.getDefinedAction(model).get(0);
        Assert.assertEquals(s.getVirtualMachine(), vm);
        Assert.assertEquals(s.getStartMoment(), 0);
        Assert.assertEquals(s.getFinishMoment(), 2);
    }
}
