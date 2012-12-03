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

package entropy.template.stub;

import entropy.configuration.SimpleVirtualMachine;
import entropy.configuration.VirtualMachine;
import entropy.template.VirtualMachineBuilderException;
import entropy.template.VirtualMachineTemplate;
import gnu.trove.THashSet;

import java.util.Map;

/**
 * A Default template that is customized at instantiation.
 * The identifier of the builded virtual machine will be a class 4 UUID.
 *
 * @author Fabien Hermenier
 */
public class StubVirtualMachineTemplate implements VirtualMachineTemplate {

    /**
     * The number of CPUs of the virtual machines.
     */
    private int nbCPUs;

    /**
     * The memory consumption of the virtual machines.
     */
    private int mem;

    /**
     * The cpu consumption of the virtual machines.
     */
    private int cpu;

    /**
     * Description of the template.
     */
    private String descr;

    /**
     * Identifier of the template.
     */
    private String id;

    /**
     * The set of accepted actions.
     */
    private THashSet<String> options;

    /**
     * Make a new template.
     *
     * @param id      the identifier of the template. Not empty
     * @param nbCPU   the number of CPU of the virtual machines
     * @param cpu     the CPU consumption of the virtual machines
     * @param mem     the memory capacity of the virtual machines
     * @param options The options accepted by this template
     */
    public StubVirtualMachineTemplate(String id, int nbCPU, int cpu, int mem, THashSet<String> options) {
        this.id = id;
        this.nbCPUs = nbCPU;
        this.cpu = cpu;
        this.mem = mem;
        this.options = options;
    }

    @Override
    public VirtualMachine build(String name, Map<String, String> options) throws VirtualMachineBuilderException {
        VirtualMachine vm = new SimpleVirtualMachine(name, nbCPUs, cpu, mem);
        vm.setCPUMax(cpu);
        for (Map.Entry<String, String> e : options.entrySet()) {
            vm.addOption(e.getKey(), e.getValue());
        }
        return vm;
    }

    @Override
    public String getIdentifier() {
        return this.id;
    }

    public int getNbCPUs() {
        return nbCPUs;
    }

    public int getMemory() {
        return mem;
    }

    public int getCPU() {
        return cpu;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(id).append(" {\n");
        b.append("\tnbCPUs=").append(nbCPUs).append('\n');
        b.append("\tCPU=").append(cpu).append('\n');
        b.append("\tmemory=").append(mem).append('\n');
        b.append("\tdescription=").append(descr).append("\n");
        b.append("\toptions=").append(options).append("\n}\n");
        return b.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StubVirtualMachineTemplate that = (StubVirtualMachineTemplate) o;
        return (cpu == that.cpu && mem == that.mem && nbCPUs == that.nbCPUs && id.equals(that.id) && options.equals(that.options));
    }

    @Override
    public int hashCode() {
        int result = nbCPUs;
        result = 31 * result + mem;
        result = 31 * result + cpu;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + options.hashCode();
        return result;
    }
}
