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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.rmi.RemoteException;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

public class FolderWorkflowTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    Node root, node;
    WorkflowManager manager;
    String[] content = {
        "/test/f", "hippostd:folder",
        "jcr:mixinTypes", "hippo:harddocument",
        "/test/aap", "hippostd:folder",
        "jcr:mixinTypes", "hippo:harddocument",
        "/test/aap/noot", "nt:unstructured",
        "/test/aap/noot/mies", "hippostd:folder",
        "jcr:mixinTypes", "hippo:harddocument",
        "/test/aap/noot/mies/vuur", "nt:unstructured",
        "/test/aap/noot/mies/vuur/jot", "nt:unstructured",
        "/test/aap/noot/mies/vuur/jot/gijs", "hippo:coredocument",
        "jcr:mixinTypes", "hippo:harddocument",
        "/test/aap/noot/mies/vuur/jot/gijs/duif", "hippo:document",
        "jcr:mixinTypes", "hippo:harddocument"
    };

    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        root = session.getRootNode();
        if(root.hasNode("test"))
            root.getNode("test").remove();
        root = root.addNode("test");
        session.save();

        build(session, content);
        session.save();
        node = root.getNode("f");
        manager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
    }

    @After
    public void tearDown() throws Exception {
        root = session.getRootNode();
        if(root.hasNode("test"))
            root.getNode("test").remove();
        super.tearDown();
    }

    @Test
    public void testQuery() throws Exception {
        Node document = session.getRootNode().getNode("test/aap/noot/mies/vuur/jot/gijs");
        QueryManager manager = session.getWorkspace().getQueryManager();
        HippoQuery query = (HippoQuery) manager.getQuery(session.getRootNode().getNode("hippo:configuration/hippo:documents/embedded"));
        Map<String,String> arguments = new TreeMap<String,String>();
        arguments.put("id", document.getUUID());
        query.bindValue("id", session.getValueFactory().createValue(document.getUUID()));
        QueryResult resultset = query.execute();
        NodeIterator iter = resultset.getNodes();
        assertTrue(iter.getSize() > 0);
        assertTrue(iter.hasNext());
        assertEquals("/test/aap/noot/mies", iter.nextNode().getPath());
    }

    // FIXME: Re-enable test. Maybe some configuration is missing? Does the (root) user have the correct privileges (hippo:editor)?
    @Ignore
    public void testDelete() throws Exception {
        Node document = session.getRootNode().getNode("test/aap/noot/mies/vuur/jot/gijs");
        Workflow workflow = manager.getWorkflow("default", document);
        assertNotNull(workflow);
        assertTrue(workflow instanceof DefaultWorkflow);
        ((DefaultWorkflow)workflow).delete();
        assertTrue(root.hasNode("aap/noot/mies/vuur/jot"));
        assertFalse(root.hasNode("aap/noot/mies/vuur/jot/gijs"));
    }

    @Test
    public void testFolder() throws RepositoryException, WorkflowException, RemoteException {
        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", node);
        assertNotNull(workflow);
        Map<String,Set<String>> types = workflow.list();
        assertNotNull(types);
        assertTrue(types.containsKey("new-folder"));
        assertTrue(types.get("new-folder").contains("hippostd:folder"));
        String path = workflow.add("new-folder", "hippostd:folder", "d");
        assertNotNull(path);
        node = session.getRootNode().getNode(path.substring(1));
        assertEquals("/test/f/d",node.getPath());
        assertTrue(node.isNodeType("hippostd:folder"));
    }

    @Test
    public void testDirectory() throws RepositoryException, WorkflowException, RemoteException {
        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", node);
        assertNotNull(workflow);
        Map<String,Set<String>> types = workflow.list();
        assertNotNull(types);
        assertTrue(types.containsKey("new-folder"));
        assertTrue(types.get("new-folder").contains("hippostd:directory"));
        String path = workflow.add("new-folder", "hippostd:directory", "d");
        assertNotNull(path);
        node = session.getRootNode().getNode(path.substring(1));
        assertEquals("/test/f/d",node.getPath());
        assertTrue(node.isNodeType("hippostd:directory"));
    }

    @Test
    public void testNonExistent() throws RepositoryException, WorkflowException, RemoteException {
        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", node);
        assertNotNull(workflow);
        Map<String,Set<String>> types = workflow.list();
        assertNotNull(types);

        assertFalse(types.containsKey("new-does-not-exist"));
        try {
            workflow.add("new-does-not-exists", "does-not-exist", "d");
            fail("exception expected when using undefined category");
        } catch(WorkflowException ex) {
            // expected
        }

        assertTrue(types.containsKey("new-folder"));
        assertFalse(types.get("new-folder").contains("does-not-exist"));
        try {
            String path = workflow.add("new-folder", "does-not-exist", "d");
            fail("exception expected when usng undefined prototype");
        } catch(WorkflowException ex) {
            // expected
        }
    }


    @Ignore
    public void testTemplateDocument() throws RepositoryException, WorkflowException, RemoteException {
        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", node);
        assertNotNull(workflow);
        Map<String,String[]> renames = new TreeMap<String,String[]>();
        Map<String,Set<String>> types = workflow.list();
        assertNotNull(types);
        assertTrue(types.containsKey("simple"));
        assertTrue(types.get("simple").contains("new-document"));
        String path = workflow.add("simple", "new-document", "d");
        assertNotNull(path);
        node = session.getRootNode().getNode(path.substring(1));
        assertEquals("/test/f/d",node.getPath());
        assertTrue(node.isNodeType("hippo:handle"));
        assertTrue(node.hasNode(node.getName()));
        assertTrue(node.getNode(node.getName()).isNodeType("hippostd:document"));
    }

    @Test
    public void testReorderFolder() throws RepositoryException, WorkflowException, RemoteException {
        Node node = root.addNode("f","hippostd:folder");
        node.addMixin("hippo:harddocument");
        node.addNode("aap");
        node.addNode("noot");
        node.addNode("mies");
        node.addNode("zorro");
        node.addNode("foo");
        node.addNode("bar");
        session.save();

        NodeIterator it = node.getNodes();
        assertEquals("aap", it.nextNode().getName());
        assertEquals("noot", it.nextNode().getName());
        assertEquals("mies", it.nextNode().getName());
        assertEquals("zorro", it.nextNode().getName());
        assertEquals("foo", it.nextNode().getName());
        assertEquals("bar", it.nextNode().getName());

        WorkflowManager manager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
        FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", node);

        /*
         * aap      aap
         * noot     bar
         * mies     foo
         * zorro => mies
         * foo      noot
         * bar      zorro
         */
        List<String> newOrder = new LinkedList<String>();
        newOrder.add("aap");
        newOrder.add("bar");
        newOrder.add("foo");
        newOrder.add("mies");
        newOrder.add("noot");
        newOrder.add("zorro");

        workflow.reorder(newOrder);
        node.refresh(false);

        it = node.getNodes();
        assertEquals("aap", it.nextNode().getName());
        assertEquals("bar", it.nextNode().getName());
        assertEquals("foo", it.nextNode().getName());
        assertEquals("mies", it.nextNode().getName());
        assertEquals("noot", it.nextNode().getName());
        assertEquals("zorro", it.nextNode().getName());

    }

    /* The following two tests can only be executed if repository is run
     * locally, and the method copy in FolderWorkflowImpl is made public,
     * which is shouldn't be.  They where used for development purposes,
     * mainly.

    @Test
    public void testCopyFolder() throws RepositoryException, RemoteException {
        FolderWorkflowImpl workflow;
        workflow = new FolderWorkflowImpl(session, session, session.getRootNode().getNode(
                                "hippo:configuration/hippo:queries/hippo:templates/folder/hippostd:templates/document folder"));
        assertFalse(session.getRootNode().getNode("test").hasNode("folder"));
        TreeMap<String,String[]> renames = new TreeMap<String,String[]>();
        renames.put("./_name", new String[] { "f" });
        workflow.copy(session.getRootNode().getNode(
                                "hippo:configuration/hippo:queries/hippo:templates/folder/hippostd:templates/document folder"),
                                session.getRootNode().getNode("test"), renames, ".");
        assertTrue(session.getRootNode().getNode("test").hasNode("f"));
    }

    @Test
    public void testCopyDocument() throws RepositoryException, RemoteException {
        FolderWorkflowImpl workflow;
        workflow = new FolderWorkflowImpl(session, session, session.getRootNode().getNode(
                                                                 "hippo:configuration/hippo:queries/hippo:templates/document"));
        assertFalse(session.getRootNode().getNode("test").hasNode("document"));
        assertFalse(session.getRootNode().getNode("test").hasNode("d"));
        TreeMap<String,String[]> renames = new TreeMap<String,String[]>();
        renames.put("./_name", new String[] { "d" });
        renames.put("./_node/_name", new String[] { "d" });
        workflow.copy(session.getRootNode().getNode(
                                       "hippo:configuration/hippo:queries/hippo:templates/simple/hippostd:templates/document"),
                                       session.getRootNode().getNode("test"), renames, ".");
        assertTrue(session.getRootNode().getNode("test").hasNode("d"));
        assertTrue(session.getRootNode().getNode("test").getNode("d").hasNode("d"));
    }

    */

    private static void createDirectories(Session session, WorkflowManager manager, Node node, Random random, int numiters)
        throws RepositoryException, WorkflowException, RemoteException {
        Vector<String> paths = new Vector<String>();
        Vector<String> worklog = new Vector<String>();
        for(int itercount=0; itercount<numiters; ++itercount) {
            int parentIndex = (paths.size() > 0 ? random.nextInt(paths.size()) : -1);
            String parentPath;
            Node parent;
            if(parentIndex >= 0) {
                parentPath = paths.get(parentIndex);
                parent = node.getNode(parentPath.substring(2));
            } else {
                parentPath = ".";
                parent = node;
            }
            session.refresh(false);
            FolderWorkflow workflow = (FolderWorkflow) manager.getWorkflow("internal", parent);
            FolderWorkflow folderWorkflow = workflow;
            assertNotNull(workflow);
            Map<String,Set<String>> types = workflow.list();
            assertNotNull(types);
            assertTrue(types.containsKey("new-folder"));
            assertTrue(types.get("new-folder").contains("hippostd:folder"));
            String childPath = workflow.add("new-folder", "hippostd:folder", "f");
            assertNotNull(childPath);
            Node child = session.getRootNode().getNode(childPath.substring(1));
            assertTrue(child.isNodeType("hippostd:folder"));
            assertTrue(child.isNodeType("hippo:document"));
            assertTrue(child.isNodeType("hippo:harddocument"));
            childPath = parentPath + "/f";
            //assertEquals("/test/f"+childPath.substring(1), child.getPath());
            if(!paths.contains(childPath)) {
                paths.add(childPath);
            }
            worklog.add(childPath);
        }
    }

    @Test
    public void testExtensive() throws RepositoryException, WorkflowException, RemoteException {
        createDirectories(session, manager, node, new Random(72099L), 100);
    }

    private Exception concurrentError = null;

    private class ConcurrentRunner extends Thread {
        long seed;
        int niters;
        ConcurrentRunner(long seed, int niters) {
            this.seed = seed;
            this.niters = niters;
        }
        public void run() {
            try {
                Session session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
                WorkflowManager manager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
                Node test = session.getRootNode().getNode(node.getPath().substring(1));
                createDirectories(session, manager, node, new Random(seed), niters);
            } catch(RepositoryException ex) {
                concurrentError = ex;
            } catch(WorkflowException ex) {
                concurrentError = ex;
            } catch(RemoteException ex) {
                concurrentError = ex;
            }
        }
    }

    @Ignore
    public void testConcurrent() throws Exception {
        final int niters = 50;
        Thread thread1 = new ConcurrentRunner(2095487L, niters);
        Thread thread2 = new ConcurrentRunner(70178491L, niters);
        thread1.start();
        thread2.start();
        thread2.join();
        thread1.join();
        if(concurrentError != null) {
            throw concurrentError;
        }
    }

    @Ignore
    public void testMoreConcurrent() throws Exception {
        final int nthreads = 20;
        final int niters = 30;
        long seed = 1209235890128L;
        Thread[] threads = new Thread[nthreads];
        for(int i=0; i<nthreads; i++) {
            threads[i] = new ConcurrentRunner(seed++, niters);
        }
        for(int i=0; i<nthreads; i++) {
            threads[i].start();
        }
        for(int i=0; i<nthreads; i++) {
            threads[i].join();
        }
        if(concurrentError != null) {
            throw concurrentError;
        }
    }
}
