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

package entropy.execution.driver;

import entropy.PropertiesHelper;
import entropy.TestHelper;
import entropy.configuration.Node;
import entropy.configuration.SimpleNode;
import entropy.configuration.SimpleVirtualMachine;
import entropy.configuration.VirtualMachine;
import entropy.plan.action.*;
import entropy.plan.action.Shutdown;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests for DriverFactory.
 *
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestDriverFactory {


    /**
     * Make bad actions.
     *
     * @return an array of bad actions.
     */
    @DataProvider(name = "getBadActions")
    public static Object[][] getBadActions() {
        Node n1 = new SimpleNode("N1", 1, 2, 3);
        Node n2 = new SimpleNode("N2", 1, 2, 3);
        Node n3 = new SimpleNode("N3", 1, 2, 3);
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 2, 3);
        n1.setMigrationDriverID("xenapi");
        n1.setResumeDriverID("xenapi");
        n1.setSuspendDriverID("xenapi");
        n1.setStopDriverID("sshStop");
        n1.setRunDriverID("sshRun");
        n1.setStartupDriverID("wakeOnLan");
        n1.setShutdownDriverID("sshShutdown");
        n2.setMigrationDriverID("sshMigration");

        return new Object[][]{
                {new Migration(vm1, n3, n2)}, //No migration driver
                {new Startup(n3)}, // no driver
                {new Shutdown(n3)}, // no driver
                {new Resume(vm1, n1, n3)}, // no driver
                {new Suspend(vm1, n1, n3)}, // no driver
                {new Run(vm1, n3)}, // no driver
                {new Stop(vm1, n3)}, // no driver
        };
    }

    /**
     * Test the detection of non viable action.
     *
     * @param a the action
     * @throws DriverInstantiationException the exception we expect
     */
    @Test(dataProvider = "getBadActions", expectedExceptions = {DriverInstantiationException.class})
    public void testBadActions(Action a) throws DriverInstantiationException {
        PropertiesHelper props = TestHelper.readEntropyProperties("src/test/resources/entropy/execution/driver/TestDriverFactory.defaultProperties.txt");
        DriverFactory f = new DriverFactory(props);
        f.transform(a);
    }

    /**
     * Make good actions and the driver we want to have.
     *
     * @return a array containing array (first is the action, then the driver)
     */
    @DataProvider(name = "getGoodActions")
    public static Object[][] getGoodActions() {
        Node n1 = new SimpleNode("N1", 1, 2, 3);
        Node n2 = new SimpleNode("N2", 1, 2, 3);
        VirtualMachine vm1 = new SimpleVirtualMachine("VM1", 1, 2, 3);
        n1.setMigrationDriverID("xenapi");
        n1.setResumeDriverID("xenapi");
        n1.setSuspendDriverID("xenapi");
        n1.setStopDriverID("sshStop");
        n1.setRunDriverID("sshRun");
        n1.setStartupDriverID("wakeOnLan");
        n1.setShutdownDriverID("sshShutdown");
        n2.setMigrationDriverID("sshMigration");

        return new Object[][]{
                {new Migration(vm1, n1, n1), XenGuestMigration.class},
                {new Migration(vm1, n2, n2), SSHMigration.class},
                {new Startup(n1), WoLStartup.class},
                {new Shutdown(n1), SSHShutdown.class},
                {new Resume(vm1, n1, n1), XenGuestResume.class},
                {new Suspend(vm1, n1, n1), XenGuestSuspend.class},
                {new Run(vm1, n1), SSHRun.class},
                {new Stop(vm1, n1), SSHStop.class},
        };
    }

    /**
     * Test the fabric of good drivers.
     *
     * @param a the action
     * @param c the class of the driver we expect
     */
    @Test(dataProvider = "getGoodActions")
    public void testGoodAction(Action a, Class<Driver> c) {
        try {
            PropertiesHelper props = TestHelper.readEntropyProperties("src/test/resources/entropy/execution/driver/TestDriverFactory.defaultProperties.txt");
            DriverFactory f = new DriverFactory(props);
            Assert.assertEquals(f.transform(a).getClass(), c);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }
}
