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

import entropy.vjob.VJob;
import entropy.vjob.builder.VJobBuilder;
import entropy.vjob.builder.VJobBuilderException;
import entropy.vjob.builder.VJobElementBuilder;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileReader;

/**
 * @author Fabien Hermenier
 */
public class XMLVJobBuilder implements VJobBuilder {

    private VJobElementBuilder eBuilder;

    public static final String SCHEMA_LOCATION = "/entropy/vjob/builder/xml/vjob.xsd";

    private final XMLVJobHandler contentHandler;

    private static final ErrorHandler errorHandler = new XMLVJobErrorHandler();

    private SAXParser parser;

    private XMLReader xmlReader;

    public XMLVJobBuilder(VJobElementBuilder eBuilder, XMLConstraintsCatalog cat) {
        this.eBuilder = eBuilder;
        contentHandler = new XMLVJobHandler(eBuilder, cat);

        SAXParserFactory factory = SAXParserFactory.newInstance();

        factory.setValidating(false);
        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        try {
            factory.setSchema(schemaFactory.newSchema(new Source[]{new StreamSource(getClass().getResourceAsStream(SCHEMA_LOCATION))}));
            parser = factory.newSAXParser();
            xmlReader = parser.getXMLReader();
            xmlReader.setErrorHandler(errorHandler);
            xmlReader.setContentHandler(contentHandler);
        } catch (SAXException e) {
            //Should not happen
            VJob.logger.error("Unable to locate xsd schema to validate XML vjobs: " + e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            //Should not happen
            VJob.logger.error("Unable to configure XML vjobs parser: " + e.getMessage(), e);
        }

    }

    @Override
    public VJob build(File f) throws VJobBuilderException {
        try {
            xmlReader.parse(new InputSource(new FileReader(f)));
            return contentHandler.getVJob();
        } catch (Exception e) {
            throw new VJobBuilderException(e.getMessage(), e);
        }
    }

    @Override
    public VJobElementBuilder getElementBuilder() {
        return this.eBuilder;
    }

    @Override
    public void setElementBuilder(VJobElementBuilder eb) {
        this.eBuilder = eb;
    }

    @Override
    public String getAssociatedExtension() {
        return "xml";
    }
}
