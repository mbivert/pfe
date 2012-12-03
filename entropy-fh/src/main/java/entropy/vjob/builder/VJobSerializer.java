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
import java.io.OutputStream;

/**
 * Interface to specify a vjob serializer/deserializer.
 *
 * @author Fabien Hermenier
 */
public interface VJobSerializer {

    /**
     * Serialize a vjob into a stream.
     *
     * @param vjob the vjob to serialize
     * @param out  the stream to write to
     * @throws java.io.IOException if an error occurred while writing to the stream
     */
    void serialize(VJob vjob, OutputStream out) throws IOException;
}
