/*
 * Copyright 2009 Northwestern University
 *
 * Licensed under the Educational Community License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package edu.northwestern.jcr.adapter.xtf.util;

import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.io.ByteArrayInputStream;

import java.util.List;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.InputSource;

/**
 * Very basic utility for applying an XPath epxression to an xml file and 
 * printing information about the execution of the XPath object and the 
 * nodes it finds.
 * Takes 2 arguments:
 *     (1) an xml filename
 *     (2) an XPath expression to apply to the file
 * Examples:
 *     java ApplyXPath foo.xml /
 *     java ApplyXPath foo.xml /doc/name[1]/@last
 *
 * @author Xin Xiang
 */
public class ApplyXPath
{
	/**
	 * The constructor.
	 */
	public ApplyXPath()
	{

	}

	/** 
	 * Process input args and execute the XPath and return an
	 * array of string.
	 * The xpath needs to point to a text node.
	 * @param filename name of the XML file
	 * @param xpath the XPath expression
	 * @return the nodes as a result of the evaluation
	 */
	public String [] evaluateFile(String filename, String xpath)
		throws Exception
	{
		if ((filename == null) || (filename.length() == 0)
			|| (xpath == null) || (xpath.length() == 0)) {
			System.out.println("Bad input args: " + filename + ", " + xpath);

			return null;
		}

		// Tell that we're loading classes and parsing, so the time it 
		// takes to do this doesn't get confused with the time to do 
		// the actual query and serialization.
		System.out.println("Loading classes, parsing "+ filename +
						   ", and setting up serializer");
      
		// Set up a DOM tree to query.
		InputSource in = new InputSource(new FileInputStream(filename));

		return evaluateXPath(in, xpath);
	}

	/** 
	 * Process input args and execute the XPath and return an
	 * array of string.
	 * The xpath needs to point to a text node.
	 * @param xml the String content of the XML
	 * @param xpath the XPath expression
	 * @return the nodes as a result of the evaluation
	 */
	public String [] evaluateString(String xml, String xpath)
		throws Exception
	{
		// Tell that we're loading classes and parsing, so the time it 
		// takes to do this doesn't get confused with the time to do 
		// the actual query and serialization.
		System.out.println("Loading classes, parsing the XML" + 
						   ", and setting up serializer");
      
		// Set up a DOM tree to query.
		InputSource in = 
			new InputSource(new ByteArrayInputStream(xml.getBytes("UTF-8")));

		return evaluateXPath(in, xpath);
	}

	public String [] evaluateXPath(InputSource in, String xpath) 
		throws Exception
	{
		List resultList;

		DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
		dfactory.setNamespaceAware(false);
		Document doc = dfactory.newDocumentBuilder().parse(in);
      
		// Set up an identity transformer to use as serializer.
		Transformer serializer = 
			TransformerFactory.newInstance().newTransformer();
		serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

		// Use the simple XPath API to select a nodeIterator.
		System.out.println("Querying DOM using " + xpath);
		NodeIterator nl = XPathAPI.selectNodeIterator(doc, xpath);
                  
		Node n;
		resultList = new ArrayList<String>();
		while ((n = nl.nextNode())!= null) {         
			if (isTextNode(n)) {
				// DOM may have more than one node corresponding to a 
				// single XPath text node.  Coalesce all contiguous text nodes
				// at this level
				StringBuffer sb = new StringBuffer(n.getNodeValue());
				for (Node nn = n.getNextSibling(); 
					 isTextNode(nn);
					 nn = nn.getNextSibling()) {
					sb.append(nn.getNodeValue());
				}
				resultList.add(sb.toString());
			}
			else {
				// attribute value
				// How to write to a string ?
				StringWriter stringWriter = new StringWriter();
				serializer.transform(new DOMSource(n), 
									 // new StreamResult(new OutputStreamWriter(System.out)));
									 new StreamResult(stringWriter));
				stringWriter.flush();
				resultList.add(stringWriter.toString());
			}
		}

		return (String []) resultList.toArray(new String[0]);
	}

	/** 
	 * Process input args and execute the XPath and write
	 * the result to a file.
	 * The xpath must not point to a text node.
	 * @param filename String
	 * @param xpath String
	 */
	public void writeFile(String filename, String xpath, String tmpfilename)
		throws Exception
	{
		if ((filename == null) || (filename.length() == 0)
			|| (xpath == null) || (xpath.length() == 0)) {
			System.out.println("Bad input args: " + filename + ", " + xpath);
		}

		// Tell that we're loading classes and parsing, so the time it 
		// takes to do this doesn't get confused with the time to do 
		// the actual query and serialization.
		System.out.println("Loading classes, parsing "+ filename +
						   ", and setting up serializer");
      
		// Set up a DOM tree to query.
		InputSource in = new InputSource(new FileInputStream(filename));
		DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
		dfactory.setNamespaceAware(false);
		Document doc = dfactory.newDocumentBuilder().parse(in);
      
		// Set up an identity transformer to use as serializer.
		Transformer serializer = 
			TransformerFactory.newInstance().newTransformer();
		serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

		// Use the simple XPath API to select a nodeIterator.
		System.out.println("Querying DOM using " + xpath);
		NodeIterator nl = XPathAPI.selectNodeIterator(doc, xpath);
                  
		Node n;
		while ((n = nl.nextNode())!= null) {         
			if (isTextNode(n)) {
				// DOM may have more than one node corresponding to a 
				// single XPath text node.  Coalesce all contiguous text nodes
				// at this level
				// StringBuffer sb = new StringBuffer(n.getNodeValue());
				// for (Node nn = n.getNextSibling(); 
				// 	 isTextNode(nn);
				// 	 nn = nn.getNextSibling()) {
				// 	sb.append(nn.getNodeValue());
				// }
				// vector.add(sb.toString());
			}
			else {
				OutputStreamWriter osw =
					new OutputStreamWriter(new FileOutputStream(tmpfilename));
				serializer.transform(new DOMSource(n), 
									 new StreamResult(osw));
			}
		}
	}
  
	/** Decide if the node is text, and so must be handled specially */
	private boolean isTextNode(Node n) 
	{
		if (n == null)
			return false;
	
		short nodeType = n.getNodeType();
		return nodeType == Node.CDATA_SECTION_NODE || 
			nodeType == Node.TEXT_NODE;
	}

	/** Main method to run from the command line.    */
	public static void main (String[] args)
		throws Exception
	{
		if (args.length != 2) {
			System.out.println("java ApplyXPath filename.xml xpath\n"
							   + "Reads filename.xml and applies the xpath;" + 
							   " prints the nodelist found.");
			return;
		}
        
		ApplyXPath app = new ApplyXPath();
		String [] name = app.evaluateFile(args[0], args[1]);

		for (int i = 0; i < name.length; ++i) {
			System.out.println(name[i]);
		}
	}	
  
} // end of class ApplyXPath
