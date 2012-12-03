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

package entropy.plan.durationEvaluator;

import entropy.configuration.ManagedElement;
import org.antlr.runtime.Token;

/**
 * @author Fabien Hermenier
 */
public class Operator extends EvaluatorTree {

    private int type;

    public Operator(Token p) {
        super(p);
        this.type = p.getType();
    }

    @Override
    public double evaluate(ManagedElement e) {
        double f = ((EvaluatorTree) getChild(0)).evaluate(e);
        double s = ((EvaluatorTree) getChild(1)).evaluate(e);
        switch (type) {
            case ANTLRDurationEvaluator2Parser.DIV:
                return f / s;
            case ANTLRDurationEvaluator2Parser.MULTIPLY:
                return f * s;
            case ANTLRDurationEvaluator2Parser.MINUS:
                return f - s;
            case ANTLRDurationEvaluator2Parser.PLUS:
                return f + s;
            case ANTLRDurationEvaluator2Parser.POW:
                return Math.pow(f, s);
        }
        throw new UnsupportedOperationException();
    }
}
