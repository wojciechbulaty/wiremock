/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.extension.responsetemplating.helpers;

import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.common.ListOrSingle;
import com.github.tomakehurst.wiremock.common.xml.*;
import com.github.tomakehurst.wiremock.extension.responsetemplating.RenderCache;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static javax.xml.xpath.XPathConstants.NODE;

/**
 * This class uses javax.xml.xpath.* for reading a xml via xPath so that the result can be used for response
 * templating.
 */
public class HandlebarsXPathHelper extends HandlebarsHelper<String> {

    @Override
    public Object apply(final String inputXml, final Options options) throws IOException {
        if (inputXml == null ) {
            return "";
        }

        if (options.param(0, null) == null) {
            return handleError("The XPath expression cannot be empty");
        }

        final String xPathInput = options.param(0);

        XmlDocument xmlDocument;
        try {
            xmlDocument = getXmlDocument(inputXml, options);
        } catch (XmlException e) {
            return handleError(inputXml + " is not valid XML");
        }

        try {
            XmlNode xmlNode = getXmlNode(getXPathPrefix() + xPathInput, xmlDocument, options);

            if (xmlNode == null) {
                return "";
            }

            return xmlNode.toString();
        } catch (XPathException e) {
            return handleError(xPathInput + " is not a valid XPath expression", e);
        }
    }

    private XmlNode getXmlNode(String xPathExpression, XmlDocument doc, Options options) {
        RenderCache renderCache = getRenderCache(options);
        RenderCache.Key cacheKey = RenderCache.Key.keyFor(XmlDocument.class, xPathExpression, doc);
        XmlNode node = renderCache.get(cacheKey);

        if (node == null) {
            ListOrSingle<XmlNode> nodes = doc.findNodes(xPathExpression);
            node = nodes.isEmpty() ? null : nodes.getFirst();
            renderCache.put(cacheKey, node);
        }

        return node;
    }

    private XmlDocument getXmlDocument(String xml, Options options) {
        RenderCache renderCache = getRenderCache(options);
        RenderCache.Key cacheKey = RenderCache.Key.keyFor(XmlDocument.class, xml);
        XmlDocument document = renderCache.get(cacheKey);
        if (document == null) {
            document = Xml.parse(xml);
            renderCache.put(cacheKey, document);
        }

        return document;
    }

    /**
     * No prefix by default. It allows to extend this class with a specified prefix. Just overwrite this method to do
     * so.
     *
     * @return a prefix which will be applied before the specified xpath.
     */
    protected String getXPathPrefix() {
        return "";
    }
}
