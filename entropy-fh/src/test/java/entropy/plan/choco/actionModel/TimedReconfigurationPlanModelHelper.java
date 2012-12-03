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

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.DefaultReconfigurationProblem;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSlicesHeightsFastBP;
import entropy.plan.choco.constraint.sliceScheduling.SlicesPlanner;
import entropy.plan.durationEvaluator.DurationEvaluator;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A helper class to create simple BasicTimedReconfigurationPlanModel.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit", "RP-core"})
public class TimedReconfigurationPlanModelHelper {

    /**
     * Make a model that aims to pass from a source to a destination configuration.
     *
     * @param src The source configuration
     * @param dst The destination configuration
     * @return the model
     */
    public static ReconfigurationProblem makeBasicModel(Configuration src, Configuration dst) {
        return makeBasicModel(src, dst, new MockDurationEvaluator(9, 1, 2, 3, 4, 5, 6, 7, 8));
    }

    public static ReconfigurationProblem makeBasicModel(Configuration src, Configuration dst, DurationEvaluator ev) {
        try {
            ManagedElementSet<VirtualMachine> toTerminate = src.getAllVirtualMachines().clone();
            toTerminate.removeAll(dst.getAllVirtualMachines());
            ReconfigurationProblem pb = new DefaultReconfigurationProblem(src,
                    dst.getRunnings(),
                    dst.getWaitings(),
                    dst.getSleepings(),
                    toTerminate,
                    src.getAllVirtualMachines(),
                    dst.getOnlines(),
                    dst.getOfflines(),
                    ev);
            new SatisfyDemandingSlicesHeightsFastBP().add(pb);
            new SlicesPlanner().add(pb);
            return pb;
        } catch (Exception e2) {
            Assert.fail(e2.getMessage(), e2);
        }
        return null;
    }
}
