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
package entropy.template.xen;

import entropy.MissingRequiredPropertyException;
import entropy.configuration.VirtualMachine;
import entropy.template.VirtualMachineBuilderException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;

/**
 * Some unit tests for XenVirtualMachineBuilder.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestXenVirtualMachineBuilder {


    private static final String CONFIG_LOCATION = "src/test/resources/entropy/template/xen/xenTemplate.properties";

    /**
     * Test the instantation.
     */
    public void testInstantiation() throws Exception {
        XenVirtualMachineBuilder b = new XenVirtualMachineBuilder(CONFIG_LOCATION);
        Assert.assertEquals(b.getConfigDir(), "src/test/resources/entropy/template/xen/xenCfgs");
    }

    public void testDistribution() throws Exception {
        XenVirtualMachineBuilder b = new XenVirtualMachineBuilder();
        Assert.assertEquals(b.getConfigDir(), "/VMs");
    }

    /**
     * Test the parsing of a valid file.
     */
    public void testGoodRetrieve() {
        try {
            XenVirtualMachineBuilder b = new XenVirtualMachineBuilder(CONFIG_LOCATION);
            VirtualMachine vm = b.build("vm1", new HashMap<String, String>());
            Assert.assertEquals(vm.getName(), "vm1");
            Assert.assertEquals(vm.getMemoryDemand(), 1024);
            Assert.assertEquals(vm.getNbOfCPUs(), 4);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    /**
     * Test the parsing of a file without VCPU (by default = 1).
     */
    public void testRetrieveWithoutVCPU() {

        try {
            XenVirtualMachineBuilder b = new XenVirtualMachineBuilder(CONFIG_LOCATION);
            VirtualMachine vm = b.build("noVCPU", new HashMap<String, String>());
            Assert.assertEquals(vm.getMemoryDemand(), 2048);
            Assert.assertEquals(vm.getNbOfCPUs(), 1);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }

    /**
     * Test the parsing without memory.
     *
     * @throws entropy.vjob.builder.VJobBuilderException
     *          the exception we expect
     */
    @Test(expectedExceptions = {VirtualMachineBuilderException.class})
    public void testRetrieveWithoutMem() throws VirtualMachineBuilderException {
        try {
            XenVirtualMachineBuilder b = new XenVirtualMachineBuilder(CONFIG_LOCATION);
            b.build("noMem", new HashMap<String, String>());
        } catch (IOException e) {
            Assert.fail(e.getMessage(), e);
        } catch (MissingRequiredPropertyException e) {
            Assert.fail(e.getMessage(), e);
        }
    }
}
