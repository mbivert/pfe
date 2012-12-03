/*
 * Copyright (c) Fabien Hermenier
 *
 * This file is part of Entropy.
 *
 * Entropy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Entropy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package entropy.configuration;

import java.util.Set;

/**
 * Specify a Virtual Machine.
 * Depending on its state, a virtual machine may consume some CPU and memory resources.
 * It may require some different amount of CPU and memory resources to run at a peak level.
 *
 * @author Fabien Hermenier
 */
public interface VirtualMachine extends ManagedElement, Cloneable {

    /**
     * Return the number of virtual CPUs required by the virtual machine.
     *
     * @return a integer >= 1
     */
    int getNbOfCPUs();

    /**
     * Return the current CPU consumption of the virtual
     * machine.
     *
     * @return a integer
     */
    int getCPUConsumption();

    /**
     * Return the amount of memory consumed by the virtual machine.
     *
     * @return the amount of memory
     */
    int getMemoryConsumption();

    /**
     * Get the amount of memory the virtual machine require to run at a peak level.
     *
     * @return the amount of memory
     */
    int getMemoryDemand();

    /**
     * Get the amount of CPU resources the virtual machine need to run at a peak level.
     *
     * @return a positive integer.
     */
    int getCPUDemand();

    /**
     * Set the current CPU usage of the virtual machine.
     *
     * @param c the cpu usage
     */
    void setCPUConsumption(int c);

    /**
     * Set the current memory usage of the virtual machine.
     *
     * @param m the memory usage
     */
    void setMemoryConsumption(int m);

    /**
     * Set the amount of CPU resources a virtual machine require to run at a peak level.
     *
     * @param c the amount.
     */
    void setCPUDemand(int c);

    /**
     * Set the amount of memory resources a virtual machine require to run at a peak level.
     *
     * @param m the amount.
     */
    void setMemoryDemand(int m);

    /**
     * Set the current number of vCPU used by the virtual machine.
     *
     * @param nb the number of vCPU.
     */
    void setNbOfCPUs(int nb);

    /**
     * Set the maximum amount of CPU the VM is allowed to use.
     *
     * @param nb the amount of CPU
     */
    void setCPUMax(int nb);

    /**
     * Get the maximum amount of CPU the VM is allowed to use.
     *
     * @return {@link Integer#MAX_VALUE} if their is no restriction, otherwise a positive number
     */
    int getCPUMax();

    /**
     * Deep copy of the virtual machine. All the resources usage
     * are copied
     *
     * @return a copy of the virtual machine
     */
    VirtualMachine clone();

    /**
     * Get the vjob Identifier of the virtual machine.
     *
     * @return the identifier of the vjob. May be null
     */
    @Deprecated
    String getVJobId();

    /**
     * Get the template identifier associated to the virtual machine.
     *
     * @return a non-empty string
     */
    String getTemplate();

    /**
     * Set the template identifier associated to the virtual machine.
     *
     * @param tpl the template identifier
     */
    void setTemplate(String tpl);

    /**
     * Specify an option for the virtual machine.
     *
     * @param opt the option
     */
    void addOption(String opt);

    /**
     * Check the presence of an option in the virtual machine specification.
     *
     * @param opt the option to check the presence of
     * @return {@code true} if the option was previously specified
     */
    boolean checkOption(String opt);

    /**
     * Get the options related to the virtual machine specification.
     *
     * @return a set of option, may be empty.
     */
    Set<String> getOptions();

    /**
     * Add a valuated option.
     *
     * @param key   the identifier of the option
     * @param value its value
     */
    void addOption(String key, String value);

    /**
     * Get the value associated to an option.
     *
     * @param k the identifier of the option
     * @return the value of the option if set. {@code null} otherwise
     */
    String getOption(String k);

    /**
     * Get the identifier of the hosting platform for this virtual machine.
     *
     * @return an identifier or {@code null}
     */
    String getHostingPlatform();

    /**
     * Set the hosting platform for this virtual machine.
     *
     * @param p the identifier of the hosting platform
     */
    void setHostingPlatform(String p);

}
