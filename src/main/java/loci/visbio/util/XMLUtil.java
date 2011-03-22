//
// XMLUtil.java
//

/*
VisBio application for visualization of multidimensional biological
image data. Copyright (C) 2002-@year@ Curtis Rueden and Abraham Sorber.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.visbio.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * XMLUtil contains useful functions for manipulating DOMs.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/visbio/src/loci/visbio/util/XMLUtil.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/visbio/src/loci/visbio/util/XMLUtil.java">SVN</a></dd></dl>
 */
public final class XMLUtil {

  // -- Static fields --

  /** Document builder for creating DOMs. */
  protected static DocumentBuilder docBuilder;

  // -- Constructor --

  private XMLUtil() { }

  // -- Utility methods --

  /** Creates a new DOM. */
  public static Document createDocument(String rootName) {
    if (docBuilder == null) {
      try {
        docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      }
      catch (ParserConfigurationException exc) {
        exc.printStackTrace();
        return null;
      }
    }
    Document doc = docBuilder.newDocument();
    if (rootName != null) doc.appendChild(doc.createElement(rootName));
    return doc;
  }

  /** Parses a DOM from the given XML file on disk. */
  public static Document parseXML(File file) {
    try { return parseXML(new FileInputStream(file)); }
    catch (FileNotFoundException exc) { exc.printStackTrace(); }
    return null;
  }

  /** Parses a DOM from the given XML string. */
  public static Document parseXML(String xml) {
    return parseXML(new ByteArrayInputStream(xml.getBytes()));
  }

  /** Parses a DOM from the given XML input stream. */
  public static Document parseXML(InputStream is) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      return db == null ? null : db.parse(is);
    }
    catch (IOException exc) { exc.printStackTrace(); }
    catch (ParserConfigurationException exc) { exc.printStackTrace(); }
    catch (SAXException exc) { exc.printStackTrace(); }
    return null;
  }

  /** Writes the given DOM to the specified file on disk. */
  public static void writeXML(File file, Document doc) {
    try {
      FileOutputStream out = new FileOutputStream(file);
      writeXML(out, doc);
      out.close();
    }
    catch (IOException exc) { exc.printStackTrace(); }
  }

  /**
   * Writes the given DOM to a string.
   * @return The string to which the DOM was written.
   */
  public static String writeXML(Document doc) {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    writeXML(os, doc);
    return os.toString();
  }

  /** Writes the given DOM to the specified output stream. */
  public static void writeXML(OutputStream os, Document doc) {
    try {
      TransformerFactory transformFactory = TransformerFactory.newInstance();
      Transformer idTransform = transformFactory.newTransformer();
      Source input = new DOMSource(doc);
      Result output = new StreamResult(os);
      idTransform.transform(input, output);
    }
    catch (TransformerException exc) { exc.printStackTrace(); }
    // append newline to end of output
    try { os.write(System.getProperty("line.separator").getBytes()); }
    catch (IOException exc) { exc.printStackTrace(); }
  }

  /**
   * Appends a child element with the given name to the specified DOM element.
   */
  public static Element createChild(Element el, String name) {
    Element child = el.getOwnerDocument().createElement(name);
    el.appendChild(child);
    return child;
  }

  /**
   * Appends a text node with the given information
   * to the specified DOM element.
   */
  public static Text createText(Element el, String info) {
    Text text = el.getOwnerDocument().createTextNode(info);
    el.appendChild(text);
    return text;
  }

  /**
   * Retrieves the given DOM element's first child element with the specified
   * name. If name is null, the first child of any type is returned.
   */
  public static Element getFirstChild(Element el, String name) {
    NodeList nodes = el.getChildNodes();
    int len = nodes.getLength();
    for (int i=0; i<len; i++) {
      Node node = nodes.item(i);
      if (!(node instanceof Element)) continue;
      Element e = (Element) node;
      if (name == null || e.getTagName().equals(name)) return e;
    }
    return null;
  }

  /**
   * Retrieves the given DOM element's child elements with the specified name.
   * If name is null, all children are retrieved.
   */
  public static Element[] getChildren(Element el, String name) {
    Vector v = new Vector();
    NodeList nodes = el.getChildNodes();
    int len = nodes.getLength();
    for (int i=0; i<len; i++) {
      Node node = nodes.item(i);
      if (!(node instanceof Element)) continue;
      Element e = (Element) node;
      if (name == null || e.getTagName().equals(name)) v.add(e);
    }
    Element[] els = new Element[v.size()];
    v.copyInto(els);
    return els;
  }

  /** Gets the text information associated with the given DOM element. */
  public static String getText(Element el) {
    NodeList nodes = el.getChildNodes();
    int len = nodes.getLength();
    for (int i=0; i<len; i++) {
      Node node = nodes.item(i);
      if ("#text".equals(node.getNodeName())) return node.getNodeValue();
    }
    return null;
  }

  /** XSLT Stylesheet for Forward project */
  public static String transformXML(String xml, String pathToStylesheet) {
    Templates stylesheet = XMLTools.getStylesheet(pathToStylesheet, null);
    try {
      return XMLTools.transformXML(xml, stylesheet);
    }
    catch (IOException exc) {
      return null;
    }
  }
}

