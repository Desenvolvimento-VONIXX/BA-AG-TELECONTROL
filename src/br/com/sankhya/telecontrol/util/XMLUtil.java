package br.com.sankhya.telecontrol.util;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class XMLUtil {
	
	/**
	 * Converte uma String em objeto que representa o XML.
	 * 
	 * @param c
	 * @param s
	 * @return
	 * @throws JAXBException
	 * @throws PropertyException
	 */
	public static Object stringToXml(Class<?> c, String s) throws JAXBException, PropertyException {
		JAXBContext jaxbContext = JAXBContext.newInstance(c);

		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

		return jaxbUnmarshaller.unmarshal(new InputSource(new StringReader(s)));
	}
	
	/**
	 * Converte um objeto que representa XML numa String.
	 * 
	 * @param o
	 * @return
	 * @throws JAXBException
	 * @throws PropertyException
	 */
	public static String xmlToString(Object o) throws JAXBException, PropertyException {
		JAXBContext jaxbContext = JAXBContext.newInstance(o.getClass());
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

		StringWriter sw = new StringWriter();
		jaxbMarshaller.marshal(o, sw);

		return sw.toString();
	}

	public static String xmlToString(Document doc) throws TransformerFactoryConfigurationError, TransformerException {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);
		String xmlString = result.getWriter().toString();
		return xmlString;
	}
}
