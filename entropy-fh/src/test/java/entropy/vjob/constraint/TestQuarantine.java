package entropy.vjob.constraint;

import entropy.configuration.*;
import entropy.plan.PlanException;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.choco.ChocoCustomRP;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSlicesHeightsFastBP;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.template.MockVirtualMachineTemplateFactory;
import entropy.vjob.DefaultVJob;
import entropy.vjob.Quarantine;
import entropy.vjob.VJob;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.plasma.BasicPlasmaVJob;
import entropy.vjob.builder.plasma.PlasmaVJob;
import entropy.vjob.builder.protobuf.DefaultPBConstraintsCatalog;
import entropy.vjob.builder.protobuf.ProtobufVJobBuilder;
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.protobuf.QuarantineBuilder;
import entropy.vjob.builder.xml.DefaultXMLConstraintsCatalog;
import entropy.vjob.builder.xml.XMLVJobBuilder;
import entropy.vjob.builder.xml.XmlVJobSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link entropy.vjob.Quarantine}
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestQuarantine {

    /**
     * Test instantiation, getters, hashCode and equals
     */
    public void testBasics() {
        ManagedElementSet<Node> ns = new SimpleManagedElementSet<Node>();
        ns.add(new SimpleNode("N1", 1, 2, 3));
        ns.add(new SimpleNode("N2", 1, 2, 3));
        Quarantine q = new Quarantine(ns);
        Assert.assertFalse(q.toString().contains("null"));
        Assert.assertEquals(q.getNodes(), ns);
        Assert.assertEquals(q.getAllVirtualMachines().size(), 0);
        Assert.assertEquals(q.getMisPlaced(new SimpleConfiguration()).size(), 0);
        Assert.assertTrue(q.isSatisfied(new SimpleConfiguration()));

        Quarantine q2 = new Quarantine(ns);
        Assert.assertEquals(q, q2);
        Assert.assertEquals(q.hashCode(), q2.hashCode());
        ManagedElementSet<Node> ns2 = ns.clone();
        ns2.remove(ns2.get("N2"));
        q2 = new Quarantine(ns2);
        Assert.assertNotEquals(q, q2);
        Assert.assertNotEquals(q.hashCode(), q2.hashCode());
    }

    /**
     * Test with problem that can be solved iff one VM enter the quarantine area.
     * So hope it will fail
     */
    @Test(expectedExceptions = {PlanException.class})
    public void testNoIn() throws PlanException {
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i < 5; i++) {
            Node n = new SimpleNode("N" + i, 1, i, i);
            cfg.addOnline(n);
        }
        cfg.addOnline(new SimpleNode("N5", 1, 5, 5));
        ManagedElementSet<Node> ns = cfg.getAllNodes().clone(); //ns = N[3..5]
        ns.remove(ns.get("N1"));
        ns.remove(ns.get("N2"));
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 1);
        VirtualMachine vm3 = new SimpleVirtualMachine("VM3", 1, 1, 1);
        vm2.setCPUDemand(3);
        cfg.setRunOn(vm1, ns.get("N3"));
        cfg.setRunOn(vm2, cfg.getAllNodes().get("N1"));
        cfg.setSleepOn(vm3, ns.get("N4"));

        ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 1, 2, 3, 4, 5, 6, 7, 8));
        List<VJob> vjobs = new ArrayList<VJob>();
        PlasmaVJob v = new BasicPlasmaVJob("v1");
        v.addConstraint(new Quarantine(ns));
        vjobs.add(v);
        TimedReconfigurationPlan p = plan.compute(cfg,
                cfg.getRunnings(),
                cfg.getWaitings(),
                cfg.getSleepings(),
                new SimpleManagedElementSet<VirtualMachine>(),
                cfg.getOnlines(),
                cfg.getOfflines(),
                vjobs);
        System.err.println(p);
    }

    /**
     * Test with problem that can be solved iff one VM leave the quarantine area.
     * So hope it will fail
     */
    @Test(expectedExceptions = {PlanException.class})
    public void testNoOut() throws PlanException {
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i < 5; i++) {
            Node n = new SimpleNode("N" + i, 1, 5 - i, 5 - i);
            cfg.addOnline(n);
        }
        cfg.addOffline(new SimpleNode("N5", 1, 5, 5));
        ManagedElementSet<Node> ns = cfg.getAllNodes().clone(); //ns = N[3..5]
        ns.remove(ns.get("N1"));
        ns.remove(ns.get("N2"));
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 1, 1);
        VirtualMachine vm2 = new SimpleVirtualMachine("VM2", 1, 1, 1);
        VirtualMachine vm3 = new SimpleVirtualMachine("VM3", 1, 1, 1);
        vm1.setCPUDemand(4);
        cfg.setRunOn(vm1, ns.get("N3"));
        cfg.setRunOn(vm2, cfg.getAllNodes().get("N2"));
        cfg.setSleepOn(vm3, ns.get("N4"));


        ChocoCustomRP plan = new ChocoCustomRP(new MockDurationEvaluator(9, 1, 2, 3, 4, 5, 6, 7, 8));
        plan.setPackingConstraintClass(new SatisfyDemandingSlicesHeightsFastBP());
        List<VJob> vjobs = new ArrayList<VJob>();
        PlasmaVJob v = new BasicPlasmaVJob("v1");
        v.addConstraint(new Quarantine(ns));
        vjobs.add(v);
        TimedReconfigurationPlan p = plan.compute(cfg,
                cfg.getRunnings(),
                cfg.getWaitings(),
                cfg.getSleepings(),
                new SimpleManagedElementSet<VirtualMachine>(),
                cfg.getOnlines(),
                cfg.getOfflines(),
                vjobs);
        System.err.println(p);
    }

    /**
     * Test if protobuf serialization if fine by doing a cycle serialization/deserialization
     */
    public void testProtobufSerialization() {
        ManagedElementSet<Node> ns = new SimpleManagedElementSet<Node>();
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i < 5; i++) {
            Node n = new SimpleNode("N" + i, 1, 5 - i, 5 - i);
            ns.add(n);
            cfg.addOnline(n);
        }
        Quarantine q1 = new Quarantine(ns);
        QuarantineBuilder qb = new QuarantineBuilder();
        VJob v = new DefaultVJob("V1");
        v.addConstraint(q1);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            ProtobufVJobSerializer.getInstance().write(v, out.getPath());
            ProtobufVJobBuilder vb = new ProtobufVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null));
            vb.getElementBuilder().useConfiguration(cfg);
            DefaultPBConstraintsCatalog cat = new DefaultPBConstraintsCatalog();
            cat.add(qb);
            vb.setConstraintCatalog(cat);
            VJob v2 = vb.build(out);
            Quarantine q2 = (Quarantine) v2.getConstraints().iterator().next();
            Assert.assertEquals(q1, q2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test if xml serialization if fine by doing a cycle serialization/deserialization
     */
    public void testXMLSerialization() {
        ManagedElementSet<Node> ns = new SimpleManagedElementSet<Node>();
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i < 5; i++) {
            Node n = new SimpleNode("N" + i, 1, 5 - i, 5 - i);
            ns.add(n);
            cfg.addOnline(n);
        }
        Quarantine q1 = new Quarantine(ns);
        entropy.vjob.builder.xml.QuarantineBuilder qb = new entropy.vjob.builder.xml.QuarantineBuilder();
        VJob v = new DefaultVJob("V1");
        v.addConstraint(q1);
        try {
            File out = File.createTempFile("vjob", "pbd");
            out.deleteOnExit();
            DefaultXMLConstraintsCatalog cat = new DefaultXMLConstraintsCatalog();
            cat.add(qb);
            XmlVJobSerializer.getInstance().write(v, out.getPath());
            XMLVJobBuilder vb = new XMLVJobBuilder(new DefaultVJobElementBuilder(new MockVirtualMachineTemplateFactory(), null), cat);
            vb.getElementBuilder().useConfiguration(cfg);
            VJob v2 = vb.build(out);
            Quarantine q2 = (Quarantine) v2.getConstraints().iterator().next();
            Assert.assertEquals(q1, q2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
