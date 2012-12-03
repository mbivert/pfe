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

package entropy.platform;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Fabien Hermenier
 */
public class DefaultPlatformFactory implements PlatformFactory {

    private Map<String, Platform> plts;

    public DefaultPlatformFactory() {
        this.plts = new HashMap<String, Platform>();
    }

    public void addPlatform(Platform p) {
        this.plts.put(p.getIdentifier(), p);
    }

    @Override
    public Set<String> getAvailables() {
        return plts.keySet();
    }

    @Override
    public Platform getPlatform(String k) {
        return plts.get(k);
    }
}
