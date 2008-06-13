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
package org.hippoecm.repository.sample;

import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.HippoRepository;

abstract class SampleWorkflowSetup
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static int oldAuthorId;
    static int newAuthorId;

    static void commonStart(HippoRepository server) throws RepositoryException {
        Random rnd = new Random(8675687);
        oldAuthorId = rnd.nextInt();
        newAuthorId = rnd.nextInt();

        Session session = server.login("admin","admin".toCharArray());
        Node root = session.getRootNode();

        // set up the workflow specification as a node "/hippo:configuration/hippo:workflows/mycategory/myworkflow"
        Node node = root.getNode("hippo:configuration");
        node = node.getNode("hippo:workflows");
        node = node.addNode("mycategory","hippo:workflowcategory");
        node = node.addNode("myworkflow","hippo:workflow");
        node.setProperty("hippo:nodetype","sample:newsArticle");
        node.setProperty("hippo:display","Sample Workflow");
        node.setProperty("hippo:renderer","org.hippoecm.repository.sample.SampleWorkflowRenderer");
        node.setProperty("hippo:classname","org.hippoecm.repository.sample.SampleWorkflowImpl");
        Node types = node.getNode("hippo:types");
        node = types.addNode("org.hippoecm.repository.sample.AuthorDocument","hippo:type");
        node.setProperty("hippo:nodetype","sample:author");
        node.setProperty("hippo:display","AuthorDocument");
        node.setProperty("hippo:classname","org.hippoecm.repository.sample.AuthorDocument");
        node = types.addNode("org.hippoecm.repository.sample.ArticleDocument","hippo:type");
        node.setProperty("hippo:nodetype","sample:newsArticle");
        node.setProperty("hippo:display","ArticleDocument");
        node.setProperty("hippo:classname","org.hippoecm.repository.sample.ArticleDocument");

        // set up the queryable document specification as a node "/configuration/hippo:documents/authors"
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("files//*[@jcr:primaryType='sample:author' and @sample:name='$name']", Query.XPATH);
        node = query.storeAsNode("/hippo:configuration/hippo:documents/authors");
        String statement = node.getProperty("jcr:statement").getString();
        String language = node.getProperty("jcr:language").getString();
        node.remove();
        node = root.getNode("hippo:configuration/hippo:documents");
        node = node.addNode("authors","hippo:ocmquery");
        node.setProperty("jcr:statement",statement);
        node.setProperty("jcr:language",language);
        node.setProperty("hippo:classname","org.hippoecm.repository.sample.AuthorDocument");
        node = node.getNode("hippo:types");
        node = node.addNode("org.hippoecm.repository.sample.AuthorDocument","hippo:type");
        node.setProperty("hippo:nodetype","sample:author");
        node.setProperty("hippo:display","AuthorDocument");
        node.setProperty("hippo:classname","org.hippoecm.repository.sample.AuthorDocument");

        root.addNode("files");

        node = root.getNode("files");
        node = node.addNode("myauthor","sample:author");
        node.setProperty("sample:id",newAuthorId);
        node.setProperty("sample:name","Jan Smit");

        node = root.getNode("files");
        node = node.addNode("myarticle","sample:newsArticle");
        node.setProperty("sample:id",1);
        node.setProperty("sample:authorId",oldAuthorId);

        session.save();
        session.logout();
    }

    static void commonEnd(HippoRepository server) throws RepositoryException {
        Session session = server.login("dummy","dummy".toCharArray());
        Node root = session.getRootNode();
        root.getNode("files").remove();

        root.getNode("hippo:configuration/hippo:workflows").remove();
        root.addNode("hippo:configuration/hippo:workflows", "hippo:workflowfolder");

        root.getNode("hippo:configuration/hippo:documents").remove();
        root.addNode("hippo:configuration/hippo:documents", "hippo:ocmqueryfolder");
        session.save();
        session.logout();
    }
}
