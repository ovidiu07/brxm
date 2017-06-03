/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.parser;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.ResourceInputProvider;
import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.Definition;
import org.onehippo.cm.model.DefinitionType;
import org.onehippo.cm.model.ValueType;
import org.onehippo.cm.model.impl.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.ConfigSourceImpl;
import org.onehippo.cm.model.impl.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ValueImpl;
import org.onehippo.cm.model.impl.WebFileBundleDefinitionImpl;
import org.onehippo.cm.model.util.SnsUtils;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import static org.onehippo.cm.model.Constants.CND_KEY;
import static org.onehippo.cm.model.Constants.DEFINITIONS;
import static org.onehippo.cm.model.Constants.META_CATEGORY_KEY;
import static org.onehippo.cm.model.Constants.META_DELETE_KEY;
import static org.onehippo.cm.model.Constants.META_IGNORE_REORDERED_CHILDREN;
import static org.onehippo.cm.model.Constants.META_ORDER_BEFORE_KEY;
import static org.onehippo.cm.model.Constants.META_RESIDUAL_CHILD_NODE_CATEGORY_KEY;
import static org.onehippo.cm.model.Constants.URI_KEY;

public class ConfigSourceParser extends SourceParser {

    public ConfigSourceParser(ResourceInputProvider resourceInputProvider) {
        super(resourceInputProvider);
    }

    public ConfigSourceParser(ResourceInputProvider resourceInputProvider, boolean verifyOnly) {
        super(resourceInputProvider, verifyOnly);
    }

    public ConfigSourceParser(ResourceInputProvider resourceInputProvider, boolean verifyOnly, boolean explicitSequencing) {
        super(resourceInputProvider, verifyOnly, explicitSequencing);
    }

    @Override
    protected void constructSource(final String path, final Node src, final ModuleImpl parent) throws ParserException {
        final Map<String, Node> sourceMap = asMapping(src, new String[]{DEFINITIONS}, null);
        final ConfigSourceImpl source = parent.addConfigSource(path);

        final Map<String, Node> definitionsMap = asMapping(sourceMap.get(DEFINITIONS), null,
                DefinitionType.CONFIG_NAMES);

        for (String definitionName : definitionsMap.keySet()) {
            final Node definitionNode = definitionsMap.get(definitionName);
            switch (DefinitionType.valueOf(definitionName.toUpperCase())) {
                case NAMESPACE:
                    constructNamespaceDefinitions(definitionNode, source);
                    break;
                case CONFIG:
                    constructConfigDefinitions(definitionNode, source);
                    break;
                case WEBFILEBUNDLE:
                    constructWebFileBundleDefinition(definitionNode, source);
                    break;
            }
        }

        source.markUnchanged();
    }

    private void constructNamespaceDefinitions(final Node src, final ConfigSourceImpl parent) throws ParserException {
        for (NodeTuple nodeTuple : asTuples(src)) {
            final String prefix = asStringScalar(nodeTuple.getKeyNode());
            final Map<String, Node> map = asMapping(nodeTuple.getValueNode(), new String[]{URI_KEY}, new String[]{CND_KEY});
            final URI uri = asURIScalar(map.get(URI_KEY));
            final ValueImpl cndPath = map.containsKey(CND_KEY)
                    ? new ValueImpl(asResourcePathScalar(map.get(CND_KEY), parent, resourceInputProvider),
                                    ValueType.STRING, true, false)
                    : null;
            parent.addNamespaceDefinition(prefix, uri, cndPath);
        }
    }

    private void constructConfigDefinitions(final Node src, final ConfigSourceImpl parent) throws ParserException {
        for (NodeTuple nodeTuple : asTuples(src)) {
            final ConfigDefinitionImpl definition = parent.addConfigDefinition();
            final String key = asPathScalar(nodeTuple.getKeyNode(), true, false);
            constructDefinitionNode(key, nodeTuple.getValueNode(), definition);
        }
    }

    @Override
    protected void populateDefinitionNode(final DefinitionNodeImpl definitionNode, final Node node) throws ParserException {
        final List<NodeTuple> tuples = asTuples(node);
        for (NodeTuple tuple : tuples) {
            final String key = asStringScalar(tuple.getKeyNode());
            final Node tupleValue = tuple.getValueNode();
            if (key.equals(META_DELETE_KEY)) {
                if (!verifyOnly) {
                    if (tuples.size() > 1) {
                        throw new ParserException("Node cannot contain '" + META_DELETE_KEY + "' and other keys", node);
                    }
                }
                final boolean delete = asNodeDeleteValue(tupleValue);
                definitionNode.setDelete(delete);
            } else if (key.equals(META_CATEGORY_KEY)) {
                if (definitionNode.getPath().equals("/")) {
                    throw new ParserException("Overriding '" + META_CATEGORY_KEY + "' on the root node is not supported", node);
                }
                final ConfigurationItemCategory category = constructCategory(tupleValue);
                if (tuples.size() > 1 && category != ConfigurationItemCategory.CONFIG) {
                    throw new ParserException("Nodes that specify '" + META_CATEGORY_KEY + ": " + category
                            + "' cannot contain other keys", node);
                }
                final Pair<String, Integer> nameAndIndex = SnsUtils.splitIndexedName(definitionNode.getName());
                if (nameAndIndex.getRight() > 0) {
                    throw new ParserException("'" + META_CATEGORY_KEY
                            + "' cannot be configured for explicitly indexed same-name siblings", node);
                }
                definitionNode.setCategory(category);
            } else if (key.equals(META_RESIDUAL_CHILD_NODE_CATEGORY_KEY)) {
                final Pair<String, Integer> parsedName = SnsUtils.splitIndexedName(definitionNode.getName());
                if (parsedName.getRight() > 0) {
                    throw new ParserException("'" + META_RESIDUAL_CHILD_NODE_CATEGORY_KEY
                            + "' cannot be configured for explicitly indexed same-name siblings", node);
                }
                final ConfigurationItemCategory category = constructCategory(tupleValue);
                definitionNode.setResidualChildNodeCategory(category);
            } else if (key.equals(META_ORDER_BEFORE_KEY)) {
                final String name = asNodeOrderBeforeValue(tupleValue);
                if (definitionNode.getName().equals(name)) {
                    throw new ParserException("Invalid " + META_ORDER_BEFORE_KEY + " targeting this node itself", node);
                }
                definitionNode.setOrderBefore(name);
            } else if (key.equals(META_IGNORE_REORDERED_CHILDREN)) {
                final Boolean ignoreReorderedChildren = (Boolean)constructValueFromScalar(tupleValue, ValueType.BOOLEAN).getObject();
                definitionNode.setIgnoreReorderedChildren(ignoreReorderedChildren);
            } else if (key.startsWith("/")) {
                final String name = key.substring(1);
                constructDefinitionNode(name, tupleValue, definitionNode);
            } else {
                constructDefinitionProperty(key, tupleValue, definitionNode);
            }
        }
    }

    private boolean asNodeDeleteValue(final Node node) throws ParserException {
        final ScalarNode scalar = asScalar(node);
        final Object object = scalarConstructor.constructScalarNode(scalar);
        if (!object.equals(true)) {
            throw new ParserException("Value for " + META_DELETE_KEY + " must be boolean value 'true'", node);
        }
        return true;
    }

    private String asNodeOrderBeforeValue(final Node node) throws ParserException {
        return asStringScalar(node);
    }

    private void constructWebFileBundleDefinition(final Node definitionNode, final ConfigSourceImpl source) throws ParserException {
        final List<Node> nodes = asSequence(definitionNode);
        for (Node node : nodes) {
            final String name = asStringScalar(node);
            for (Definition definition : source.getModifiableDefinitions()) {
                if (definition instanceof WebFileBundleDefinitionImpl) {
                    final WebFileBundleDefinitionImpl existingDefinition = (WebFileBundleDefinitionImpl) definition;
                    if (existingDefinition.getName().equals(name)) {
                        throw new ParserException("Duplicate web file bundle name '" + name + "'", node);
                    }
                }
            }
            source.addWebFileBundleDefinition(name);
        }
    }
}
