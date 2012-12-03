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

package entropy.platform.any;

import entropy.platform.Platform;
import gnu.trove.THashMap;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A simili default platform for homogeneous environment. Used as a stub
 *
 * @author Fabien Hermenier
 */
public class AnyPlatform implements Platform {

    private THashMap<String, String> opts;

    public AnyPlatform() {
        this.opts = new THashMap<String, String>();
    }

    @Override
    public String getIdentifier() {
        return "any";
    }

    @Override
    public void addOption(String opt) {
        this.opts.put(opt, null);
    }

    @Override
    public boolean checkOption(String opt) {
        return this.opts.containsKey(opt);
    }

    @Override
    public Set<String> getOptions() {
        return this.opts.keySet();
    }

    @Override
    public void addOption(String key, String value) {
        this.opts.put(key, value);
    }

    @Override
    public String getOption(String k) {
        return this.opts.get(k);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        return this.getClass().equals(o.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() + 31 * "any".hashCode();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("any");
        if (!opts.isEmpty()) {
            b.append('[');
            for (Iterator<Map.Entry<String, String>> ite = opts.entrySet().iterator(); ite.hasNext(); ) {
                Map.Entry<String, String> e = ite.next();
                b.append(e.getKey());
                String v = e.getValue();
                if (v != null) {
                    b.append('=').append(v);
                }
                if (ite.hasNext()) {
                    b.append(',');
                }
            }
            b.append(']');
        }
        return b.toString();
    }

    @Override
    public AnyPlatform clone() {
        AnyPlatform p = new AnyPlatform();
        for (Map.Entry<String, String> e : opts.entrySet()) {
            p.addOption(e.getKey(), e.getValue());
        }
        return p;
    }
}
