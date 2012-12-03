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

import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;
import entropy.PropertiesHelper;
import entropy.TestHelper;
import entropy.configuration.*;
import entropy.plan.*;
import entropy.plan.action.Action;
import entropy.plan.action.Migration;
import entropy.plan.action.Shutdown;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.template.MockVirtualMachineTemplateFactory;
import entropy.template.VirtualMachineTemplateFactory;
import entropy.vjob.DefaultVJob;
import entropy.vjob.Fence;
import entropy.vjob.LazySpread;
import entropy.vjob.VJob;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.plasma.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for CustomizablePlannerModule.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit", "RP-core"})
public class TestChocoCustomRP {

    private static final String RESOURCES_DIR = "src/test/resources/entropy/plan/choco/TestChocoCustomizablePlannerModule.";

    private ChocoCustomRP makeModule() {
        return new ChocoCustomRP(new MockDurationEvaluator(2, 5, 1, 1, 7, 14, 7, 2, 4));
    }

    @Test(expectedExceptions = {PlanException.class})
    public void testWithoutConstraintsAndSolution() throws PlanException {
        ChocoCustomRP planner = makeModule();
        planner.setTimeLimit(10);
        Configuration src = TestHelper.readConfiguration(RESOURCES_DIR + "noSolution.txt");
        List<VJob> vjobs = new ArrayList<VJob>();
        PlasmaVJob v = new BasicPlasmaVJob("v1");
        v.addVirtualMachines(src.getRunnings());
        MockVirtualMachineBuilder builder = new MockVirtualMachineBuilder();
        vjobs.add(v);
        try {
            planner.compute(src,
                    src.getRunnings(),
                    src.getWaitings(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getOnlines(),
                    src.getOfflines(),
                    vjobs);

        } catch (MultipleResultingStateException e) {
            Assert.fail(e.getMessage(), e);
        } catch (NoAvailableTransitionException e) {
            Assert.fail(e.getMessage(), e);
        } catch (NonViableSourceConfigurationException e) {
            Assert.fail(e.getMessage(), e);
        } catch (UnknownResultingStateException e) {
            Assert.fail(e.getMessage());
        }
    }

    public void testWithoutVJobs() {
        CustomizablePlannerModule planner = makeModule();
        Configuration src = TestHelper.readConfiguration(RESOURCES_DIR + "empty.txt");
        List<VJob> vjobs = new ArrayList<VJob>();
        MockVirtualMachineBuilder builder = new MockVirtualMachineBuilder();
        try {
            TimedReconfigurationPlan plan = planner.compute(src,
                    src.getRunnings(),
                    src.getWaitings(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getOnlines(),
                    src.getOfflines(),
                    vjobs);
            Assert.assertEquals(plan.size(), 0);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    public void testViableWithoutConstraints() {
        //BasicConfigurator.configure();
        CustomizablePlannerModule planner = makeModule();
        Configuration src = TestHelper.readConfiguration(RESOURCES_DIR + "simple.txt");
        List<VJob> vjobs = new ArrayList<VJob>();
        PlasmaVJob v = new BasicPlasmaVJob("v1");
        v.addVirtualMachines(src.getRunnings());
        MockVirtualMachineBuilder builder = new MockVirtualMachineBuilder();
        vjobs.add(v);
        try {
            TimedReconfigurationPlan plan = planner.compute(src,
                    src.getRunnings(),
                    src.getWaitings(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getOnlines(),
                    src.getOfflines(),
                    vjobs);
            //System.out.println(plan);
            Assert.assertEquals(plan.size(), 0);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    public void testWithSequences() {
        ChocoCustomRP planner = makeModule();
        planner.setRepairMode(false); //required to have a solution
        Configuration src = TestHelper.readConfiguration(RESOURCES_DIR + "sequencing_src.txt");
        Configuration dst = TestHelper.readConfiguration(RESOURCES_DIR + "sequencing_dst.txt");
        List<VJob> vjobs = new ArrayList<VJob>();
        VJob v = new BasicPlasmaVJob("v1");
        v.addVirtualMachines(src.getRunnings());
        MockVirtualMachineBuilder builder = new MockVirtualMachineBuilder();
        vjobs.add(v);
        try {
            TimedReconfigurationPlan plan = planner.compute(src,
                    dst.getRunnings(),
                    dst.getWaitings(),
                    dst.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    dst.getOnlines(),
                    dst.getOfflines(),
                    vjobs);
            Assert.assertEquals(plan.size(), 5);
            Shutdown s = null;
            Migration m1 = null, m2 = null, m3 = null, m4 = null;
            for (Action a : plan.getActions()) {
                if (a instanceof Shutdown) {
                    s = (Shutdown) a;
                } else if (a instanceof Migration) {
                    Migration m = (Migration) a;
                    if (m.getVirtualMachine().getName().equals("VM1")) m1 = m;
                    else if (m.getVirtualMachine().getName().equals("VM2")) m2 = m;
                    else if (m.getVirtualMachine().getName().equals("VM3")) m3 = m;
                    else if (m.getVirtualMachine().getName().equals("VM4")) m4 = m;
                }
            }
            Assert.assertTrue(s.getStartMoment() >= m1.getFinishMoment());
            Assert.assertTrue(s.getStartMoment() >= m2.getFinishMoment());
            Assert.assertTrue(m1.getStartMoment() >= m3.getFinishMoment());
            Assert.assertTrue(m2.getStartMoment() >= m4.getFinishMoment());

        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    public void testNonViableWithoutConstraints() {
        CustomizablePlannerModule planner = makeModule();
        Configuration src = TestHelper.readConfiguration(RESOURCES_DIR + "nonViable.txt");
        //System.err.println(src.getCurrentlyOverloadedNodes());     
        List<VJob> vjobs = new ArrayList<VJob>();
        VJob v = new DefaultVJob("v1");
        v.addVirtualMachines(src.getRunnings());
        MockVirtualMachineBuilder builder = new MockVirtualMachineBuilder();
        vjobs.add(v);
        try {
            TimedReconfigurationPlan plan = planner.compute(src,
                    src.getRunnings(),
                    src.getWaitings(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getOnlines(),
                    src.getOfflines(),
                    vjobs);
            System.out.println(plan);
            Assert.assertEquals(plan.getDuration(), 5);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    /**
     * Test solving with basic constraints.
     */
    public void testWithSatisfiableConstraints() {
        CustomizablePlannerModule planner = makeModule();
        Configuration src = TestHelper.readConfiguration(RESOURCES_DIR + "nonViable.txt");
        List<VJob> vjobs = new ArrayList<VJob>();
        VJob v = new DefaultVJob("v1");
        v.addVirtualMachines(src.getRunnings());
        try {
            v.addConstraint(new LazySpread(v.getVirtualMachines()));
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
        vjobs.add(v);
        try {
            TimedReconfigurationPlan plan = planner.compute(src,
                    src.getRunnings(),
                    src.getWaitings(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getOnlines(),
                    src.getOfflines(),
                    vjobs);
            System.err.println(plan);
            Assert.assertEquals(plan.size(), 3);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    public void testWithBootAndShutdownActions() {
        ChocoCustomRP planner = makeModule();
        //Check solving statistics
        Assert.assertTrue(planner.getSolvingStatistics() == SolvingStatistics.getStatisticsForNotSolvingProcess());
        Assert.assertEquals(planner.getSolutionsStatistics().size(), 0);

        planner.setRepairMode(false);
        Configuration src = TestHelper.readConfiguration(RESOURCES_DIR + "nonViable.txt");
        src.addOffline(src.getOnlines().get("N9"));
        List<VJob> vjobs = new ArrayList<VJob>();

        ManagedElementSet<Node> offs = new SimpleManagedElementSet<Node>();
        ManagedElementSet<Node> ons = new SimpleManagedElementSet<Node>();
        ons.addAll(src.getOnlines());

        ons.remove(src.getOnlines().get("N3"));
        offs.add(src.getOnlines().get("N3"));

        ons.add(src.getAllNodes().get("N9"));

        PlasmaVJob v = new BasicPlasmaVJob("v1");
        v.addVirtualMachines(src.getRunnings());
        vjobs.add(v);
        try {
            TimedReconfigurationPlan plan = planner.compute(src,
                    src.getRunnings(),
                    src.getWaitings(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    ons,
                    offs,
                    vjobs);
            System.err.println(plan);
            Assert.assertEquals(plan.getActions().size(), 6);

        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
        SolvingStatistics st = planner.getSolvingStatistics();
        List<SolutionStatistics> sols = planner.getSolutionsStatistics();

        //Every solutions, asc sorted wrt. solving duration
        Assert.assertTrue(sols.size() >= 1);
        for (int i = 1; i < sols.size(); i++) {
            SolutionStatistics s2 = sols.get(i);
            SolutionStatistics s1 = sols.get(i - 1);
            Assert.assertTrue(s2.getNbBacktracks() >= s1.getNbBacktracks() &&
                    s2.getNbNodes() >= s1.getNbNodes() &&
                    s2.getTimeCount() >= s1.getTimeCount() &&
                    s1.getObjective() > s2.getObjective());
        }
        //Solvin statistics at least equals to the last solution
        SolutionStatistics s = sols.get(sols.size() - 1);
        st = planner.getSolvingStatistics();
        Assert.assertTrue(st.getNbBacktracks() >= s.getNbBacktracks() &&
                st.getNbNodes() >= s.getNbNodes() &&
                st.getTimeCount() >= s.getTimeCount());


    }

    /**
     * Solve a plan with some slice with no requirements.
     */
    public void testWithNoRequirements() {
        CustomizablePlannerModule planner = makeModule();
        Configuration src = TestHelper.readConfiguration(RESOURCES_DIR + "noRequirements.txt");
        List<VJob> vjobs = new ArrayList<VJob>();
        VJob v = new DefaultVJob("v1");
        v.addVirtualMachines(src.getRunnings());
        MockVirtualMachineBuilder builder = new MockVirtualMachineBuilder();
        vjobs.add(v);
        try {
            TimedReconfigurationPlan plan = planner.compute(src,
                    src.getRunnings(),
                    src.getWaitings(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getOnlines(),
                    src.getOfflines(),
                    vjobs);
            Assert.assertEquals(plan.size(), 0);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    public void testCosts() {
        CustomizablePlannerModule planner = makeModule();
        Configuration src = TestHelper.readConfiguration(RESOURCES_DIR + "testCost.txt");
        List<VJob> vjobs = new ArrayList<VJob>();
        VJob v = new DefaultVJob("v1");
        v.addVirtualMachines(src.getRunnings());
        MockVirtualMachineBuilder builder = new MockVirtualMachineBuilder();
        vjobs.add(v);
        try {
            TimedReconfigurationPlan plan = planner.compute(src,
                    src.getRunnings(),
                    src.getWaitings(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getOnlines(),
                    src.getOfflines(),
                    vjobs);
            System.out.println(plan);
            Assert.assertEquals(plan.size(), 1);
            Assert.assertEquals(plan.getDuration(), 5);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

/*    public void dummy() {
        ChocoCustomRP planner = makeModule();
        Configuration src = TestHelper.readConfiguration(RESOURCES_DIR + "multipleOneOf_cfg.txt");
        try {
            VirtualMachineTemplateFactory f = new MockVirtualMachineTemplateFactory();
            PlasmaVJobBuilder b = new PlasmaVJobBuilder(new DefaultVJobElementBuilder(f), new ConstraintsCatalogBuilderFromProperties(new PropertiesHelper("plasmaVJobs.properties")).build());
            b.getElementBuilder().useConfiguration(src);
            VJob v = b.build("m", new File(RESOURCES_DIR + "multipleOneOf.txt"));
            List<VJob> vjobs = new ArrayList<VJob>();
            vjobs.add(v);
            TimedReconfigurationPlan plan = planner.compute(src,
                    src.getRunnings(),
                    src.getWaitings(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getOnlines(),
                    src.getOfflines(),
                    vjobs);
            System.out.println(plan);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            ChocoLogging.flushLogs();
        }
    }     */

    public void testMigrateThenRun() {
        Node n1 = new SimpleNode("N1", 5, 5, 5);
        Node n2 = new SimpleNode("N2", 4, 4, 4);
        Node n3 = new SimpleNode("N3", 4, 4, 4);

        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 2, 2, 2);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 2, 5, 5);
        Configuration src = new SimpleConfiguration();
        src.addOnline(n1);
        src.addOnline(n2);
        src.addOnline(n3);
        src.setRunOn(vm1, n1);
        src.addWaiting(vm2);
        ChocoCustomRP planner = makeModule();

        try {
            VirtualMachineTemplateFactory f = new MockVirtualMachineTemplateFactory();
            PlasmaVJobBuilder b = new PlasmaVJobBuilder(new DefaultVJobElementBuilder(f), new ConstraintsCatalogBuilderFromProperties(new PropertiesHelper("config/plasmaVJobs.properties")).build());
            b.getElementBuilder().useConfiguration(src);
            VJob v = new DefaultVJob("V1");
            List<VJob> vjobs = new ArrayList<VJob>();
            vjobs.add(v);
            planner.setRepairMode(false);
            TimedReconfigurationPlan plan = planner.compute(src,
                    src.getAllVirtualMachines(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getOnlines(),
                    src.getOfflines(),
                    vjobs);
            System.out.println(plan);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            ChocoLogging.flushLogs();
        }
    }

    /**
     * Test a reconfiguration with VMs that can be either migrated to reinstantiated.
     * 2 identical VMs can be moved to get a viable configuration (from a resource usage pov).
     * However 1 is volatile and moved this one leads to the best possible reconfiguration plan
     */
    public void testWithClonedVMs() {
        Configuration cfg = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 5, 5, 5);
        Node n2 = new SimpleNode("N2", 5, 5, 5);
        cfg.addOnline(n1);
        cfg.addOnline(n2);

        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 2, 2);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 2, 2);

        cfg.setRunOn(vm1, n1);
        cfg.setRunOn(vm2, n1);
        vm1.setCPUDemand(4);
        vm2.setCPUDemand(4);

        vm1.addOption("clone");
        vm1.addOption("boot", "1");
        vm1.addOption("halt", "2");
        vm2.addOption("halt", "10");

        ChocoCustomRP planner = makeModule();
        try {
            VirtualMachineTemplateFactory f = new MockVirtualMachineTemplateFactory();
            PlasmaVJobBuilder b = new PlasmaVJobBuilder(new DefaultVJobElementBuilder(f), new ConstraintsCatalogBuilderFromProperties(new PropertiesHelper("config/plasmaVJobs.properties")).build());
            b.getElementBuilder().useConfiguration(cfg);
            planner.setRepairMode(false);
            TimedReconfigurationPlan plan = planner.compute(cfg,
                    cfg.getAllVirtualMachines(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    cfg.getOnlines(),
                    cfg.getOfflines(),
                    new ArrayList<VJob>());
            System.out.println(plan);
            System.out.println(plan.getDestination());
            Assert.assertEquals(plan.getDestination().getLocation(vm1), n2);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        } finally {
            ChocoLogging.flushLogs();
        }
    }


    /**
     * Unit test with a configuration having a manageable offline node
     * that is required to be online to solve the instance.
     */
    public void testWithBootableNodeAndRequiredForVMs() {
        Configuration src = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 3, 3, 3);
        Node n2 = new SimpleNode("N2", 3, 3, 3);
        src.addOnline(n1);
        src.addOffline(n2);
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 2);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 2);
        src.addWaiting(vm1);
        src.setRunOn(vm2, src.getOnlines().get("N1"));
        ManagedElementSet<Node> other = new SimpleManagedElementSet<Node>();

        try {
            ChocoCustomRP planner = makeModule();
            TimedReconfigurationPlan p = planner.compute(src,
                    src.getAllVirtualMachines(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getOnlines(),
                    other,
                    new ArrayList<VJob>());
            System.err.println(p);
            Assert.assertEquals(p.getDuration(), 3);
            Configuration cfg = p.getDestination();
            Assert.assertTrue(cfg.isOnline(n1));
            Assert.assertTrue(cfg.isOnline(n2));
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    /**
     * Unit test with a configuration having a manageable offline node
     * that is not required to be online to solve the instance.
     */
    public void testWithBootableNodeNotRequired() {
        Configuration src = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 3, 3, 4);
        Node n2 = new SimpleNode("N2", 3, 3, 4);
        src.addOnline(n1);
        src.addOffline(n2);
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 1);
        src.addWaiting(vm1);
        src.setRunOn(vm2, src.getOnlines().get("N1"));
        ManagedElementSet<Node> other = new SimpleManagedElementSet<Node>();
        List<VJob> vjobs = new ArrayList<VJob>();
        VJob v = new DefaultVJob("v1");
        vjobs.add(v);
        try {

            ChocoCustomRP planner = makeModule();
            planner.setRepairMode(false);
            TimedReconfigurationPlan p = planner.compute(src,
                    src.getAllVirtualMachines(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getOnlines(),
                    other,
                    vjobs);
            System.err.println(p);
            Assert.assertEquals(p.getDuration(), 1);
            Configuration cfg = p.getDestination();
            Assert.assertTrue(cfg.isOnline(n1));
            Assert.assertTrue(cfg.isOffline(n2));
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }


    public void test3() {
        //ChocoLogging.setVerbosity(Verbosity.SOLUTION);
        ManagedElementSet<Node> ns = new SimpleManagedElementSet<Node>();
        for (int i = 0; i < 3; i++) {
            Node n = new SimpleNode("N" + i, 2, 200, 100000);
            ns.add(n);
        }

        ManagedElementSet<VirtualMachine> vms = new SimpleManagedElementSet<VirtualMachine>();
        for (int j = 0; j < ns.size(); j++) {
            VirtualMachine vm = new SimpleVirtualMachine("VM" + j, 2, 50, 1024);
            vms.add(vm);
        }

        Configuration src = new SimpleConfiguration();
        for (Node n : ns) {
            src.addOnline(n);
        }
        src.setRunOn(vms.get(0), ns.get(0));
        src.setRunOn(vms.get(1), ns.get(1));
        src.setRunOn(vms.get(2), ns.get(2));

        List<VJob> vjobs = new ArrayList<VJob>();
        VJob v = new DefaultVJob("v1");
        vjobs.add(v);
        Fence f = new Fence(vms, new SimpleManagedElementSet<Node>(ns.get(1)));
        v.addConstraint(f);
        try {

            ChocoCustomRP planner = makeModule();
            //planner.doOptimize(false);
            planner.setRepairMode(false);
            TimedReconfigurationPlan p = planner.compute(src,
                    src.getAllVirtualMachines(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<Node>(),
                    new SimpleManagedElementSet<Node>(),
                    vjobs);
            System.out.println(p);

            Assert.assertEquals(p.getDestination().getRunnings(ns.get(1)).size(), 3);
            Assert.assertEquals(p.getDuration(), 5);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    public void testSchedule() {

        Configuration src = new SimpleConfiguration();
        Node n1 = new SimpleNode("N1", 1, 3, 3);
        Node n2 = new SimpleNode("N2", 1, 3, 3);
        Node n3 = new SimpleNode("N3", 1, 3, 3);
        src.addOnline(n1);
        src.addOnline(n2);
        src.addOnline(n3);

        int i = 0;
        for (int j = 0; j < 8; j++) {
            VirtualMachine vm = new SimpleVirtualMachine("VM" + j, 1, 1, 1);
            src.setRunOn(vm, src.getAllNodes().get(++i % 3));
        }
        System.out.println(src);
        //Shift every VM.
        VJob v = new DefaultVJob("v1");
        v.addConstraint(new Fence(src.getRunnings(n1), new SimpleManagedElementSet<Node>(n2)));
        v.addConstraint(new Fence(src.getRunnings(n2), new SimpleManagedElementSet<Node>(n3)));
        v.addConstraint(new Fence(src.getRunnings(n3), new SimpleManagedElementSet<Node>(n1)));
        List<VJob> l = new ArrayList<VJob>();
        l.add(v);
        try {
            ChocoCustomRP planner = makeModule();
            planner.setRepairMode(false);
            planner.setTimeLimit(0);
            planner.doOptimize(false);
            ChocoLogging.setVerbosity(Verbosity.SOLUTION);
            TimedReconfigurationPlan p = planner.compute(src,
                    src.getAllVirtualMachines(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    src.getSleepings(),
                    new SimpleManagedElementSet<VirtualMachine>(),
                    new SimpleManagedElementSet<Node>(),
                    new SimpleManagedElementSet<Node>(),
                    l);
            System.err.println(p);
            System.err.println(p.getDestination());
            System.err.flush();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }


}
