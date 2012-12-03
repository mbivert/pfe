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

package entropy.platform;

import java.util.Set;

/**
 * Denotes a possible hosting platform that can
 * be deployed on a node.
 * <p/>
 * TODO: A good place to have action transformation into drivers for actions related to VMs
 *
 * @author Fabien Hermenier
 */
public interface Platform {


    /**
     * Get the identifier associated to the platform.
     *
     * @return a non-empty String
     */
    String getIdentifier();

    /**
     * Specify an option for the platform.
     *
     * @param opt the option
     */
    void addOption(String opt);

    /**
     * Check the presence of an option in the platform specification.
     *
     * @param opt the option to check the presence of
     * @return {@code true} if the option was previously specified
     */
    boolean checkOption(String opt);

    /**
     * Get the options related to the platform specification.
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

    Platform clone();
}
