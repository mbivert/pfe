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

import entropy.template.stub.TinyInstance;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestDefaultVirtualMachineTemplateFactory {

    public void testBasic() {
        DefaultVirtualMachineTemplateFactory d = new DefaultVirtualMachineTemplateFactory();
        d.add(new TinyInstance());
        Assert.assertTrue(d.getAvailables().size() == 1 && d.getAvailables().contains("tinyInstance"));
        Assert.assertNull(d.getTemplate("toto"));
        Assert.assertEquals(d.getTemplate("tinyInstance").getIdentifier(), "tinyInstance");
    }
}
