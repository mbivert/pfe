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

package entropy.configuration;

import gnu.trove.THashMap;

import java.util.Set;

/**
 * Implement a Virtual Machine.
 * Depending on its state, a virtual machine may consume some CPU and memory resources.
 * It may require some different amount of CPU and memory resources to run at a peak level.
 *
 * @author Fabien Hermenier
 * @deprecated use {@link SimpleVirtualMachine} instead
 */
public final class DefaultVirtualMachine extends DefaultManagedElement implements VirtualMachine {

    /**
     * Identifier for memory consumption. (should not be used. Deprecated ?}
     */
    public static final String MEMORY = "memory";

    /**
     * Identifier for the memory consumption.
     */
    public static final String MEMORY_CONSUMPTION = MEMORY;

    /**
     * Identifier for the amount of memory the virtual machine need.
     */
    public static final String MEMORY_NEED = "memory#need";

    /**
     * Identifier for the number of VCPU.
     */
    public static final String CPU_NB = "cpu#nb";

    /**
     * Identifier for the consumption of each VCPU.
     */
    public static final String CPU_CONSUMPTION = "cpu#consumption";

    /**
     * Identifier for the amount of VCPU the virtual machine need.
     */
    public static final String CPU_NEED = "cpu#need";

    /**
     * Identifier for the maximum amount of VCPU the virtual machine can use.
     */
    public static final String CPU_MAX = "cpu#max";


    /**
     * The identifier for the VJob ID.
     */
    public static final String VJOB_ID = "leaseId";

    private String template;

    private THashMap<String, String> options;

    private String platform;

    /**
     * Instantiate a new virtual machine.
     *
     * @param id          The node identifier, must be unique
     * @param nbCPUs      The number of virtual CPUs
     * @param consumption The consumption of each CPU
     * @param memory      The amount of memory in MB
     */
    public DefaultVirtualMachine(String id, int nbCPUs, int consumption, int memory) {
        super(id);
        updateValue(DefaultVirtualMachine.CPU_NB, nbCPUs);
        updateValue(DefaultVirtualMachine.MEMORY_CONSUMPTION, memory);
        updateValue(DefaultVirtualMachine.CPU_CONSUMPTION, consumption);
        //By default, needs = demand to avoid a NullPointerException when using getValue()
        updateValue(DefaultVirtualMachine.CPU_NEED, consumption);
        updateValue(DefaultVirtualMachine.MEMORY_NEED, memory);
        updateValue(DefaultVirtualMachine.CPU_MAX, -1);
        this.options = new THashMap<String, String>();
    }

    /**
     * Copy constructor. Make a deep copy of common values stored into a virtual
     * machine
     *
     * @param ref the reference virtual machine
     */
    public DefaultVirtualMachine(VirtualMachine ref) {
        this(ref.getName(), ref.getNbOfCPUs(), ref.getCPUConsumption(), ref.getMemoryConsumption());
        this.updateValue(DefaultVirtualMachine.VJOB_ID, ref.getVJobId());
        this.setCPUNeed(ref.getCPUDemand());
        this.setMemoryNeed(ref.getMemoryDemand());
        this.setCPUMax(ref.getCPUMax());
        this.setTemplate(ref.getTemplate());
        for (String opt : ref.getOptions()) {
            this.options.put(opt, ref.getOption(opt));
        }
    }

    /**
     * Compare this instance to another.
     *
     * @param o The instance to compare with
     * @return true if o is an instance of VirtualMachine and has the same name
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof VirtualMachine) {
            return getName().equals(((VirtualMachine) o).getName());
        }
        return false;
    }

    /**
     * Get the hashcode.
     *
     * @return the hashcode of the virtual machine name
     */
    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /**
     * Return the amount of memory consumed by the virtual machine.
     *
     * @return the amount of memory in MB
     */
    public int getMemoryConsumption() {
        return (Integer) getValue(DefaultVirtualMachine.MEMORY_CONSUMPTION);
    }

    /**
     * Get the amount of memory the virtual machine require to run at a peak level.
     * if the value is not known, it returns <code>this.getMemoryConsumption()</code>.
     *
     * @return a positive integer that may be null
     */
    public int getMemoryDemand() {
        return (Integer) getValue(DefaultVirtualMachine.MEMORY_NEED);
    }

    /**
     * Return the number of virtual CPUs required by the virtual machine.
     *
     * @return a integer >= 1
     */
    public int getNbOfCPUs() {
        return (Integer) getValue(DefaultVirtualMachine.CPU_NB);
    }

    /**
     * Return the current CPU consumption of each CPU required by the virtual
     * machine.
     *
     * @return a integer
     */
    public int getCPUConsumption() {
        return (Integer) getValue(DefaultVirtualMachine.CPU_CONSUMPTION);
    }

    /**
     * Set the amount of memory resources a virtual machine require to run at a peak level.
     *
     * @param amount the amount.
     */
    public void setMemoryNeed(int amount) {
        this.updateValue(MEMORY_NEED, amount);
    }

    /**
     * Get the amount of CPU resources the virtual machine need to run at a peak level.
     * if the value is not known, it returns <code>this.getCPUConsumption()</code>.
     *
     * @return a positive integer.
     */
    public int getCPUDemand() {
        return (Integer) getValue(DefaultVirtualMachine.CPU_NEED);
    }

    /**
     * Set the amount of CPU resources a virtual machine require to run at a peak level.
     *
     * @param amount the amount.
     */
    public void setCPUNeed(int amount) {
        this.updateValue(CPU_NEED, amount);
    }

    /**
     * Get the vjob Identifier of the virtual machine.
     *
     * @return the identifier of the vjob. May be null
     */
    public String getVJobId() {
        return ((String) getValue(DefaultVirtualMachine.VJOB_ID));
    }

    @Override
    public void setCPUConsumption(int c) {
        updateValue(CPU_CONSUMPTION, c);
    }

    @Override
    public void setCPUMax(int nb) {
        this.updateValue(CPU_MAX, nb);
    }

    @Override
    public int getCPUMax() {
        return (Integer) getValue(CPU_MAX);
    }

    @Override
    public void setMemoryConsumption(int m) {
        updateValue(MEMORY_CONSUMPTION, m);
    }

    @Override
    public void setCPUDemand(int c) {
        updateValue(CPU_NEED, c);
    }

    @Override
    public void setMemoryDemand(int m) {
        updateValue(MEMORY_NEED, m);
    }

    @Override
    public void setNbOfCPUs(int nb) {
        updateValue(CPU_NB, nb);
    }

    @Override
    public VirtualMachine clone() {
        return new DefaultVirtualMachine(this);
    }

    @Override
    public String getTemplate() {
        return this.template;
    }

    @Override
    public void setTemplate(String tpl) {
        this.template = tpl;
    }

    @Override
    public void addOption(String opt) {
        this.addOption(opt, null);
    }

    @Override
    public boolean checkOption(String opt) {
        return this.options.contains(opt);
    }

    @Override
    public Set<String> getOptions() {
        return this.options.keySet();
    }

    @Override
    public void addOption(String key, String value) {
        this.options.put(key, value);
    }

    @Override
    public String getOption(String k) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getHostingPlatform() {
        return this.platform;
    }

    @Override
    public void setHostingPlatform(String p) {
        this.platform = p;
    }
}
