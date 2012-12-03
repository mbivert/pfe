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

package entropy.vjob.builder.plasma;

import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;
import entropy.vjob.builder.VJobElementBuilder;
import org.antlr.runtime.Token;

/**
 * A Tree parser to identify a virtual machine or a node.
 *
 * @author Fabien Hermenier
 */
public class ValTree extends VJobTree {

    /**
     * The builder to make the element.
     */
    private VJobElementBuilder elemBuilder;

    /**
     * Make a new parser.
     *
     * @param t    the token to analyze
     * @param errs the errors to report
     * @param eb   the builder to make the element
     */
    public ValTree(Token t, SemanticErrors errs, VJobElementBuilder eb) {
        super(t, errs);
        this.elemBuilder = eb;
    }

    @Override
    public Content go(VJobTree parent) {
        String lbl = token.getText();
        Node n = elemBuilder.matchAsNode(lbl);
        if (n != null) {
            return new Content(Content.Type.node, n);
        }
        VirtualMachine vm = elemBuilder.matchVirtualMachine(lbl);
        if (vm != null) {
            return new Content(Content.Type.vm, vm);
        }
        return ignoreError("element " + lbl + " is not a virtual machine nor a node");
    }
}
