/*
 * Copyright (c) Fabien Hermenier
 *
 * This file is part of Entropy.
 *
 * Entropy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Entropy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package entropy.configuration;


import java.util.*;

/**
 * Basic implementation of a ManagedElementSet.
 * {@code add()},{@code addAll()}, {@code contains()}, , {@code get()}, , {@code set()} operations
 * are performed in an almost constant time.
 * {@code remove()}, {@code removeAll()} operations are in a constant time if the element is not in the set.
 * O(n) otherwise
 *
 * @author Fabien Hermenier
 */
public class SimpleManagedElementSet<E extends ManagedElement> extends ArrayList<E> implements ManagedElementSet<E>, Cloneable {

    private Map<String, E> map;

    /**
     * Make a singleton.
     *
     * @param e the element in the singleton.
     */
    public SimpleManagedElementSet(E e) {
        this();
        this.add(e);
    }

    /**
     * Make an empty set.
     */
    public SimpleManagedElementSet() {
        map = new HashMap<String, E>();
    }

    @Override
    public E get(String name) {
        return map.get(name);
    }

    @Override
    public boolean contains(E o) {
        return map.containsKey(o.getName());
    }

    @Override
    public boolean remove(E o) {
        return map.remove(o.getName()) != null && super.remove(o);
    }

    @Override
    public final boolean add(E e) {
        String k = e.getName();
        if (!map.containsKey(k)) {
            map.put(k, e);
            return super.add(e);
        }
        return false;
    }

    @Override
    public void add(int i, E e) {
        String k = e.getName();
        if (!map.containsKey(k)) {
            map.put(k, e);
            super.add(i, e);
        }
    }

    @Override
    public boolean addAll(ManagedElementSet<E> elems) {
        boolean ret = false;
        for (E e : elems) {
            ret |= this.add(e);
        }
        return ret;
    }

    @Override
    public boolean removeAll(ManagedElementSet<E> elems) {
        boolean ret = false;
        for (E e : elems) {
            ret |= this.remove(e);
        }
        return ret;
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        map.clear();
        super.clear();
    }

    @Override
    public E set(int i, E e) {
        E old = super.set(i, e);
        String h = e.getName();
        //e is necessarily in. It may be just a position change, so if is just a position change, no need to update the map
        //however, need to remove the old value iff it is different from the new one
        if (this.map.containsKey(h) && !h.equals(old.getName())) {
            map.remove(old.getName());
        } else {
            map.put(h, e);
        }
        return old;
    }

    @Override
    public boolean contains(Object o) {
        try {
            E x = (E) o;
            return map.containsKey(x.getName());
        } catch (ClassCastException e) {

        }
        return false;

    }

    @Override
    public boolean containsAll(ManagedElementSet<E> elems) {
        for (E e : elems) {
            if (!this.contains(e)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public boolean retainAll(ManagedElementSet<E> elems) {
        int i = 0;
        boolean ret = false;
        while (i < size()) {
            E o = get(i);
            if (!elems.contains(o)) {
                this.remove(o);
                ret = true;
            } else {
                i++;
            }
        }
        return ret;
    }

    @Override
    public ManagedElementSet<E> clone() {
        ManagedElementSet<E> copy = new SimpleManagedElementSet<E>();
        for (E e : this) {
            copy.add(e);
        }
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("{");
        for (Iterator<E> ite = this.iterator(); ite.hasNext(); ) {
            E e = ite.next();
            b.append(e.getName());
            if (ite.hasNext()) {
                b.append(", ");
            }
        }
        return b.append("}").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o instanceof ManagedElementSet) {
            //Cannot use super.equals() has we do not consider ordering
            ManagedElementSet<E> ref = (ManagedElementSet<E>) o;
            if (ref.size() != this.size()) {
                return false;
            }
            for (E e : ref) {
                if (!contains(e)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public ManagedElementSet<E> minus(ManagedElementSet<E> elems) {
        ManagedElementSet<E> res = new SimpleManagedElementSet<E>();
        for (E e : this) {
            if (!elems.contains(e)) {
                res.add(e);
            }
        }
        return res;
    }
}
