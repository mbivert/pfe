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

package entropy.template;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestVirtualMachineTemplateFactoryBuilderFromProperties {

    private static final String CFG = "src/test/resources/entropy/template/templates.properties";

    public void testDefault() throws Exception {
        VirtualMachineTemplateFactory f = new VirtualMachineTemplateFactoryBuilderFromProperties().build();
        Assert.assertEquals(f.getAvailables().size(), 1);
    }

    public void testWithStubs() throws Exception {
        VirtualMachineTemplateFactory f = new VirtualMachineTemplateFactoryBuilderFromProperties(CFG).build();
        Assert.assertEquals(f.getAvailables().size(), 1);
        Assert.assertTrue(f.getAvailables().contains("tinyInstance"));
    }
}
