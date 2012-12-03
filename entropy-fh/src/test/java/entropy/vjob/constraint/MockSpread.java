/*
 * Copyright (c) 2010 Ecole des Mines de Nantes.
 *
 *      This file is part of Entropy.
 *
 *      Entropy is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      Entropy is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package entropy.vjob.constraint;

import entropy.configuration.ManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.vjob.Spread;
import entropy.vjob.builder.protobuf.PBVJob;

/**
 * A simple mock object to test Spread.
 *
 * @author Fabien Hermenier
 */
public class MockSpread extends Spread {

    MockSpread(ManagedElementSet<VirtualMachine> vms) {
        super(vms);
    }


    @Override
    public String toString() {
        return "mSpread(" + vms + ");";
    }

    @Override
    public void inject(ReconfigurationProblem plan) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toXML() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PBVJob.vjob.Constraint toProtobuf() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
