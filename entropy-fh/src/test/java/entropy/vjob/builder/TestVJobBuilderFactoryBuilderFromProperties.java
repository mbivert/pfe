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

package entropy.vjob.builder;

import gnu.trove.THashSet;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Fabien Hermenier
 */
@Test(groups = "unit")
public class TestVJobBuilderFactoryBuilderFromProperties {

    private static final String DEFAULT = "config/vjobsBuilder.properties";

    public void testDefault() {
        try {
            VJobBuilderFactoryBuilderFromProperties bp = new VJobBuilderFactoryBuilderFromProperties(DEFAULT);
            VJobBuilderFactory f = bp.build();
            THashSet<String> exts = new THashSet<String>();
            exts.add("xml");
            exts.add("pbd");
            exts.add("plasma");
            Assert.assertEquals(f.getManagedExtensions(), exts);
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);
        }
    }
}
