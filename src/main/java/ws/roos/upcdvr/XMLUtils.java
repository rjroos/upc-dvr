package ws.roos.upcdvr;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * XMLUtils
 *
 * @version $Id$
 */
public class XMLUtils {
	
	private final static String CHAR_NBSP = "\u00A0";

	public static String getContent(Node n) {
		assertNotNull("node == null", n);
		StringBuilder result = new StringBuilder();
		if (n.getNodeType() == Node.TEXT_NODE) {
			result.append(n.getNodeValue());
		}
		NodeList childNodes = n.getChildNodes();
		for (int i = 0 ; i < childNodes.getLength() ; i++) {
			Node n2 = childNodes.item(i);
			result.append(getContent(n2));
		}
		
		String tmp = result.toString();
		return tmp.replaceAll(CHAR_NBSP, " ");
	}
	
	public static int getContentAsInt(Node n) {
		String content = getContent(n).trim();
		try {
			return Integer.parseInt(content);
		} catch (NumberFormatException e) {
			throw new IllegalStateException(
					"Cannot get number from '" + content + "' (" + format(n, 0) + ")", e);
		}
	}

	public static String format(Node n, int indentLevel) {
		String indent = "";
		for (int i = 0 ; i < indentLevel ; i++) {
			indent += "\t";
		}
		
		StringBuilder result = new StringBuilder();
		result.append(indent);
		if (n.getNodeType() == Node.ELEMENT_NODE) {
			result.append("<");
			result.append(n.getNodeName());
			NamedNodeMap attrs = n.getAttributes();
			for (int i = 0; i < attrs.getLength(); i++) {
				Node attr = attrs.item(i);
				result.append(" ");
				result.append(attr.getNodeName());
				result.append("=\"");
				result.append(attr.getNodeValue());
				result.append("\"");
			}
			result.append(">\n");
		}
		
		if (n.getNodeType() == Node.TEXT_NODE) {
			String str = n.getNodeValue();
			result.append(str + "\n");
		}
		
		NodeList childNodes = n.getChildNodes();
		for (int i = 0 ; i < childNodes.getLength() ; i++) {
			Node n2 = childNodes.item(i);
			if (isEmptyTextNode(n2)) {
				continue;
			}
			result.append(format(n2, indentLevel + 1));
		}
		
		if (n.getNodeType() == Node.ELEMENT_NODE) {
			result.append(indent);
			result.append("</");
			result.append(n.getNodeName());
			result.append(">\n");
		}
		return result.toString();
	}

	private static boolean isEmptyTextNode(Node node) {
		if (node.getNodeType() == Node.TEXT_NODE) {
			String str = node.getNodeValue();
			if (str == null || str.trim().length() == 0) {
				return true;
			}
		}
		return false;
	}

	public static String getXPathValue(Node node, String expr) {
		assertNotNull("node == null", node);
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			return (String) xpath.evaluate(expr, node, XPathConstants.STRING);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		} 
	}
	
	public static Document parseDocument(InputStream isXML, InputStream isSchema) {
		assertNotNull("xml == null", isXML);
		assertNotNull("schema == null", isSchema);
		try {
			DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
			dFactory.setNamespaceAware(true);
			DocumentBuilder parser = dFactory.newDocumentBuilder();
			Document document = parser.parse(isXML);

			SchemaFactory factory = SchemaFactory.newInstance(
					XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Source schemaFile = new StreamSource(isSchema);
			Schema schema = factory.newSchema(schemaFile);
			
			Validator validator = schema.newValidator();
			validator.validate(new DOMSource(document));
			
			return document;
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Document htmlToDocument(String sResult) {
		XMLReader reader = new Parser();
		try {
			reader.setFeature(Parser.namespacesFeature, false);
			reader.setFeature(Parser.namespacePrefixesFeature, false);
	
			TransformerFactory tfact = TransformerFactory.newInstance();
			Transformer transformer = tfact.newTransformer();
	
			DOMResult result = new DOMResult();
			StringReader sr = new StringReader(sResult);
			InputSource inputSource = new InputSource(sr);
			SAXSource saxSource = new SAXSource(reader, inputSource);
			transformer.transform(saxSource, result);
	
			return (Document) result.getNode();
		} catch (SAXNotRecognizedException e) {
			throw new RuntimeException(e);
		} catch (SAXNotSupportedException e) {
			throw new RuntimeException(e);
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException(e);
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<Element> filter(NodeList nl, String attr, String value) {
		List<Element> result = new ArrayList<Element>();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (!(n instanceof Element)) {
				continue;
			}
			Element e = (Element) n;
			if (!value.equals(e.getAttribute(attr))) {
				continue;
			}
			result.add(e);
		}
		return result;
	}

	/**
	 * Dump element leesbaar op stdout.
	 */
	public static void dump(Node e) {
		if (e == null) {
			System.out.println("NULL");
			return;
		}
		System.out.println("### BEGIN DUMP OF " + e);
		System.out.println(format(e, -1));
		System.out.println("### END DUMP OF " + e);
	}

	public static Element getXPathNode(Node node, String expr) {
		assertNotNull("node == null", node);
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			return (Element) xpath.evaluate(expr, node, XPathConstants.NODE);
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		} 
	}

	public static List<Element> getXPathList(Node node, String expr) {
		assertNotNull("node == null", node);
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		List<Element> result = new ArrayList<Element>();
		try {
			NodeList nl = (NodeList) xpath.evaluate(
					expr, node, XPathConstants.NODESET);
			for (int i = 0 ; i < nl.getLength() ; i++) {
				result.add((Element) nl.item(i));
			}
			return result;
		} catch (XPathExpressionException e) {
			throw new RuntimeException(e);
		} 
	}

	public static void dump(List<Element> divs) {
		int count = 0;
		for (Element e : divs) {
			System.out.println(count++);
			dump(e);
		}
	}

	public static Document newDocument() {
		try {
			DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
			dFactory.setNamespaceAware(true);
			DocumentBuilder builder = dFactory.newDocumentBuilder();
			return builder.newDocument();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	public static String toString(Document doc) {
		try {
			StringWriter sw = new StringWriter();
			Source source = new DOMSource(doc);
			Result result = new StreamResult(sw);

			TransformerFactory tf = TransformerFactory.newInstance();
			tf.setAttribute("indent-number", new Integer(4));
			Transformer xformer = tf.newTransformer();
			xformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
			xformer.setOutputProperty(OutputKeys.INDENT, "yes");
			xformer.transform(source, result);
			return sw.toString();
		} catch (TransformerException e) {
			throw new RuntimeException(e);
        }
	}

	public static Element getChildNode(
			Element parent, String name) {
		List<Element> childNodes = getChildNodes(parent, name);
		if (childNodes.isEmpty()) {
			return null;
		}
		return childNodes.get(0);
	}

	public static List<Element> getChildNodes(
			Element parent, String name) {
		List<Element> result = new ArrayList<Element>();
		NodeList childNodes = parent.getChildNodes();
		for (int i = 0 ; i < childNodes.getLength() ; i++) {
			Node n = childNodes.item(i);
			if (name.equals(n.getNodeName())) {
				result.add((Element) n);
			}
		}
		return result;
	}

	private static void assertNotNull(String msg, Object o) {
		if (o == null) {
			throw new IllegalStateException(msg);
		}
	}
}
