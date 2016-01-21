/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.restapi.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.hst.restapi.ResourceContext;
import org.hippoecm.hst.restapi.scanning.NodeVisitorType;
import org.hippoecm.hst.restapi.content.visitors.HippoPublicationWorkflowDocumentVisitor;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_LANGUAGE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MESSAGE;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_DESCRIPTION;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_KEY;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_SYNONYMS;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TAXONOMY;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TRANSLATION;

@NodeVisitorType
public class TaxonomyVisitor extends HippoPublicationWorkflowDocumentVisitor {

    @Override
    public String getNodeType() {
        return NODETYPE_HIPPOTAXONOMY_TAXONOMY;
    }

    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        super.visitNode(context, node, response);

        final LinkedHashMap<String, Object> categories = new LinkedHashMap<>();
        for (Node child : new NodeIterable(node.getNodes())) {
            if (child.isNodeType(NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                visitCategory(context, child, categories);
            }
        }
        if (!categories.isEmpty()) {
            response.put("categories", categories);
        }
    }

    protected void visitCategory(final ResourceContext context, final Node catNode, final Map<String, Object> response)
            throws RepositoryException {
        final LinkedHashMap<String, Object> category = new LinkedHashMap<>();
        response.put(catNode.getName(), category);
        category.put("key", catNode.getProperty(HIPPOTAXONOMY_KEY).getString());

        final LinkedHashMap<String, Object> categories = new LinkedHashMap<>();
        for (Node child : new NodeIterable(catNode.getNodes())) {
            if (child.isNodeType(NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                visitCategory(context, child, categories);
            }
            else if (child.isNodeType(NODETYPE_HIPPOTAXONOMY_TRANSLATION)) {
                final LinkedHashMap<String, Object> translation = new LinkedHashMap<>();
                category.put(child.getProperty(HIPPO_LANGUAGE).getString(), translation);
                translation.put("message", child.getProperty(HIPPO_MESSAGE).getString());
                if (child.hasProperty(HIPPOTAXONOMY_DESCRIPTION)) {
                    translation.put("description", child.getProperty(HIPPOTAXONOMY_DESCRIPTION).getString());
                }
                if (child.hasProperty(HIPPOTAXONOMY_SYNONYMS)) {
                    final ArrayList<String> synonyms = new ArrayList<>();
                    for (Value value : child.getProperty(HIPPOTAXONOMY_SYNONYMS).getValues()) {
                        if (value.getString() != null) {
                            synonyms.add(value.getString());
                        }
                    }
                    if (!synonyms.isEmpty()) {
                        translation.put("synonyms", synonyms);
                    }
                }
            }
        }
        if (!categories.isEmpty()) {
            category.put("categories", categories);
        }
    }

    @Override
    protected boolean skipChild(final ResourceContext context, final ContentTypeChild childType, final Node child)
            throws RepositoryException {
        return child.isNodeType(NODETYPE_HIPPOTAXONOMY_CATEGORY);
    }
}
