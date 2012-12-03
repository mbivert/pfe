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

import java.util.Iterator;
import java.util.List;

/**
 * @author Fabien Hermenier
 */
public class Param {

    public static enum Type {set, integer, real, vm, node, string}

    public Type type;

    public Object value;

    public Param(Type t, String ref) {
        this.type = t;
        this.value = ref;
    }

    public Param(int val) {
        this.type = Type.integer;
        this.value = val;
    }

    public Param(String val) {
        this.type = Type.string;
        this.value = val;
    }

    public Param(double val) {
        this.type = Type.real;
        this.value = val;
    }

    public Param() {
    }

    public String toString() {
        if (type != null) {
            switch (type) {
                case set:
                    StringBuilder b = new StringBuilder("{");
                    List l = (List) value;
                    for (Iterator ite = l.iterator(); ite.hasNext(); ) {
                        b.append(ite.next());
                        if (ite.hasNext()) {
                            b.append(", ");
                        }
                    }
                    b.append("}");
                    return b.toString();
                default:
                    return value.toString();
            }
        }
        return "null";
    }
}
