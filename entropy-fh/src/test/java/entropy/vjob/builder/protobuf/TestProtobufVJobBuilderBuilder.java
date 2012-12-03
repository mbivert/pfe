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

import entropy.configuration.Configuration;
import entropy.configuration.SimpleConfiguration;
import entropy.configuration.SimpleNode;
import entropy.configuration.SimpleVirtualMachine;
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
public class TestProtobufVJobBuilderBuilder {

    public static final String DEFAULT = "config/protobufVJobs.properties";

    /**
     * Load the default properties file and try to parse a sample vjob.
     */
    public void testDefault() {
        ProtobufVJobBuilderBuilder bb = new ProtobufVJobBuilderBuilder(DEFAULT);
        try {
            VJobElementBuilder eb = new DefaultVJobElementBuilder(null);

            Configuration cfg = new SimpleConfiguration();
            for (int i = 1; i < 5; i++) {
                cfg.addWaiting(new SimpleVirtualMachine("vm" + i, i, i, i));
                cfg.addOnline(new SimpleNode("N" + i, i, i, i));
            }
            ProtobufVJobBuilder b = bb.build(eb);
            b.getElementBuilder().useConfiguration(cfg);
            VJob v = b.build(new File("src/test/resources/entropy/vjob/builder/protobuf/tmp.pb"));
            Assert.assertEquals(v.getVirtualMachines(), cfg.getAllVirtualMachines());
            Assert.assertEquals(v.getNodes(), cfg.getAllNodes());
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }


}
