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

import entropy.MissingRequiredPropertyException;
import entropy.PropertiesHelper;
import entropy.template.VirtualMachineTemplateFactoryBuilderException;
import entropy.template.VirtualMachineTemplateFactoryBuilderFromProperties;
import entropy.vjob.VJob;

import java.io.IOException;

/**
 * @author Fabien Hermenier
 */
public class VJobBuilderFactoryBuilderFromProperties implements VJobBuilderFactoryBuilder {

    public static final String DEFAULT_PROPERTIES = "config/vjobsBuilder.properties";

    public static final String TO_LOAD = "vjobBuilders.load";

    public static final String FQCN_PROPERTY = "vjobBuilders.location.";

    private String file;

    public VJobBuilderFactoryBuilderFromProperties() {
        this(DEFAULT_PROPERTIES);
    }

    public VJobBuilderFactoryBuilderFromProperties(String path) {
        this.file = path;
    }

    public String getFile() {
        return this.file;
    }

    @Override
    public VJobBuilderFactory build() throws VJobBuilderFactoryBuilderException {

        //Get the VM builder


        DefaultVJobBuilderFactory f = new DefaultVJobBuilderFactory();
        try {
            VJobElementBuilder eb = new DefaultVJobElementBuilder(new VirtualMachineTemplateFactoryBuilderFromProperties().build(), null);

            PropertiesHelper prop = new PropertiesHelper(file);
            String fqcn = null;
            try {
                String list = prop.getRequiredProperty(TO_LOAD);
                String[] constraints = list.split(",");
                for (String constraint : constraints) {
                    fqcn = prop.getRequiredProperty(FQCN_PROPERTY + constraint);
                    Class cl = getClass().getClassLoader().loadClass(fqcn);
                    VJobBuilderBuilder bb = (VJobBuilderBuilder) cl.newInstance();
                    f.add(bb.build(eb));
                }
                VJob.logger.debug("Builders available for file extensions: " + f.getManagedExtensions());
            } catch (MissingRequiredPropertyException e) {
                throw new VJobBuilderFactoryBuilderException("Unable to build the vjob builder: " + e.getMessage(), e);
            } catch (ClassNotFoundException e) {
                throw new VJobBuilderFactoryBuilderException("Unable to build the vjob builder: " + e.getMessage(), e);
            } catch (InstantiationException e) {
                throw new VJobBuilderFactoryBuilderException("Unable to build the vjob builder: " + e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new VJobBuilderFactoryBuilderException("Unable to build the vjob builder: " + e.getMessage(), e);
            } catch (VJobBuilderBuilderException e) {
                throw new VJobBuilderFactoryBuilderException("Unable to build the vjob builder '" + fqcn + "' : " + e.getMessage(), e);
            }
        } catch (IOException e) {
            throw new VJobBuilderFactoryBuilderException("Unable to read '" + file + "': " + e.getMessage(), e);
        } catch (VirtualMachineTemplateFactoryBuilderException e) {
            throw new VJobBuilderFactoryBuilderException(e.getMessage(), e);
        }
        return f;
    }
}
