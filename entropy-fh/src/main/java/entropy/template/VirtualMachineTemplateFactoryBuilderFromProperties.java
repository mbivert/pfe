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

import entropy.Entropy;
import entropy.MissingRequiredPropertyException;
import entropy.PropertiesHelper;

import java.io.File;
import java.io.IOException;

/**
 * @author Fabien Hermenier
 */
public class VirtualMachineTemplateFactoryBuilderFromProperties implements VirtualMachineTemplateFactoryBuilder {

    private static final String PROPERTIES_LOCATION = "config/templates.properties";

    /**
     * The property to define the constraints to add to the catalog.
     */
    public static final String TO_LOAD_PROPERTY = "templates.load";

    /**
     * The prefix of each contraint to load. Indicates the FQCN of the PlacementConstraintBuilder.
     */
    public static final String TEMPLATE_BASE = "template.";

    private String file = null;

    public VirtualMachineTemplateFactoryBuilderFromProperties() {
        this(PROPERTIES_LOCATION);
    }

    public VirtualMachineTemplateFactoryBuilderFromProperties(String file) {
        this.file = file;
    }


    @Override
    public VirtualMachineTemplateFactory build() throws VirtualMachineTemplateFactoryBuilderException {
        DefaultVirtualMachineTemplateFactory c = new DefaultVirtualMachineTemplateFactory();
        if (!new File(file).exists()) {
            Entropy.getLogger().info(file + " is unknown: No template to load");
            return c;
        }
        try {
            PropertiesHelper props = new PropertiesHelper(file);

            if (props.isDefined(TO_LOAD_PROPERTY)) {
                try {
                    String list = props.getRequiredProperty(TO_LOAD_PROPERTY);
                    String[] ids = list.split(",");
                    for (String id : ids) {
                        String fqcn = props.getRequiredProperty(TEMPLATE_BASE + id);
                        Class cl = getClass().getClassLoader().loadClass(fqcn);
                        c.add((VirtualMachineTemplate) cl.newInstance());
                    }
                } catch (MissingRequiredPropertyException e) {
                    throw new VirtualMachineTemplateFactoryBuilderException("Unable to build the template factory: " + e.getMessage(), e);
                } catch (ClassNotFoundException e) {
                    throw new VirtualMachineTemplateFactoryBuilderException("Unable to build the template factory: " + e.getMessage(), e);
                } catch (InstantiationException e) {
                    throw new VirtualMachineTemplateFactoryBuilderException("Unable to build the template factory:" + e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    throw new VirtualMachineTemplateFactoryBuilderException("Unable to build the template factory:" + e.getMessage(), e);
                }
            } else {
                Entropy.getLogger().info("No template to load");
            }
            Entropy.getLogger().debug("Available templates: " + c.getAvailables());
            return c;
        } catch (IOException e) {
            throw new VirtualMachineTemplateFactoryBuilderException("Unable to read '" + file + "': " + e.getMessage(), e);
        }
    }
}
