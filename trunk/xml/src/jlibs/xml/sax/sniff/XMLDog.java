/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package jlibs.xml.sax.sniff;

import jlibs.xml.DefaultNamespaceContext;
import jlibs.xml.sax.sniff.engine.Sniffer;
import jlibs.xml.sax.sniff.model.Root;
import jlibs.xml.sax.sniff.parser.JaxenParser;
import org.jaxen.saxpath.SAXPathException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathVariableResolver;
import javax.xml.xpath.XPathFunctionResolver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Santhosh Kumar T
 */
public class XMLDog implements Debuggable{
    private Root root;

    public XMLDog(NamespaceContext nsContext, XPathVariableResolver variableResolver, XPathFunctionResolver functionResolver){
        root = new Root(nsContext, variableResolver, functionResolver);
    }

    public XPath add(String xpath) throws SAXPathException{
        return add(xpath, -1);
    }

    private List<XPath> xpaths = new ArrayList<XPath>();
    public XPath add(String xpath, int minHits) throws SAXPathException{
        XPath compiledXPath = new JaxenParser(root).parse(xpath);
        compiledXPath.setMinHits(minHits);
        xpaths.add(compiledXPath);
        
        return compiledXPath;
    }

    public XPathResults sniff(InputSource source) throws ParserConfigurationException, SAXException, IOException{
        if(debug)
            root.print();

        Root _root = root;
        List<XPath> _xpaths = this.xpaths;

        boolean clone = true;
        synchronized(this){
            clone = _root.isUsing();
            if(!clone)
                _root.setUsing(true);
        }

        if(clone){
            if(debug)
                debugger.println("cloning xpaths...");
            
            _root = new Root(root.nsContext, root.variableResolver, root.functionResolver);
            _xpaths = new ArrayList<XPath>(xpaths.size());
            for(XPath xpath: xpaths){
                XPath _xpath = xpath.copy(_root);
                _xpath.setMinHits(xpath.minHits);
                _xpaths.add(_xpath);
            }
        }

        try{
            DefaultNamespaceContext nsContext = new Sniffer(_root).sniff(source);
            _root.parsingDone();
            return new XPathResults(nsContext, _xpaths);
        }finally{
            if(!clone){
                if(debug)
                    debugger.println("resetting xpaths...");

                for(XPath xpath: _xpaths)
                    xpath.expr.reset();
                root.setUsing(false);
            }
        }
    }
}
