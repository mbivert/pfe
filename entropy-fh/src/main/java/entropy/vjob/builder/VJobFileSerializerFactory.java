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

import entropy.vjob.VJob;

import java.io.IOException;
import java.util.Set;

/**
 * @author Fabien Hermenier
 */
public interface VJobFileSerializerFactory {

    /**
     * Write a vjob to a file.
     * If the parent folder does not exists, it is created.
     * The file format depends on the file extension.
     *
     * @param vjob the vjob to write
     * @param path of the file
     * @throws java.io.IOException if an error occurred while writing the file
     */
    void write(VJob vjob, String path) throws IOException;

    Set<String> getManagedExtensions();
}
