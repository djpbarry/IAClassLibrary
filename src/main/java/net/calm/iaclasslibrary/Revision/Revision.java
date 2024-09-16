package net.calm.iaclasslibrary.Revision;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class Revision {
	public static String getVersionFromPom(String filePath) {
		try {
			// Set up DocumentBuilder to parse XML
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true); // Make it namespace aware
			DocumentBuilder builder = factory.newDocumentBuilder();

			// Parse the pom.xml
			Document document = builder.parse(new File(filePath));
			document.getDocumentElement().normalize();

			// Look for the <version> tag under the project element, excluding parent and plugins
			NodeList versionList = document.getElementsByTagNameNS("*", "version");

			for (int i = 0; i < versionList.getLength(); i++) {
				Node versionNode = versionList.item(i);
				// Check if it's the project's version (ignoring parent)
				if (versionNode.getParentNode().getNodeName().equals("project")) {
					return versionNode.getTextContent();
				}
			}

			return "Version not found!";
		} catch (Exception e) {
			e.printStackTrace();
			return "Error reading pom.xml!";
		}
	}
}
