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
package org.hippoecm.repository.updater;

import java.util.LinkedList;

import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

public abstract class UpdaterItemVisitor implements ItemVisitor {

    final protected boolean breadthFirst;
    private LinkedList<Item> currentQueue;
    private LinkedList<Item> nextQueue;
    private int currentLevel;

    public UpdaterItemVisitor() {
        this(false);
    }

    public UpdaterItemVisitor(boolean breadthFirst) {
        this.breadthFirst = breadthFirst;
        if (breadthFirst) {
            currentQueue = new LinkedList<Item>();
            nextQueue = new LinkedList<Item>();
        }
        currentLevel = 0;
    }

    protected abstract void entering(Property property, int level)
            throws RepositoryException;

    protected abstract void entering(Node node, int level)
            throws RepositoryException;

    protected abstract void leaving(Property property, int level)
            throws RepositoryException;

    protected abstract void leaving(Node node, int level)
            throws RepositoryException;

    public final void visit(Property property) throws RepositoryException {
        entering(property, currentLevel);
        leaving(property, currentLevel);
    }

    public void visit(Node node) throws RepositoryException {
        if (node.getPath().equals("/jcr:system")) {
            return;
        }
        //System.err.println("VISIT "+node.getPath());
        try {
            if (!breadthFirst) {
                // depth-first traversal
                entering(node, currentLevel);
                currentLevel++;
                PropertyIterator propIter = node.getProperties();
                while (propIter.hasNext()) {
                    propIter.nextProperty().accept(this);
                }
                NodeIterator nodeIter = node.getNodes();
                while (nodeIter.hasNext()) {
                    nodeIter.nextNode().accept(this);
                }
                currentLevel--;
                leaving(node, currentLevel);
            } else {
                // breadth-first traversal
                entering(node, currentLevel);
                leaving(node, currentLevel);

                PropertyIterator propIter = node.getProperties();
                while (propIter.hasNext()) {
                    nextQueue.addLast(propIter.nextProperty());
                }
                NodeIterator nodeIter = node.getNodes();
                while (nodeIter.hasNext()) {
                    nextQueue.addLast(nodeIter.nextNode());
                }

                while (!currentQueue.isEmpty() || !nextQueue.isEmpty()) {
                    if (currentQueue.isEmpty()) {
                        currentLevel++;
                        currentQueue = nextQueue;
                        nextQueue = new LinkedList<Item>();
                    }
                    Item item = (Item) currentQueue.removeFirst();
                    item.accept(this);
                }
                currentLevel = 0;
            }
        } catch (RepositoryException ex) {
            currentLevel = 0;
            throw ex;
        }
    }

    public static class Default extends UpdaterItemVisitor {

        public Default() {
        }

        public Default(boolean breadthFirst) {
            super(breadthFirst);
        }

        protected void entering(Node node, int level)
                throws RepositoryException {
        }

        protected void entering(Property property, int level)
                throws RepositoryException {
        }

        protected void leaving(Node node, int level)
                throws RepositoryException {
        }

        protected void leaving(Property property, int level)
                throws RepositoryException {
        }
    }

    public static class Converted extends Default {

        public void visit(Node node) throws RepositoryException {
            if (((UpdaterNode) node).hollow) {
                return;
            }
            super.visit(node);
        }
    }
}

