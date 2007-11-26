/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
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
package org.hippoecm.repository;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;

public class FacetedNavigationChildNameTest extends FacetedNavigationAbstractTest {

    public void testFacetIssues() throws RepositoryException {
        final String simple = "testnode";
        final String encodeMe = "2..,!@#$%^&*()_-[]{}|\\:;'\".,/?testnode";
        Node node;

        // first create a document node 
        {
            if (!session.getRootNode().hasNode("documents"))
                session.getRootNode().addNode("documents");
            node = session.getRootNode().getNode("documents");
            //String encodeMe = "yadida"; 
            Node docNode = node.addNode(simple, HippoNodeType.NT_DOCUMENT);
            docNode.setProperty("x", "success");
            docNode.setProperty("y", encodeMe);
            session.save();
        }

        // create the faceted navigation
        {
            node = session.getRootNode().getNode("navigation").addNode("navxy", HippoNodeType.NT_FACETSEARCH);
            node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "navxy");
            node.setProperty(HippoNodeType.HIPPO_DOCBASE, "/documents");
            node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "x", "y" });
            session.save();
        }
        
        String path = "navigation/navxy/success";
        Node facetNode = session.getRootNode().getNode(path);
        assertNotNull(facetNode);
        facetNode.hashCode(); // This earlier caused a NullPointerException, see HREPTWO-269
        assertEquals(ISO9075Helper.encodeLocalName("success"), facetNode.getName());


        Node nodeFromFacet = (Node) facetNode.getNode(HippoNodeType.HIPPO_RESULTSET).getNodes().nextNode();
        try {
            path = "navigation/navxy/success/"+ISO9075Helper.encodeLocalName(encodeMe);
            facetNode = session.getRootNode().getNode(path);
            //System.out.println(facetNode.getName());
            assertEquals(ISO9075Helper.encodeLocalName(encodeMe), facetNode.getName());
            nodeFromFacet = (Node) facetNode.getNode(HippoNodeType.HIPPO_RESULTSET).getNodes().nextNode();
            Node docNode = session.getRootNode().getNode("documents").getNode(simple);
            assertTrue(nodeFromFacet.hasProperty("x"));
            assertTrue(nodeFromFacet.hasProperty("y"));
            assertEquals(docNode.getProperty("x").getString(), nodeFromFacet.getProperty("x").getString());
            assertEquals(docNode.getProperty("y").getString(), nodeFromFacet.getProperty("y").getString());
        } catch (PathNotFoundException e) {
            fail("Issue HREPTWO-270 should be reopened");
        }

        session.refresh(false);
    }

    @Override
    public void testPerformance() throws RepositoryException, IOException {
    }

}
