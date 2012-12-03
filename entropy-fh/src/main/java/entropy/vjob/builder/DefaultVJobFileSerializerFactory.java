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
import entropy.vjob.builder.protobuf.ProtobufVJobSerializer;
import entropy.vjob.builder.xml.XmlVJobSerializer;
import gnu.trove.THashMap;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Factory to write a vjob into a file.
 * The output format is decided depending on the file extension.
 * By default, the factory provides Xml and protobuf serializer
 *
 * @author Fabien Hermenier
 */
public final class DefaultVJobFileSerializerFactory implements VJobFileSerializerFactory {

    private static final VJobFileSerializerFactory INSTANCE = new DefaultVJobFileSerializerFactory();

    private THashMap<String, VJobFileSerializer> serializers;

    /**
     * No instantiation.
     */
    private DefaultVJobFileSerializerFactory() {
        this.serializers = new THashMap<String, VJobFileSerializer>();
        this.add(XmlVJobSerializer.getInstance());
        this.add(ProtobufVJobSerializer.getInstance());
    }

    public Collection<VJobFileSerializer> getSerializers() {
        return this.serializers.values();
    }

    public void add(VJobFileSerializer ser) {
        this.serializers.put(ser.getFileExtension(), ser);
    }

    @Override
    public void write(VJob vjob, String path) throws IOException {
        String ext = path.substring(path.lastIndexOf('.') + 1, path.length());
        VJobFileSerializer ser = this.serializers.get(ext);
        if (ser == null) {
            throw new IOException("Unable to decide about the output format for '" + path + "'. Availables are " + this.serializers.keySet());
        }
        ser.write(vjob, path);
    }

    public static VJobFileSerializerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public Set<String> getManagedExtensions() {
        return serializers.keySet();
    }
}
