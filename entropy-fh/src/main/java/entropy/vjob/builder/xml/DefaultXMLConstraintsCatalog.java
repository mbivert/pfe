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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of ConstraintCalalog.
 *
 * @author Fabien Hermenier
 */
public class DefaultXMLConstraintsCatalog implements XMLConstraintsCatalog {

    /**
     * The map to get the builder associated to a constraint.
     */
    private Map<String, XMLPlacementConstraintBuilder> builders;

    /**
     * Build a new catalog.
     */
    public DefaultXMLConstraintsCatalog() {
        this.builders = new HashMap<String, XMLPlacementConstraintBuilder>();

    }

    /**
     * Add a constraint builder to the catalog.
     * There must not be another builder with the same identifier in the catalog
     *
     * @param c the constraint to add
     * @return true if the constraintbuilder has been added.
     */
    public boolean add(XMLPlacementConstraintBuilder c) {
        if (this.builders.containsKey(c.getIdentifier())) {
            return false;
        }
        this.builders.put(c.getIdentifier(), c);
        return true;
    }

    @Override
    public Set<String> getAvailableConstraints() {
        return this.builders.keySet();
    }

    @Override
    public XMLPlacementConstraintBuilder getConstraint(String id) {
        return builders.get(id);
    }
}
