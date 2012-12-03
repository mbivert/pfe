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

import entropy.configuration.*;
import entropy.platform.PlatformFactory;
import entropy.template.VirtualMachineTemplateFactory;

import java.util.Map;

/**
 * A stub builder.
 * if a node or a VM is asked, it is created with all of the resource capacities/usage equals to 1.
 *
 * @author Fabien Hermenier
 */
public class StubVJobElementBuilder implements VJobElementBuilder {

    private ManagedElementSet<Node> ns;

    private ManagedElementSet<VirtualMachine> vms;

    private static final StubVJobElementBuilder instance = new StubVJobElementBuilder();

    private StubVJobElementBuilder() {
        this.ns = new SimpleManagedElementSet<Node>();
        this.vms = new SimpleManagedElementSet<VirtualMachine>();
    }


    public static StubVJobElementBuilder getInstance() {
        return instance;
    }

    /**
     * Unused.
     *
     * @param cfg the configuration
     */
    @Override
    public void useConfiguration(Configuration cfg) {

    }

    @Override
    public Node matchAsNode(String id) {
        Node n = ns.get(id);
        if (n == null) {
            n = new SimpleNode(id, 1, 1, 1);
            ns.add(n);
        }
        return n;
    }

    @Override
    public VirtualMachine matchVirtualMachine(String id) {
        VirtualMachine vm = vms.get(id);
        if (vm == null) {
            vm = new SimpleVirtualMachine(id, 1, 1, 1);
            vms.add(vm);
        }
        return vm;
    }

    @Override
    public VirtualMachine matchVirtualMachine(String id, String tpl, Map<String, String> options) {
        VirtualMachine vm = vms.get(id);
        if (vm == null) {
            vm = new SimpleVirtualMachine(id, 1, 1, 1);
            vm.setTemplate(tpl);
            for (Map.Entry<String, String> e : options.entrySet()) {
                vm.addOption(e.getKey(), e.getValue());
            }
            vms.add(vm);
        }
        return vm;
    }

    @Override
    public VirtualMachineTemplateFactory getTemplates() {
        return null;
    }

    @Override
    public PlatformFactory getPlatformFactory() {
        return null;
    }
}
