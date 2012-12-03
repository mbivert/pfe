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

import entropy.configuration.VirtualMachine;

import java.util.Map;

/**
 * A Template is a virtual machine skeleton. Each call to build() generate a new
 * Virtual Machine instance having a supposed globally unique name.
 *
 * @author Fabien Hermenier
 */
public interface VirtualMachineTemplate {

    /**
     * Build a new virtual machine.
     *
     * @param name    the name of the virtual machine
     * @param options the options
     * @return a new virtual machine with a unique name
     */
    VirtualMachine build(String name, Map<String, String> options) throws VirtualMachineBuilderException;

    /**
     * Get the identifier associated to the template.
     *
     * @return a non-empty String
     */
    String getIdentifier();
}
