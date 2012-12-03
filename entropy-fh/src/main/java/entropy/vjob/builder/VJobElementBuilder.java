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

import entropy.configuration.Configuration;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.platform.PlatformFactory;
import entropy.template.VirtualMachineTemplateFactory;

import java.util.Map;

/**
 * An interface to specify a node and virtual machine builder.
 * This builder is used when a vjob is deserialized. It has to retrieve the existing nodes and virtual machines
 * in a configuration from their name, or ask for a virtual machine instantiation is the virtual machine is not
 * yet handled.
 *
 * @author Fabien Hermenier
 */
public interface VJobElementBuilder {
    /**
     * Set the configuration to use to retrieve existing elements.
     *
     * @param cfg the configuration
     */
    void useConfiguration(Configuration cfg);

    /**
     * Get the element as a node.
     *
     * @param id the identifier of the element
     * @return a node if such an element exists in the current configuration with the identifier.
     *         {@code null} otherwise
     */
    Node matchAsNode(String id);

    /**
     * Get an element as an existing virtual machine.
     *
     * @param id the identifier of the virtual machine
     * @return a virtual machine is such an element exists in the current configuraton or {@code null}
     */
    VirtualMachine matchVirtualMachine(String id);

    /**
     * Get an element as an existing virtual machine or a new virtual machine.
     *
     * @param id      the identifier of the virtual machine
     * @param tpl     the template of the virtual machine
     * @param options the options associated to the virtual machine
     * @return if the virtual machine already exists in the current configuration if it matches also the template. In this case, options
     *         are overrided. Otherwise, a new virtual machine will be instantiated using the given template and the options.
     */
    VirtualMachine matchVirtualMachine(String id, String tpl, Map<String, String> options);

    /**
     * Get the template factory used to instantiate new virtual machines.
     *
     * @return the templates
     */
    VirtualMachineTemplateFactory getTemplates();


    /**
     * Get the platform factory used to instantiate node platform.
     *
     * @return the platform factory.
     */
    PlatformFactory getPlatformFactory();
}
