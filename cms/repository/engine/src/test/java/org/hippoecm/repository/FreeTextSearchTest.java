/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;

public class FreeTextSearchTest extends TestCase {
    public final static String NT_SEARCHDOCUMENT = "hippo:testsearchdocument";
    public static final String NT_COMPOUNDSTRUCTURE = "hippo:testcompoundstructure";
    public static final String NT_HTML = "hippo:testhtml";
    

    public final static String DOCUMENT_TITLE_PART = "foo";
    public final static String COMPOUNDDOCUMENT_TITLE_PART = "bar";
    public final static String  HTML_CONTENT_PART = "lux";
    public final static String  BINARY_CONTENT_PART = "dog";

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
   
    private final static String TEST_PATH = "test";
    private Node testPath;

    
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (session.getRootNode().hasNode(TEST_PATH)) {
            session.getRootNode().getNode(TEST_PATH).remove();
        }
        testPath = session.getRootNode().addNode(TEST_PATH);
        session.save();
    }

    
    /*
     * This creates the following structure:
     * 
     * - Document1 (hippo:handle)
     *     ` Document1 (hippo:testsearchdocument)
     *           ` compoundchild (hippo:testcompoundstructure)
     *                |- hippo:testhtml  (hippo:testhtml)
     *                |- hippo:testresource (hippo:testtextresource)
     *                `- hippo:testpdfresource (hippo:resource) (this one conditional if includePdf = true)
     *                     
     */
    
    private void createCompoundStructure(boolean includePdf) throws Exception {
        Node handle = testPath.addNode("Document1", HippoNodeType.NT_HANDLE);
        Node document = handle.addNode("Document1", NT_SEARCHDOCUMENT);
        document.setProperty("title", "This is the title of document 1 containing " + DOCUMENT_TITLE_PART);
        Node compound = document.addNode("compoundchild", NT_COMPOUNDSTRUCTURE);
        compound.setProperty("compoundtitle", "This is the compoundtitle containing " + COMPOUNDDOCUMENT_TITLE_PART);
        Node html = compound.addNode("hippo:testhtml", NT_HTML);
        html.setProperty("hippo:testcontent", "The content property of testhtml node containing " + HTML_CONTENT_PART);
        
        Node resource = compound.addNode("hippo:testresource", "hippo:testtextresource");
        resource.setProperty("jcr:encoding", "UTF-8");
        resource.setProperty("jcr:mimeType", "text/plain");
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(data, "UTF-8");
        writer.write("The quick brown fox jumps over the lazy " + BINARY_CONTENT_PART);
        writer.close();
        resource.setProperty("jcr:data", new ByteArrayInputStream(data.toByteArray()));
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
    
        testPath.save();
    }
    
 
    @Test
    public void testSimpleFreeTextSearch() throws Exception {
        
        createCompoundStructure(false);
        
        String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+DOCUMENT_TITLE_PART+"')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        NodeIterator nodes = queryResult.getNodes();
        assertTrue(nodes.getSize() == 1L); 
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
        
    }
 
    /**
     * test where we search on a String property in a child node and we expect the parent node, Document1 
     * to find, which is a parent of the node containing the property
     * @throws RepositoryException
     * @throws IOException 
     */
    @Test
    public void testFirstLevelChildNodeFreeTextSearch() throws Exception {
        
        createCompoundStructure(false);
        
        String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+COMPOUNDDOCUMENT_TITLE_PART+"')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        NodeIterator nodes = queryResult.getNodes();
        assertTrue(nodes.getSize() == 1L); 
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
        
    }
    
    /**
     * test where we search on a String property in a child node and we expect the parent's parent node, Document1 
     * to find, which is a parent of the node containing the property
     * @throws RepositoryException
     * @throws IOException 
     */
    @Test
    public void testSecondLevelChildNodeFreeTextSearch() throws Exception {
        
        createCompoundStructure(false);
        
        String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+HTML_CONTENT_PART+"')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        NodeIterator nodes = queryResult.getNodes();
        assertTrue(nodes.getSize() == 1L); 
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
        
    }
    
    /**
     * test where we search on a binary property in a child node and we expect the parent's parent node, Document1 
     * to find, which is a parent of the node containing the property
     * @throws RepositoryException
     * @throws IOException 
     */
    @Test
    public void testSecondChildNodeBinaryFreeTextSearch() throws Exception {
        
        createCompoundStructure(false);
        String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+BINARY_CONTENT_PART+"')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        NodeIterator nodes = queryResult.getNodes();
        assertTrue(nodes.getSize() == 1L); 
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
        
    }
    
    /**
     * When we search on the text property of a direct child node below the hippo:document, we should
     * not find the hippo:document anymore when the child node is removed. This test is to assure that, 
     * when a direct child node is removed, the hippo:document is reindexed in the repository. 
     * 
     * @throws RepositoryException
     * @throws IOException 
     */
    @Test
    public void testDeleteFirstLevelChildNode() throws Exception {
        
        createCompoundStructure(false);
        
        String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+COMPOUNDDOCUMENT_TITLE_PART+"')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        NodeIterator nodes = queryResult.getNodes();
        assertTrue(nodes.getSize() == 1L); 
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
        
        Node n = this.testPath.getNode("Document1/Document1");
        n.getNode("compoundchild").remove();
        n.save();
        
        xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+COMPOUNDDOCUMENT_TITLE_PART+"')]";
        queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        nodes = queryResult.getNodes();
        // we have removed the deeper child node that had the 'HTML_CONTENT_PART' term, hence, we should not
        // get a hit anymore
        assertTrue(nodes.getSize() == 0L); 
        
    }
    
    /**
     * When we search on the text property of some child node at some level below the hippo:document, we should
     * not find the hippo:document anymore when the child node is removed. This test is to assure that, 
     * when a deeper located child node is removed, the hippo:document must is in the repository. 
     * 
     * @throws RepositoryException
     * @throws IOException 
     */
    @Test
    public void testDeleteSecondLevelChildNode() throws Exception {
        
        createCompoundStructure(false);
        
        String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+HTML_CONTENT_PART+"')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        NodeIterator nodes = queryResult.getNodes();
        assertTrue(nodes.getSize() == 1L); 
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
        
        Node n = this.testPath.getNode("Document1/Document1/compoundchild");
        n.getNode("hippo:testhtml").remove();
        n.save();
        
        xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+HTML_CONTENT_PART+"')]";
        queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        nodes = queryResult.getNodes();
        // we have removed the deeper child node that had the 'HTML_CONTENT_PART' term, hence, we should not
        // get a hit anymore
        assertTrue(nodes.getSize() == 0L); 
        
    }
   
    
    @Test
    public void testAddFirstLevelChildNode() throws Exception {
        createCompoundStructure(false);
        Node n = this.testPath.getNode("Document1/Document1");
        Node compound = n.addNode("compoundchild", NT_COMPOUNDSTRUCTURE);
        String word = "addedcompound";
        compound.setProperty("compoundtitle", "This is the compoundtitle containing " + word);
        n.save();
        
        String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+word+"')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        NodeIterator nodes = queryResult.getNodes();
        assertTrue(nodes.getSize() == 1L); 
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
    }
    
    @Test
    public void testAddSecondLevelChildNode() throws Exception {
        createCompoundStructure(false);
        Node n = this.testPath.getNode("Document1/Document1/compoundchild");
        Node html = n.addNode("hippo:html2", NT_HTML);
        String word =  "addedhtmlnode";
        html.setProperty("hippo:testcontent", "The content property of testhtml node containing " + word);
        n.save();
        
        String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+word+"')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        NodeIterator nodes = queryResult.getNodes();
        assertTrue(nodes.getSize() == 1L); 
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
    }
    
    @Test
    public void testModifyFirstLevelChildNode() throws Exception {
        createCompoundStructure(false);
        Node n = this.testPath.getNode("Document1/Document1");
       
        Node compound = testPath.getNode("Document1/Document1/compoundchild");
        String word = "changedcompound";
        compound.setProperty("compoundtitle", "This is now the new compoundtitle containing " + word);
        n.save();
        
        String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+word+"')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        NodeIterator nodes = queryResult.getNodes();
        assertTrue(nodes.getSize() == 1L); 
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
    }
    
    
    @Test
    public void testModifySecondLevelChildNode() throws Exception {
       
        createCompoundStructure(false);
        Node n = this.testPath.getNode("Document1/Document1");
       
        Node html = testPath.getNode("Document1/Document1/compoundchild/hippo:testhtml");
        String word = "changedtesthtml";
        html.setProperty("hippo:testcontent", "The content property of testhtml node now containing " + word);
        n.save();
        
        String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+word+"')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        NodeIterator nodes = queryResult.getNodes();
        assertTrue(nodes.getSize() == 1L); 
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
    }
}
