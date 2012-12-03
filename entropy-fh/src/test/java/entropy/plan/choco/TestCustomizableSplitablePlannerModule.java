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

package entropy.plan.choco;

import entropy.PropertiesHelper;
import entropy.TestHelper;
import entropy.configuration.Configuration;
import entropy.configuration.SimpleManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.plan.SolutionStatistics;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.template.MockVirtualMachineTemplateFactory;
import entropy.template.VirtualMachineTemplateFactory;
import entropy.vjob.VJob;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.plasma.ConstraintsCatalogBuilderFromProperties;
import entropy.vjob.builder.plasma.PlasmaVJobBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestCustomizableSplitablePlannerModule {

    private static final String RESOURCES_DIR = "src/test/resources/entropy/plan/TestCustomizableSplitablePlannerModule.";

    private CustomizableSplitablePlannerModule makeModule() {
        return new CustomizableSplitablePlannerModule(new MockDurationEvaluator(7, 5, 1, 1, 7, 14, 7, 2, 4));
    }

    public void testBasics() {
        CustomizableSplitablePlannerModule planner = makeModule();
        Assert.assertEquals(planner.getPartitioningMode(), CustomizableSplitablePlannerModule.PartitioningMode.none);
        planner.setPartitioningMode(CustomizableSplitablePlannerModule.PartitioningMode.sequential);
        Assert.assertEquals(planner.getPartitioningMode(), CustomizableSplitablePlannerModule.PartitioningMode.sequential);

        Assert.assertEquals(planner.isRepairModeUsed(), true);
        planner.setRepairMode(false);
        Assert.assertEquals(planner.isRepairModeUsed(), false);

    }

    /**
     * Basic test with not that much action
     */
    public void test1() {
        CustomizableSplitablePlannerModule planner = makeModule();
        Configuration src = TestHelper.readConfiguration(RESOURCES_DIR + "splitted_cfg.txt");

        try {
            VirtualMachineTemplateFactory f = new MockVirtualMachineTemplateFactory();
            PlasmaVJobBuilder b = new PlasmaVJobBuilder(new DefaultVJobElementBuilder(f), new ConstraintsCatalogBuilderFromProperties(new PropertiesHelper("config/plasmaVJobs.properties")).build());
            b.getElementBuilder().useConfiguration(src);
            VJob v = b.build("m", new File(RESOURCES_DIR + "splitted.txt"));
            List<VJob> vjobs = new ArrayList<VJob>();
            vjobs.add(v);
            planner.setRepairMode(false);
            planner.setPartitioningMode(CustomizableSplitablePlannerModule.PartitioningMode.none);
            TimedReconfigurationPlan plan = planner.compute(src,
                    src.getRunnings(),
                    src.getWaitings(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getOnlines(),
                    src.getOfflines(),
                    vjobs);
            System.err.println(plan);
            Assert.assertEquals(plan.size(), 2);
            Assert.assertEquals(plan.getDuration(), 5);
            List<SolutionStatistics> stats = planner.getSolutionsStatistics();
            Assert.assertEquals(stats.get(stats.size() - 1).getObjective(), 10);

        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }
}
