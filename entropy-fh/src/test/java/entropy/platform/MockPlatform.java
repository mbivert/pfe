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

import java.util.Set;

/**
 * @author Fabien Hermenier
 */
public class MockPlatform implements Platform {

    private String id;

    public MockPlatform(String id) {
        this.id = id;
    }

    @Override
    public String getIdentifier() {
        return id;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addOption(String opt) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean checkOption(String opt) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<String> getOptions() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addOption(String key, String value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getOption(String k) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Platform clone() {
        return new MockPlatform(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o.getClass().equals(getClass()) && ((Platform) o).getIdentifier().equals(getIdentifier());
    }
}
