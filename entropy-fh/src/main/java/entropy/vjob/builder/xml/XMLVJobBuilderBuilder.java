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

package entropy.vjob.builder.xml;

import entropy.PropertiesHelper;
import entropy.vjob.builder.VJobBuilderBuilder;
import entropy.vjob.builder.VJobBuilderBuilderException;
import entropy.vjob.builder.VJobElementBuilder;

import java.io.IOException;

/**
 * A XMLVJobBuilderBuilder using properties.
 *
 * @author Fabien Hermenier
 */
public class XMLVJobBuilderBuilder implements VJobBuilderBuilder {

    public static final String PROPERTIES = "config/XMLVJobs.properties";

    /**
     * The properties file.
     */
    private String file;

    /**
     * The helper to read the properties.
     */
    private PropertiesHelper propHelper;

    /**
     * Build the vjob builder using the default properties file {@link #PROPERTIES}.
     */
    public XMLVJobBuilderBuilder() {
        this(PROPERTIES);
    }

    /**
     * Build the vjob builder using a specific properties file.
     *
     * @param file the properties file
     */
    public XMLVJobBuilderBuilder(String file) {
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
    public XMLVJobBuilder build(VJobElementBuilder eb) throws VJobBuilderBuilderException {
        try {
            propHelper = new PropertiesHelper(this.file);
            XMLConstraintsCatalog c = new ConstraintsCatalogBuilderFromProperties(this.propHelper).build();
            return new XMLVJobBuilder(eb, c);
        } catch (IOException e) {
            throw new VJobBuilderBuilderException(e.getMessage(), e);
        } catch (ConstraintsCalalogBuilderException e) {
            throw new VJobBuilderBuilderException("Unable to build the protobuf vjob builder:" + e.getMessage(), e);
        }
    }
}
