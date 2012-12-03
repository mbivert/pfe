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

package entropy.execution;

import entropy.configuration.ManagedElement;

/**
 * A fake managed element that denote the forge that
 * is responsible of the instantiation of the virtual machines.
 *
 * @author Fabien Hermenier
 */
public class Forge implements ManagedElement {

    private final static Forge instance = new Forge();

    /**
     * No direct instantiation, its a singleton.
     */
    private Forge() {
    }

    @Override
    public String getName() {
        return "FORGE";
    }

    @Override
    public void rename(String n) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the unique instance of this class.
     *
     * @return the singleton
     */
    public static final Forge getInstance() {
        return instance;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return 18;
    }
}
