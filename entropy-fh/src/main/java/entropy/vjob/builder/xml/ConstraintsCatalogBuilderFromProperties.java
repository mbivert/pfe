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

import entropy.MissingRequiredPropertyException;
import entropy.PropertiesHelper;
import entropy.vjob.VJob;

/**
 * Build a ConstraintCatalogBuilder that creates catalog using properties.
 *
 * @author Fabien Hermenier
 */
public final class ConstraintsCatalogBuilderFromProperties implements XMLConstraintsCatalogBuilder {

    /**
     * The property to define the constraints to add to the catalog.
     */
    public static final String CATALOG_CONTENTS_PROPERTIES = "constraintsCatalog.load";

    /**
     * The prefix of each contraint to load. Indicates the FQCN of the PBPlacementConstraintBuilder.
     */
    public static final String CONSTRAINT_BUILDER = "constraintsCatalog.location.";

    /**
     * The properties to use.
     */
    private PropertiesHelper props;

    /**
     * Build a new ConstraintsCatalogBuilder using specific properties.
     *
     * @param p the properties to use
     */
    public ConstraintsCatalogBuilderFromProperties(PropertiesHelper p) {
        this.props = p;
    }

    /**
     * Build a catalog.
     *
     * @return the catalog
     * @throws ConstraintsCalalogBuilderException
     *          if an error occurred while creating the catalog.
     */
    @Override
    public XMLConstraintsCatalog build() throws ConstraintsCalalogBuilderException {
        DefaultXMLConstraintsCatalog c = new DefaultXMLConstraintsCatalog();
        if (props.isDefined(CATALOG_CONTENTS_PROPERTIES)) {
            try {
                String list = props.getRequiredProperty(CATALOG_CONTENTS_PROPERTIES);
                String[] constraints = list.split(",");
                for (String constraint : constraints) {
                    String fqcn = props.getRequiredProperty(CONSTRAINT_BUILDER + constraint);
                    Class cl = getClass().getClassLoader().loadClass(fqcn);
                    c.add((XMLPlacementConstraintBuilder) cl.newInstance());
                }
                VJob.logger.debug("Available constraints for XML vjobs " + c.getAvailableConstraints());
            } catch (MissingRequiredPropertyException e) {
                throw new ConstraintsCalalogBuilderException("Unable to build the catalog: " + e.getMessage(), e);
            } catch (ClassNotFoundException e) {
                throw new ConstraintsCalalogBuilderException("Unable to build the catalog: " + e.getMessage(), e);
            } catch (InstantiationException e) {
                throw new ConstraintsCalalogBuilderException("Unable to build the catalog:" + e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new ConstraintsCalalogBuilderException("Unable to build the catalog:" + e.getMessage(), e);
            }
        } else {
            VJob.logger.debug("No constraints to load");
        }
        return c;
    }
}
