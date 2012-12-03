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

package entropy.vjob.builder.xml;

import entropy.configuration.*;
import entropy.vjob.VJob;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.VJobElementBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestXMLVJobBuilderBuilder {

    public static final String DEFAULT = "config/XMLVJobs.properties";

    /**
     * Load the default properties file and try to parse a sample vjob.
     */
    public void testDefault() {
        XMLVJobBuilderBuilder bb = new XMLVJobBuilderBuilder(DEFAULT);
        try {
            VJobElementBuilder eb = new DefaultVJobElementBuilder(null);

            Configuration cfg = new SimpleConfiguration();
            for (int i = 1; i <= 30; i++) {
                cfg.addWaiting(new SimpleVirtualMachine("clients.foo.VM" + i, 5, 5, 5));
                cfg.addWaiting(new SimpleVirtualMachine("clients.bar.VM" + i, 5, 5, 5));
            }

            for (int i = 1; i <= 20; i++) {
                cfg.addOnline(new SimpleNode("node-" + i, 1, 1, 1));
            }
            XMLVJobBuilder b = bb.build(eb);
            b.getElementBuilder().useConfiguration(cfg);
            VJob v = b.build(new File("src/test/resources/entropy/vjob/builder/xml/test.xml"));
            Assert.assertEquals(v.getVirtualMachines().size(), 60);

            //Check some options
            VirtualMachine vm = v.getVirtualMachines().get("clients.bar.VM28");
            Assert.assertEquals(vm.getOptions().size(), 2);
            Assert.assertTrue(vm.checkOption("volatile"));
            Assert.assertTrue(vm.checkOption("stop"));
            Assert.assertNull(vm.getOption("volatile"));
            Assert.assertEquals(vm.getOption("stop"), "7");

            Assert.assertEquals(v.getNodes().size(), 10);
            Assert.assertEquals(v.getConstraints().size(), 4);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }


}
