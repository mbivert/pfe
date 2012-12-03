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

package entropy.vjob.builder.protobuf;

import entropy.PropertiesHelper;
import entropy.vjob.builder.VJobBuilderBuilder;
import entropy.vjob.builder.VJobBuilderBuilderException;
import entropy.vjob.builder.VJobElementBuilder;

import java.io.IOException;

/**
 * A ProtobufVJobBuilderBuilder using properties.
 *
 * @author Fabien Hermenier
 */
public class ProtobufVJobBuilderBuilder implements VJobBuilderBuilder {

    public static final String PROPERTIES = "config/protobufVJobs.properties";

    /**
     * The properties file.
     */
    private String file;


    /**
     * Build the vjob builder using the default properties file {@link #PROPERTIES}.
     */
    public ProtobufVJobBuilderBuilder() {
        this(PROPERTIES);
    }

    /**
     * Build the vjob builder using a specific properties file.
     *
     * @param file the properties file
     */
    public ProtobufVJobBuilderBuilder(String file) {
        this.file = file;
    }

    /**
     * Get the used properties file.
     *
     * @return
     */
    public String getFile() {
        return this.file;
    }

    @Override
    public ProtobufVJobBuilder build(VJobElementBuilder eb) throws VJobBuilderBuilderException {
        try {
            PropertiesHelper propHelper = new PropertiesHelper(this.file);

            PBConstraintsCatalog c = new ConstraintsCatalogBuilderFromProperties(propHelper).build();
            ProtobufVJobBuilder b = new ProtobufVJobBuilder(eb);
            b.setConstraintCatalog(c);
            return b;
        } catch (IOException e) {
            throw new VJobBuilderBuilderException("Unable to build the protobuf vjob builder:" + e.getMessage(), e);
        } catch (ConstraintsCalalogBuilderException e) {
            throw new VJobBuilderBuilderException("Unable to build the protobuf vjob builder:" + e.getMessage(), e);
        }
    }
}
