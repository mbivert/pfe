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

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;

/**
 * @author Fabien Hermenier
 */
@Test(groups = {"unit"})
public class TestXmlBtrPlaceVJobSerializer {

    private static final String RC_ROOT = "src/test/resources/btrpsl/";

    public static final String SCHEMA_LOCATION = "src/main/resources/entropy/vjob/builder/xml/vjob.xsd";

    public void test() {
        VJobElementBuilder e = new DefaultVJobElementBuilder(null);
        Configuration cfg = new SimpleConfiguration();
        e.useConfiguration(cfg);
        for (int i = 1; i <= 30; i++) {
            cfg.addWaiting(new SimpleVirtualMachine("clients.foo.VM" + i, 5, 5, 5));

            VirtualMachine vm = new SimpleVirtualMachine("clients.bar.VM" + i, 5, 5, 5);
//            vm.addOption("volatile");
//            vm.addOption("start", Integer.toString(i));
            cfg.addWaiting(vm);
        }

        for (int i = 1; i <= 20; i++) {
            cfg.addOnline(new SimpleNode("node-" + i, 1, 1, 1));
        }
        DefaultXMLConstraintsCatalog c = new DefaultXMLConstraintsCatalog();
        c.add(new ContinuousSpreadBuilder());
        c.add(new LonelyBuilder());
        c.add(new AmongBuilder());
        c.add(new FenceBuilder());
        c.add(new BanBuilder());
        c.add(new CapacityBuilder());
        XMLVJobBuilder b = new XMLVJobBuilder(e, c);
        try {
            VJob v = b.build(new File("src/test/resources/entropy/vjob/builder/xml/test.xml"));
            Assert.assertEquals(v.getVirtualMachines().size(), 60);
            Assert.assertEquals(v.getNodes().size(), 10);
            Assert.assertEquals(v.getConstraints().size(), 4);
            File f = File.createTempFile("test", "test");
            f.deleteOnExit();
            XmlVJobSerializer.getInstance().write(v, f.getAbsolutePath());
            validate(f);

            //We parse the resulting file to compare with the original vjob
            VJob v2 = b.build(f);
            Assert.assertEquals(v, v2);
        } catch (Exception x) {
            Assert.fail(x.getMessage(), x);
        }
    }

    private static void validate(File f) {
        try {
            // define the type of schema - we use W3C:
            String schemaLang = "http://www.w3.org/2001/XMLSchema";

            // get validation driver:
            SchemaFactory factory = SchemaFactory.newInstance(schemaLang);

            // create schema by reading it from an XSD file:
            Schema schema = factory.newSchema(new StreamSource(SCHEMA_LOCATION));
            Validator validator = schema.newValidator();

            // at last perform validation:
            validator.validate(new StreamSource(f.getAbsolutePath()));

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
}
