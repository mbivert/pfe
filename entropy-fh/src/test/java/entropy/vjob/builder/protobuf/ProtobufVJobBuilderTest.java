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

package entropy.vjob.builder.protobuf;

import entropy.configuration.*;
import entropy.vjob.*;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.VJobBuilderException;
import entropy.vjob.builder.VJobElementBuilder;
import entropy.vjob.builder.plasma.BasicPlasmaVJob;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class ProtobufVJobBuilderTest {

    private static final String RESOURCE = "src/test/resources/entropy/vjob/builder/protobuf/ProtobufVJobBuilder.test.pbd";

    public void testGood() {

    }

    public void testBadTemplate() {

    }

    /**
     * The VM is not known and can not be instantiated.
     *
     * @throws VJobBuilderException
     */
    @Test(expectedExceptions = {VJobBuilderException.class})
    public void testUnknownVM() throws VJobBuilderException {
        Configuration cfg = makeConfiguration();
        //make(cfg);
        cfg.remove(cfg.getAllVirtualMachines().get("vappHA.VM10"));
        VJobElementBuilder eB = new DefaultVJobElementBuilder(null);
        eB.useConfiguration(cfg);
        ProtobufVJobBuilder builder = new ProtobufVJobBuilder(eB);
        DefaultPBConstraintsCatalog c = new DefaultPBConstraintsCatalog();
        c.add(new AmongBuilder());
        c.add(new LonelyBuilder());
        c.add(new BanBuilder());
        c.add(new CapacityBuilder());
        c.add(new ContinuousSpreadBuilder());
        builder.setConstraintCatalog(c);

        try {
            VJob v = builder.build(new File(RESOURCE));
            System.out.println(v);
        } catch (IOException e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    /**
     * The node is not known
     *
     * @throws VJobBuilderException
     */
    @Test(expectedExceptions = {VJobBuilderException.class})
    public void testUnknownNode() throws VJobBuilderException {
        Configuration cfg = makeConfiguration();
        //make(cfg);
        cfg.remove(cfg.getAllNodes().get("node-4"));
        VJobElementBuilder eB = new DefaultVJobElementBuilder(null);
        eB.useConfiguration(cfg);
        ProtobufVJobBuilder builder = new ProtobufVJobBuilder(eB);
        DefaultPBConstraintsCatalog c = new DefaultPBConstraintsCatalog();
        c.add(new AmongBuilder());
        c.add(new LonelyBuilder());
        c.add(new BanBuilder());
        c.add(new CapacityBuilder());
        c.add(new ContinuousSpreadBuilder());
        builder.setConstraintCatalog(c);

        try {
            VJob v = builder.build(new File(RESOURCE));
            System.out.println(v);
        } catch (IOException e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    private Configuration makeConfiguration() {
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i <= 20; i++) {
            VirtualMachine vm = new SimpleVirtualMachine("vappHA.VM" + i);
            cfg.addWaiting(vm);
            if (i % 2 == 0) {
                vm.setTemplate("foo");
            } else {
                vm.setTemplate("bar");
            }
        }
        VirtualMachine vmX = new SimpleVirtualMachine("vappHA.top");
        vmX.setTemplate("foo");
        cfg.addWaiting(vmX);
        VirtualMachine vmY = new SimpleVirtualMachine("vappHA.middle");
        vmX.setTemplate("ttt");
        cfg.addWaiting(vmY);

        for (int i = 1; i <= 20; i++) {
            Node n = new SimpleNode("node-" + i);
            cfg.addOnline(n);
        }
        cfg.addOnline(new SimpleNode("node-frontend"));
        return cfg;
    }

    private void make(Configuration cfg) {

        VJob v = new BasicPlasmaVJob("vappHA");

        ManagedElementSet<VirtualMachine> t1 = new SimpleManagedElementSet<VirtualMachine>();
        t1.add(cfg.getAllVirtualMachines().get("vappHA.VM1"));
        t1.add(cfg.getAllVirtualMachines().get("vappHA.VM2"));
        t1.add(cfg.getAllVirtualMachines().get("vappHA.VM3"));
        v.addConstraint(new ContinuousSpread(t1));
        v.addVirtualMachine(cfg.getAllVirtualMachines().get("vappHA.VM10"));
        v.addVirtualMachine(cfg.getAllVirtualMachines().get("vappHA.top"));
        v.addVirtualMachine(cfg.getAllVirtualMachines().get("vappHA.middle"));
        ManagedElementSet<Node> ns = new SimpleManagedElementSet<Node>();
        ns.add(cfg.getOnlines().get("node-1"));
        ns.add(cfg.getOnlines().get("node-2"));
        v.addConstraint(new Capacity(ns, 15));

        ManagedElementSet<Node> ns2 = new SimpleManagedElementSet<Node>();
        ns2.add(cfg.getOnlines().get("node-3"));
        ns2.add(cfg.getOnlines().get("node-4"));
        Set<ManagedElementSet<Node>> x = new HashSet<ManagedElementSet<Node>>();
        x.add(ns);
        x.add(ns2);
        v.addConstraint(new Among(t1, x));
        v.addConstraint(new Ban(t1, ns2));
        try {
            ProtobufVJobSerializer.getInstance().write(v, RESOURCE);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }

    }
}
