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

import entropy.configuration.Configuration;
import entropy.vjob.VJob;
import gnu.trove.THashMap;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * @author Fabien Hermenier
 */
public class DefaultVJobBuilderFactory implements VJobBuilderFactory {


    private THashMap<String, VJobBuilder> builders;

    private Configuration cfg;

    private VJobElementBuilder eb;

    public DefaultVJobBuilderFactory() {
        this.builders = new THashMap<String, VJobBuilder>();
    }

    public void add(VJobBuilder b) {
        this.builders.put(b.getAssociatedExtension(), b);
    }

    @Override
    public VJob build(String path) throws IOException, VJobBuilderException {
        String ext = path.substring(path.lastIndexOf('.') + 1, path.length());
        VJobBuilder b = builders.get(ext);
        if (b == null) {
            throw new VJobBuilderException("Unable to find a builder for '" + path + "'");
        }
        if (eb != null) {
            b.setElementBuilder(eb);
        }
        b.getElementBuilder().useConfiguration(cfg);
        return b.build(new File(path));
    }

    @Override
    public Set<String> getManagedExtensions() {
        return builders.keySet();
    }

    @Override
    public void useConfiguration(Configuration cfg) {
        this.cfg = cfg;
    }

    @Override
    public void setVJobElementBuilder(VJobElementBuilder eb) {
        this.eb = eb;
    }
}
