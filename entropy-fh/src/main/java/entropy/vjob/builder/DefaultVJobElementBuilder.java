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

package entropy.vjob.builder;

import entropy.configuration.Configuration;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.platform.PlatformFactory;
import entropy.template.VirtualMachineBuilderException;
import entropy.template.VirtualMachineTemplate;
import entropy.template.VirtualMachineTemplateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Build the element of the vjobs from their
 * value.
 *
 * @author Fabien Hermenier
 */
public class DefaultVJobElementBuilder implements VJobElementBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger("VJobBuilder");

    /**
     * The vmBuilder to instantiate virtual machines.
     */
    private VirtualMachineTemplateFactory vmBuilder;

    /**
     * The current configuration to get existing nodes and virtual machines.
     */
    private Configuration curConfig;

    private PlatformFactory plts;

    /**
     * Make a new vmBuilder.
     *
     * @param b the vmBuilder to use to instantiate new virtual machines
     */
    public DefaultVJobElementBuilder(VirtualMachineTemplateFactory b) {
        this(b, null);
    }

    public DefaultVJobElementBuilder(VirtualMachineTemplateFactory b, PlatformFactory p) {
        this.vmBuilder = b;
        this.plts = p;
    }


    @Override
    public void useConfiguration(Configuration cfg) {
        curConfig = cfg;
    }

    @Override
    public Node matchAsNode(String id) {
        if (curConfig == null) {
            return null;
        }
        Node n = curConfig.getOnlines().get(id);
        if (n != null) {
            return n;
        }
        return curConfig.getOfflines().get(id);
    }

    @Override
    public VirtualMachine matchVirtualMachine(String id) {
        return matchVirtualMachine(id, null, null);
    }

    @Override
    public VirtualMachine matchVirtualMachine(String id, String tpl, Map<String, String> options) {
        VirtualMachine vm = null;
        if (curConfig != null) {
            vm = curConfig.getRunnings().get(id);
            if (vm == null) {
                vm = curConfig.getWaitings().get(id);
            }
            if (vm == null) {
                vm = curConfig.getSleepings().get(id);
            }
        }
        if (vm == null && vmBuilder != null) {
            try {
                VirtualMachineTemplate template = vmBuilder.getTemplate(tpl);
                if (template == null) {
                    return null;
                }
                vm = template.build(id, options);
                if (vm != null && curConfig != null) {
                    curConfig.addWaiting(vm);
                }
            } catch (VirtualMachineBuilderException e) {
                LOGGER.error(e.getMessage());
            }
        } else if (vm != null) {
            //Fullfil options if necessary, as they may have been updates
            if (options != null) {
                for (Map.Entry<String, String> e : options.entrySet()) {
                    vm.addOption(e.getKey(), e.getValue());
                }
            }
            if (vm.getTemplate() == null) {
                vm.setTemplate(tpl);
            }
        }
        return vm;
    }

    @Override
    public VirtualMachineTemplateFactory getTemplates() {
        return this.vmBuilder;
    }

    @Override
    public PlatformFactory getPlatformFactory() {
        return plts;
    }
}
