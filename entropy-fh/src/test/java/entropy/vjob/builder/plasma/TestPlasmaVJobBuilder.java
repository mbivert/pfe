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

package entropy.vjob.builder.plasma;

import entropy.configuration.DefaultConfiguration;
import entropy.configuration.DefaultNode;
import entropy.configuration.DefaultVirtualMachine;
import entropy.template.MockVirtualMachineTemplateFactory;
import entropy.template.VirtualMachineTemplateFactory;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.VJobBuilderException;
import entropy.vjob.builder.VJobElementBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * Unit tests for VJobBuilder.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestPlasmaVJobBuilder {

    private static final String RESOURCES_ROOT = "src/test/resources/entropy/vjob/builder/plasma/TestVJobBuilder.";

    /**
     * Make a default VJobBuilder. Configuration is based on nodes in the pastel cluster.
     * 50 VMs are available
     * constraint Ban, OneOf and lSpread in the catalog.
     */
    private PlasmaVJobBuilder makeBuilder() {
        VirtualMachineTemplateFactory f = new MockVirtualMachineTemplateFactory();
        VJobElementBuilder eb = new DefaultVJobElementBuilder(f);
        DefaultConstraintsCatalog c = new DefaultConstraintsCatalog();
        DefaultConfiguration cfg = new DefaultConfiguration();
        for (int i = 1; i <= 20; i++) {
            cfg.addOnline(new DefaultNode("pastel-" + i + ".b217.home", 1, 1, 1));
        }
        for (int i = 1; i <= 3; i++) {
            cfg.addOnline(new DefaultNode("pastel-nfs" + i + ".b217.home", 1, 1, 1));
        }
        cfg.addOnline(new DefaultNode("pastel-frontend.b217.home", 1, 1, 1));

        //Add some virtual machines
        for (int i = 1; i <= 50; i++) {
            cfg.addWaiting(new DefaultVirtualMachine("VM" + i, 1, 1, 1));
        }
        eb.useConfiguration(cfg);
        c.add(new BanBuilder());
        c.add(new AmongBuilder());
        c.add(new LazySpreadBuilder());
        PlasmaVJobBuilder b = new PlasmaVJobBuilder(eb, c);
        return b;
    }

    /**
     * Test the parsing of pastel infrastructure
     */
    public void testPastelParsing() {
        PlasmaVJobBuilder b = makeBuilder();
        try {
            PlasmaVJob v = b.build("pastel", new File(RESOURCES_ROOT + "pastel.txt"));
            Assert.assertEquals(v.id(), "pastel");
            Assert.assertEquals(v.getNodes().size(), 24);
            Assert.assertEquals(v.getNodeSet("$WORKERS").size(), 20);
            Assert.assertEquals(v.getNodeSet("$SERVICES").size(), 4);
            for (int i = 1; i <= 4; i++) {
                Assert.assertEquals(v.getNodeSet("$R" + i).size(), 6);
            }
        } catch (VJobBuilderException e) {
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test the acceptation of an empty VJob.
     */
    public void testEmptyVJob() {
        PlasmaVJobBuilder b = makeBuilder();
        try {
            PlasmaVJob v = b.build("empty", new File(RESOURCES_ROOT + "empty.txt"));
            Assert.assertEquals(v.id(), "empty");
            Assert.assertEquals(v.getNodes().size(), 0);
            Assert.assertEquals(v.getVirtualMachines().size(), 0);
        } catch (VJobBuilderException e) {
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test the usage of an unknown variable $R5.
     *
     * @throws VJobBuilderException
     */
    @Test(expectedExceptions = {VJobBuilderException.class})
    public void testWithUndefVariable() throws VJobBuilderException {
        PlasmaVJobBuilder b = makeBuilder();
        try {
            b.build("pastel", new File(RESOURCES_ROOT + "pastelR5.txt"));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test the detection of a variable redefinition.
     *
     * @throws VJobBuilderException
     */
    @Test(expectedExceptions = {VJobBuilderException.class})
    public void testVariableRedef() throws VJobBuilderException {
        PlasmaVJobBuilder b = makeBuilder();
        try {
            b.build("pastel", new File(RESOURCES_ROOT + "pastelRedef.txt"));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test the detection of a constraint not in a catalog.
     *
     * @throws VJobBuilderException
     */
    @Test(expectedExceptions = {VJobBuilderException.class})
    public void testWithUnknownConstraint() throws VJobBuilderException {
        PlasmaVJobBuilder b = makeBuilder();
        try {
            b.build("pastel", new File(RESOURCES_ROOT + "UnknownConstraint.txt"));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test the addition between a set of virtual machines and a set of nodes
     */
    @Test(expectedExceptions = {VJobBuilderException.class})
    public void testTypeMismatch() throws VJobBuilderException {
        PlasmaVJobBuilder b = makeBuilder();
        try {
            b.build("pastel", new File(RESOURCES_ROOT + "typeMismatch.txt"));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

    }

    /**
     * A admin vjob ban the service nodes.
     */
    public void testWithProlog() {
        PlasmaVJobBuilder b = makeBuilder();
        try {
            PlasmaVJob infra = b.build("pastel", new File(RESOURCES_ROOT + "pastel.txt"));
            b.setProlog(infra);
            PlasmaVJob admin = b.build("admin", new File(RESOURCES_ROOT + "admin_vjob.txt"));
            Assert.assertEquals(admin.getVirtualMachines().size(), 0);
            Assert.assertEquals(admin.getNodes().size(), 0);
            Assert.assertEquals(admin.getConstraints().size(), 1);
        } catch (VJobBuilderException e) {
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test with a VJob simulating a 3-tier web application.
     */
    public void testWebAppVJob() {
        PlasmaVJobBuilder b = makeBuilder();
        try {
            PlasmaVJob infra = b.build("pastel", new File(RESOURCES_ROOT + "pastel.txt"));
            b.setProlog(infra);
            PlasmaVJob webapp = b.build("webapp", new File(RESOURCES_ROOT + "webapp.txt"));
            Assert.assertEquals(webapp.getVirtualMachines().size(), 17);
            Assert.assertEquals(webapp.getNodes().size(), 0);
            Assert.assertEquals(webapp.getConstraints().size(), 4);
        } catch (VJobBuilderException e) {
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Read a vjob containing various usage of range of elements.
     */
    public void testVariousExploded() {
        PlasmaVJobBuilder b = makeBuilder();
        try {
            PlasmaVJob infra = b.build("pastel", new File(RESOURCES_ROOT + "exploded.txt"));
            Assert.assertEquals(infra.getVirtualMachines().size(), 17);
        } catch (VJobBuilderException e) {
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test to check if integers can be passed as argument.
     */
    public void testWithCapacityConstraint() {
        PlasmaVJobBuilder b = makeBuilder();
        DefaultConstraintsCatalog c = (DefaultConstraintsCatalog) b.getCatalog();
        c.add(new CapacityBuilder());
        try {
            PlasmaVJob infra = b.build("pastel", new File(RESOURCES_ROOT + "capacity.txt"));
            System.err.println(infra);
            System.err.flush();
            //Assert.assertEquals(infra.getVirtualMachines().size(), 17);
        } catch (VJobBuilderException e) {
            Assert.fail(e.getMessage(), e);
        } catch (IOException e) {
            Assert.fail(e.getMessage(), e);
        }
    }
}
