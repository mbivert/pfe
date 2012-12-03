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

package entropy.configuration.parser;

import entropy.configuration.SimpleVirtualMachine;
import entropy.configuration.VirtualMachine;

/**
 * Utility class to serialize and un-serialize a VirtualMachine to/from
 * its protobuf version.
 *
 * @author Fabien Hermenier
 */
public final class PBVirtualMachineSerializer {

    /**
     * Utility class. No instantiation.
     */
    private PBVirtualMachineSerializer() {
    }

    /**
     * Un-serialize the protobuf version of a virtual machine.
     *
     * @param pbVM the virtual machine to convert.
     * @return the entropy VirtualMachine.
     */
    public static VirtualMachine read(PBVirtualMachine.VirtualMachine pbVM) {
        VirtualMachine vm2 = new SimpleVirtualMachine(pbVM.getName());

        if (pbVM.hasCpuConsumption()) {
            vm2.setCPUConsumption(pbVM.getCpuConsumption());
        }

        if (pbVM.hasCpuDemand()) {
            vm2.setCPUDemand(pbVM.getCpuDemand());
        }

        if (pbVM.hasCpuMax()) {
            vm2.setCPUMax(pbVM.getCpuMax());
        }

        if (pbVM.hasMemoryConsumption()) {
            vm2.setMemoryConsumption(pbVM.getMemoryConsumption());
        }

        if (pbVM.hasMemoryDemand()) {
            vm2.setMemoryDemand(pbVM.getMemoryDemand());
        }

        if (pbVM.hasTemplate()) {
            vm2.setTemplate(pbVM.getTemplate());
        }

        if (pbVM.hasHostingPlatform()) {
            vm2.setHostingPlatform(pbVM.getHostingPlatform());
        }

        if (pbVM.hasNbOfCPUs()) {
            vm2.setNbOfCPUs(pbVM.getNbOfCPUs());
        }

        for (PBVirtualMachine.VirtualMachine.Option opt : pbVM.getOptionsList()) {
            String k = opt.getKey();
            if (opt.hasValue()) {
                vm2.addOption(k, opt.getValue());
            } else {
                vm2.addOption(k);
            }
        }

        return vm2;
    }

    /**
     * Serialize a virtual machine to a protobuf version.
     *
     * @param vm the virtual machine to serialize
     * @return the protobuf version of a virtual machine
     */
    public static PBVirtualMachine.VirtualMachine write(VirtualMachine vm) {
        PBVirtualMachine.VirtualMachine.Builder b2 = PBVirtualMachine.VirtualMachine.newBuilder();
        b2.setName(vm.getName());
        b2.setNbOfCPUs(vm.getNbOfCPUs());

        b2.setCpuConsumption(vm.getCPUConsumption());
        b2.setMemoryConsumption(vm.getMemoryConsumption());
        b2.setCpuDemand(vm.getCPUDemand());
        b2.setMemoryDemand(vm.getMemoryDemand());
        b2.setCpuMax(vm.getCPUMax());
        String s = vm.getTemplate();
        if (s != null) {
            b2.setTemplate(s);
        }

        s = vm.getHostingPlatform();
        if (s != null) {
            b2.setHostingPlatform(s);
        }
        PBVirtualMachine.VirtualMachine.Option.Builder b = PBVirtualMachine.VirtualMachine.Option.newBuilder();
        for (String opt : vm.getOptions()) {
            String v = vm.getOption(opt);
            b.setKey(opt);
            if (v != null) {
                b.setValue(v);
            }
            b2.addOptions(b.build());
            b.clear();
        }
        return b2.build();
    }
}
