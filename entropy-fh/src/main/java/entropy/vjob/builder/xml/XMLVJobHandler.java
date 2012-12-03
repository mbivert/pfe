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

import entropy.configuration.VirtualMachine;
import entropy.vjob.DefaultVJob;
import entropy.vjob.VJob;
import entropy.vjob.builder.VJobElementBuilder;
import gnu.trove.THashMap;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author Fabien Hermenier
 */
public class XMLVJobHandler extends DefaultHandler {

    /**
     * Buffer to read the characters.
     */
    private String buffer;

    private VJob vjob;

    private XMLConstraintsCatalog catalog;

    private VJobElementBuilder eBuilder;

    private Map<String, String> options;

    private String currentVMDeclID;

    private String currentVMDeclTpl;

    private String currentOptionKey;

    private List<Param> params;

    private Param currentParam;

    private String currentConstraintId;

    private Stack<Param> setsStack;

    public XMLVJobHandler(VJobElementBuilder eBuilder, XMLConstraintsCatalog cat) {
        this.eBuilder = eBuilder;
        this.catalog = cat;
    }

    /**
     * Get the parsed template factory.
     *
     * @return a non null template factory.
     */
    public VJob getVJob() {
        return vjob;
    }

    @Override
    public void startDocument() throws SAXException {
        this.options = new THashMap<String, String>();
        this.params = new ArrayList<Param>();
        this.setsStack = new Stack<Param>();
    }

    @Override
    public void endElement(String s, String s1, String s2) throws SAXException {
        if (s2.equals("option")) {
            if (buffer != null && buffer.isEmpty()) {
                buffer = null;
            }
            options.put(currentOptionKey, buffer);
            buffer = null;
            currentOptionKey = null;
        } else if (s2.equals("vm") && currentVMDeclID != null) {
            VirtualMachine vm = eBuilder.matchVirtualMachine(currentVMDeclID, currentVMDeclTpl, options);
            if (vm == null) {
                throw new SAXException("Unable to declare virtual machine '" + currentVMDeclID + "'");
            }
            currentVMDeclID = null;
            options.clear();
            currentOptionKey = null;
            vjob.addVirtualMachine(vm);
        } else if (s2.equals("constraint")) {
            XMLPlacementConstraintBuilder cb = catalog.getConstraint(currentConstraintId);
            if (cb == null) {
                throw new SAXException("Constraint '" + currentConstraintId + "' unknown");
            }
            try {
                vjob.addConstraint(cb.buildConstraint(eBuilder, params));
            } catch (ConstraintBuilderException e) {
                throw new SAXException(e);
            }
        } else if (s2.equals("param")) {
            params.add(currentParam);
            currentParam = null;
        } else if (s2.equals("set")) {
            setsStack.pop();
        }
    }

    @Override
    public void startElement(String s, String s1, String s2, Attributes attributes) throws SAXException {
        if (s2.equals("vjob")) {
            vjob = new DefaultVJob(attributes.getValue("id"));
        } else if (s2.equals(".options")) {
            this.options.clear();
        } else if (s2.equals("option")) {
            currentOptionKey = attributes.getValue("id");
            if (currentOptionKey == null) {
                throw new SAXException("Option key should not be null");
            }
        } else if (s2.equals("constraint")) {
            this.currentConstraintId = attributes.getValue("id");
            this.params.clear();
        } else if (s2.equals("vm")) {
            String id = attributes.getValue("id");
            if (id != null) {
                currentVMDeclID = id;
                currentVMDeclTpl = attributes.getValue("tpl");
            } else {
                String ref = attributes.getValue("ref");
                Param p = new Param(Param.Type.vm, ref);
                if (currentParam == null) {
                    currentParam = p;
                } else {
                    ((List) setsStack.peek().value).add(p);
                }
            }
        } else if (s2.equals("node")) {
            String ref = attributes.getValue("ref");
            Param p = new Param(Param.Type.node, ref);
            if (currentParam == null) {
                currentParam = p;
            } else {
                ((List) setsStack.peek().value).add(p);
            }
        } else if (s2.equals("int")) {
            Param p = new Param(Integer.parseInt(attributes.getValue("value")));
            if (currentParam == null) {
                currentParam = p;
            } else {
                ((List) setsStack.peek().value).add(p);
            }
        } else if (s2.equals("double")) {
            Param p = new Param(Double.parseDouble(attributes.getValue("value")));
            if (currentParam == null) {
                currentParam = p;
            } else {
                ((List) setsStack.peek().value).add(p);
            }
        } else if (s2.equals("string")) {
            Param p = new Param(attributes.getValue("string"));
            if (currentParam == null) {
                currentParam = p;
            } else {
                ((List) setsStack.peek().value).add(p);
            }
        } else if (s2.equals("set")) {
            Param p = new Param();
            p.type = Param.Type.set;
            p.value = new ArrayList<Param>();
            if (currentParam == null) {
                currentParam = p;
                setsStack.push(p);
            } else {
                ((List) setsStack.peek().value).add(p);
                setsStack.push(p);
            }
        }

    }

    @Override
    public void characters(char[] chars, int i, int i1) throws SAXException {
        buffer = new String(chars, i, i1);
    }
}
