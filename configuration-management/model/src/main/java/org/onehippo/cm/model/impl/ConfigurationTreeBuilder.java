/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cm.model.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.PropertyOperation;
import org.onehippo.cm.model.PropertyType;
import org.onehippo.cm.model.SnsUtils;
import org.onehippo.cm.model.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.MIX_REFERENCEABLE;
import static org.onehippo.cm.model.Constants.META_IGNORE_REORDERED_CHILDREN;
import static org.onehippo.cm.model.PropertyOperation.ADD;
import static org.onehippo.cm.model.PropertyOperation.DELETE;
import static org.onehippo.cm.model.PropertyOperation.OVERRIDE;
import static org.onehippo.cm.model.PropertyOperation.REPLACE;

class ConfigurationTreeBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTreeBuilder.class);
    private static final String REP_ROOT_NT = "rep:root";

    private final ConfigurationNodeImpl root = new ConfigurationNodeImpl();

    ConfigurationTreeBuilder() {
        root.setName("");
        root.setResidualNodeCategory(ConfigurationItemCategory.RUNTIME);

        // add required jcr:primaryType: rep:root
        final ConfigurationPropertyImpl primaryTypeProperty = new ConfigurationPropertyImpl();
        primaryTypeProperty.setName(JCR_PRIMARYTYPE);
        primaryTypeProperty.setParent(root);
        primaryTypeProperty.setType(PropertyType.SINGLE);
        primaryTypeProperty.setValueType(ValueType.NAME);
        primaryTypeProperty.setValue(new ValueImpl(REP_ROOT_NT, ValueType.NAME, false, false));
        root.addProperty(JCR_PRIMARYTYPE, primaryTypeProperty);

        // add required jcr:mixinTypes: mix:referenceable
        final ConfigurationPropertyImpl mixinTypesProperty = new ConfigurationPropertyImpl();
        mixinTypesProperty.setName(JCR_MIXINTYPES);
        mixinTypesProperty.setParent(root);
        mixinTypesProperty.setType(PropertyType.LIST);
        mixinTypesProperty.setValueType(ValueType.NAME);
        mixinTypesProperty.setValues(new ValueImpl[]{new ValueImpl(MIX_REFERENCEABLE, ValueType.NAME, false, false)});
        root.addProperty(JCR_MIXINTYPES, mixinTypesProperty);
    }

    ConfigurationNodeImpl build() {
        // validation of the input and construction of the tree happens at "push time".

        pruneDeletedItems(root);
        return root;
    }

    void push(final ContentDefinitionImpl definition) {
        final ConfigurationNodeImpl rootForDefinition = getOrCreateRootForDefinition(definition);

        if (rootForDefinition != null) {
            mergeNode(rootForDefinition, definition.getModifiableNode());
        }
    }

    /**
     * Recursively merge a tree of {@link DefinitionNodeImpl}s and {@link DefinitionPropertyImpl}s
     * onto the tree of {@link ConfigurationNodeImpl}s and {@link ConfigurationPropertyImpl}s.
     */
    private void mergeNode(final ConfigurationNodeImpl node, final DefinitionNodeImpl definitionNode) {
        if (definitionNode.isDeleted()) {
            final String indexedName = SnsUtils.createIndexedName(definitionNode.getName());
            if (SnsUtils.hasSns(indexedName, node.getParent().getNodes().keySet())) {
                node.getParent().removeNode(indexedName, true);
            } else {
                markNodeAsDeletedBy(node, definitionNode);
            }
            return;
        } else if (definitionNode.isDelete() && !node.isNew()) {
            String msg = String.format("%s: Trying to delete AND merge node %s defined before by %s.",
                    definitionNode.getOrigin(), definitionNode.getPath(), node.getOrigin());
            throw new IllegalArgumentException(msg);
        }

        if (definitionNode.getCategory() != null) {
            if (definitionNode.getCategory() == ConfigurationItemCategory.CONFIGURATION) {
                node.getParent().clearChildNodeCategorySettings(node.getName());
            } else {
                keepOnlyFirstSns(node.getParent(), node.getName());
                markNodeAsDeletedBy(node, definitionNode);
                node.getParent().setChildNodeCategorySettings(node.getName(), definitionNode.getCategory(), definitionNode);
                return;
            }
        }

        if (definitionNode.getResidualChildNodeCategory() != null) {
            node.setResidualNodeCategory(definitionNode.getResidualChildNodeCategory());
            node.setResidualNodeCategoryDefinitionItem(definitionNode);
            node.addDefinitionItem(definitionNode);
        }

        if (definitionNode.getIgnoreReorderedChildren() != null) {
            if (node.getIgnoreReorderedChildren() != null || definitionNode.getIgnoreReorderedChildren() == node.getIgnoreReorderedChildren()) {
                if (definitionNode.getIgnoreReorderedChildren() == node.getIgnoreReorderedChildren()) {
                    logger.warn("Redundant '{}: {}' for node '{}' defined in '{}'", META_IGNORE_REORDERED_CHILDREN,
                            node.getIgnoreReorderedChildren(), node.getPath(), definitionNode.getOrigin());
                } else {
                    logger.warn("Overriding '{}' for node '{}' defined in '{}' which was {} before.",
                            META_IGNORE_REORDERED_CHILDREN, node.getPath(), definitionNode.getOrigin(),
                            node.getIgnoreReorderedChildren());
                }
            }
            node.setIgnoreReorderedChildren(definitionNode.getIgnoreReorderedChildren());
            node.addDefinitionItem(definitionNode);
        }

        final ConfigurationNodeImpl parent = node.getParent();
        if (parent != null && definitionNode.getOrderBefore() != null) {
            final String orderBefore = definitionNode.getOrderBefore();
            if (parent.getIgnoreReorderedChildren() != null && parent.getIgnoreReorderedChildren()) {
                logger.warn("Potential unnecessary orderBefore: '{}' for node '{}' defined in '{}': parent '{}' already configured with '{}: true'",
                        orderBefore, node.getPath(), definitionNode.getOrigin(), parent.getPath(), META_IGNORE_REORDERED_CHILDREN);
            }
            final boolean orderFirst = "".equals(orderBefore);
            final String orderBeforeIndexedName = SnsUtils.createIndexedName(orderBefore);
            if (!orderFirst) {
                if (node.getName().equals(orderBeforeIndexedName)) {
                    final String msg = String.format("Invalid orderBefore: '%s' for node '%s' defined in '%s': targeting this node itself.",
                            orderBeforeIndexedName, node.getPath(), definitionNode.getOrigin());
                    throw new IllegalArgumentException(msg);
                }
                if (!parent.getNodes().containsKey(orderBeforeIndexedName)) {
                    final String msg = String.format("Invalid orderBefore: '%s' for node '%s' defined in '%s': no sibling named '%s'.",
                            orderBeforeIndexedName, node.getPath(), definitionNode.getOrigin(), orderBeforeIndexedName);
                    throw new IllegalArgumentException(msg);
                }
            }
            boolean first = true;
            boolean prevIsSrc = false;
            for (String name : parent.getNodes().keySet()) {
                if (name.equals(node.getName())) {
                    // current == src
                    if (first && orderFirst) {
                        // src already first
                        logger.warn("Unnecessary orderBefore: '' (first) for node '{}' defined in '{}': already first child of '{}'.",
                                node.getPath(), definitionNode.getOrigin(), parent.getPath());
                        break;
                    }
                    // track src for next loop, once
                    prevIsSrc = true;
                } else if (orderFirst) {
                    // current != src != first
                    parent.orderBefore(node.getName(), name);
                    break;
                } else if (name.equals(orderBeforeIndexedName)) {
                    // found dest: only reorder if prev != src
                    if (prevIsSrc) {
                        // previous was src, current is dest: already is right order
                        logger.warn("Unnecessary orderBefore: '{}' for node '{}' defined in '{}': already ordered before sibling '{}'.",
                                orderBefore, node.getPath(), definitionNode.getOrigin(), orderBeforeIndexedName);
                    } else {
                        // dest < src: reorder
                        parent.orderBefore(node.getName(), orderBeforeIndexedName);
                    }
                    break;
                } else {
                    prevIsSrc = false;
                }
                first = false;
            }
        }

        for (DefinitionPropertyImpl property: definitionNode.getModifiableProperties().values()) {
            mergeProperty(node, property);
        }

        requirePrimaryType(node, definitionNode);

        final Map<String, ConfigurationNodeImpl> children = node.getModifiableNodes();
        for (DefinitionNodeImpl definitionChild : definitionNode.getModifiableNodes().values()) {
            final String indexedName = SnsUtils.createIndexedName(definitionChild.getName());
            final ConfigurationNodeImpl child;
            if (children.containsKey(indexedName)) {
                child = children.get(indexedName);
                if (child.isDeleted()) {
                    logger.warn("Trying to modify already deleted node '{}', skipping.", child.getPath());
                    continue;
                }
            } else {
                if (isAndRemainsNonConfigurationNode(node, indexedName, definitionChild.getCategory())) {
                    logger.warn("Trying to modify non-configuration node '{}', skipping.", definitionChild.getPath());
                    continue;
                }
                child = createChildNode(node, indexedName, definitionChild);
                if (child == null) {
                    continue;
                }
            }
            mergeNode(child, definitionChild);
        }
    }

    private void keepOnlyFirstSns(final ConfigurationNodeImpl node, final String indexedName) {
        if (SnsUtils.hasSns(indexedName, node.getNodes().keySet())) {
            final Pair<String, Integer> nameAndIndex = SnsUtils.splitIndexedName(indexedName);
            final List<String> namesToDelete = new ArrayList<>();
            for (String siblingIndexedName : node.getNodes().keySet()) {
                final Pair<String, Integer> siblingNameAndIndex = SnsUtils.splitIndexedName(siblingIndexedName);
                if (siblingNameAndIndex.getLeft().equals(nameAndIndex.getLeft()) && siblingNameAndIndex.getRight() > 1) {
                    namesToDelete.add(siblingIndexedName);
                }
            }
            for (String nameToDelete : namesToDelete) {
                node.removeNode(nameToDelete, false);
            }
        }
    }

    private boolean isAndRemainsNonConfigurationNode(final ConfigurationNodeImpl node,
                                                     final String indexedChildNodeName,
                                                     final ConfigurationItemCategory override) {
        final Pair<ConfigurationItemCategory, DefinitionItemImpl> categoryAndDefinition =
                node.getChildNodeCategorySettings(SnsUtils.getUnindexedName(indexedChildNodeName));

        return categoryAndDefinition != null
                && categoryAndDefinition.getLeft() != ConfigurationItemCategory.CONFIGURATION
                && override != ConfigurationItemCategory.CONFIGURATION;
    }

    private boolean isAndRemainsNonConfigurationProperty(final ConfigurationNodeImpl node,
                                                         final String propertyName,
                                                         final ConfigurationItemCategory override) {
        final Pair<ConfigurationItemCategory, DefinitionItemImpl> categoryAndDefinition =
                node.getChildPropertyCategorySettings(propertyName);

        return categoryAndDefinition != null
                && categoryAndDefinition.getLeft() != ConfigurationItemCategory.CONFIGURATION
                && override != ConfigurationItemCategory.CONFIGURATION;
    }

    private void markNodeAsDeletedBy(final ConfigurationNodeImpl node, final DefinitionNodeImpl definitionNode) {
        node.setDeleted(true);
        node.addDefinitionItem(definitionNode);
        node.clearNodes();
        node.clearProperties();
        node.getParent().clearChildNodeCategorySettings(node.getName());
    }

    private ConfigurationNodeImpl getOrCreateRootForDefinition(final ContentDefinitionImpl definition) {
        final DefinitionNodeImpl definitionNode = definition.getModifiableNode();
        final String definitionRootPath = definitionNode.getPath();
        final String[] pathSegments = getPathSegments(definitionRootPath);
        int segmentsConsumed = 0;

        ConfigurationNodeImpl rootForDefinition = root;
        while (segmentsConsumed < pathSegments.length
                && rootForDefinition.getNodes().containsKey(pathSegments[segmentsConsumed])) {
            rootForDefinition = rootForDefinition.getModifiableNodes().get(pathSegments[segmentsConsumed]);
            segmentsConsumed++;
        }

        if (rootForDefinition.isDeleted()) {
            logger.warn("Content definition rooted at '{}' conflicts with deleted node at '{}', skipping.",
                    definitionNode.getPath(), rootForDefinition.getPath());
            return null;
        }

        if (pathSegments.length > segmentsConsumed + 1) {
            // this definition is rooted more than 1 node level deeper than a leaf node of the current tree.
            // that's unsupported, because it is likely to create models that cannot be persisted to JCR.
            String msg = String.format("%s contains definition rooted at unreachable node '%s'. "
                            + "Closest ancestor is at '%s'.", definition.getOrigin(), definitionRootPath,
                    rootForDefinition.getPath());
            throw new IllegalStateException(msg);
        }

        if (pathSegments.length > segmentsConsumed) {
            final String nodeName = pathSegments[segmentsConsumed];
            if (isAndRemainsNonConfigurationNode(rootForDefinition, nodeName, definition.getNode().getCategory())) {
                logger.warn("Trying to modify non-configuration node '{}', skipping.", definition.getNode().getPath());
                return null;
            }
            rootForDefinition =
                    createChildNode(rootForDefinition, nodeName, definition.getModifiableNode());
        } else {
            if (rootForDefinition == root && definitionNode.isDelete()) {
                throw new IllegalArgumentException("Deleting the root node is not supported.");
            }
        }

        return rootForDefinition;
    }

    private ConfigurationNodeImpl createChildNode(final ConfigurationNodeImpl parent, final String name,
                                                  final DefinitionNodeImpl definitionNode) {
        final ConfigurationNodeImpl node = new ConfigurationNodeImpl();

        if (definitionNode.isDelete()) {
            final String msg = String.format("%s: Trying to %sdelete node %s that does not exist.",
                    definitionNode.getOrigin(), definitionNode.isDeleted() ? "" : "merge ", definitionNode.getPath());
            logger.warn(msg);
            if (definitionNode.isDeleted()) {
                return null;
            }
        }

        final Pair<String, Integer> nameAndIndex = SnsUtils.splitIndexedName(name);

        if (definitionNode.getCategory() != null) {
            if (definitionNode.getCategory() == ConfigurationItemCategory.CONFIGURATION) {
                parent.clearChildNodeCategorySettings(nameAndIndex.getLeft());
                if (definitionNode.getNodes().size() == 0 && definitionNode.getProperties().size() == 0) {
                    return null;
                }
            } else {
                parent.setChildNodeCategorySettings(nameAndIndex.getLeft(), definitionNode.getCategory(), definitionNode);
                return null;
            }
        }

        if (nameAndIndex.getRight() > 1) {
            final String expectedSibling = SnsUtils.createIndexedName(nameAndIndex.getLeft(), nameAndIndex.getRight() - 1);
            if (!parent.getNodes().containsKey(expectedSibling)) {
                final String msg = String.format("%s defines node '%s', but no sibling named '%s' was found",
                        definitionNode.getOrigin(), definitionNode.getPath(), expectedSibling);
                throw new IllegalStateException(msg);
            }
        }

        node.setName(name);
        node.setParent(parent);
        node.addDefinitionItem(definitionNode);

        parent.addNode(name, node);

        return node;
    }

    private void mergeProperty(final ConfigurationNodeImpl parent,
                               final DefinitionPropertyImpl definitionProperty) {
        // a node should have a back-reference to any definition that changes its properties
        parent.addDefinitionItem(definitionProperty.getParent());

        final Map<String, ConfigurationPropertyImpl> properties = parent.getModifiableProperties();
        final String name = definitionProperty.getName();
        final PropertyOperation op = definitionProperty.getOperation();

        ConfigurationPropertyImpl property;
        if (properties.containsKey(name)) {
            property = properties.get(name);

            if (property.isDeleted()) {
                logger.warn("Property '{}' defined in '{}' has already been deleted. This property is not re-created.",
                        property.getPath(), definitionProperty.getOrigin());
            }

            // property already exists, so its parent has this property registered as configuration
            final ConfigurationItemCategory category = definitionProperty.getCategory();
            if (category != null && category != ConfigurationItemCategory.CONFIGURATION) {
                property.setDeleted(true);
                property.addDefinitionItem(definitionProperty);
                parent.setChildPropertyCategorySettings(name, category, definitionProperty);
                return;
            }

            if (op == DELETE) {
                property.setDeleted(true);
                property.addDefinitionItem(definitionProperty);
                // no need to clear category on parent
                return;
            }

            if (property.getType() != definitionProperty.getType()) {
                handleTypeConflict(property, definitionProperty, op == OVERRIDE);
            }

            if (property.getValueType() != definitionProperty.getValueType()) {
                handleValueTypeConflict(property, definitionProperty, op == OVERRIDE);
            }

            requireOverrideOperationForPrimaryType(definitionProperty, property, op == OVERRIDE);
            requireOverrideOperationForMixinTypes(definitionProperty, property, op);
        } else {
            final ConfigurationItemCategory category = definitionProperty.getCategory();
            if (isAndRemainsNonConfigurationProperty(parent, definitionProperty.getName(), category)) {
                logger.warn("Trying to modify non-configuration property '{}', skipping.", definitionProperty.getPath());
                return;
            }
            if (category != null) {
                if (category == ConfigurationItemCategory.CONFIGURATION) {
                    parent.clearChildPropertyCategorySettings(name);
                } else {
                    parent.setChildPropertyCategorySettings(name, definitionProperty.getCategory(), definitionProperty);
                    return;
                }
            }

            if (op == DELETE) {
                final String msg = String.format("%s: Trying to delete property %s that does not exist.",
                        definitionProperty.getOrigin(), definitionProperty.getPath());
                logger.warn(msg);
                return;
            }

            // create new property
            property = new ConfigurationPropertyImpl();

            property.setName(name);
            property.setParent(parent);
            property.setType(definitionProperty.getType());
            property.setValueType(definitionProperty.getValueType());
        }

        if (op == REPLACE) {
            warnIfValuesAreEqual(definitionProperty, property);
        }

        property.addDefinitionItem(definitionProperty);
        if (PropertyType.SINGLE == definitionProperty.getType()) {
            property.setValue(definitionProperty.getValue());
        } else {
            if (op == ADD) {
                addValues(definitionProperty, property);
            } else {
                property.setValues(definitionProperty.getValues());
            }
        }

        parent.addProperty(name, property);
    }

    private void handleTypeConflict(final ConfigurationPropertyImpl property,
                                    final DefinitionPropertyImpl definitionProperty, boolean isOverride) {
        if (isOverride) {
            property.setType(definitionProperty.getType());
            property.setValue(null);
            property.setValues(null);
        } else {
            final String msg = String.format("Property %s already exists with type '%s', as determined by %s, "
                            + "but type '%s' is requested in %s.",
                    property.getPath(), property.getType(), property.getOrigin(), definitionProperty.getType(),
                    definitionProperty.getOrigin());
            throw new IllegalStateException(msg);
        }
    }

    private void handleValueTypeConflict(final ConfigurationPropertyImpl property,
                                         final DefinitionPropertyImpl definitionProperty, boolean isOverride) {
        if (isOverride) {
            property.setValueType(definitionProperty.getValueType());
            property.setValue(null);
            property.setValues(null);
        } else {
            final String msg = String.format("Property %s already exists with value type '%s', "
                            + "as determined by %s, but value type '%s' is requested in %s.",
                    property.getPath(), property.getValueType(), property.getOrigin(),
                    definitionProperty.getValueType(), definitionProperty.getOrigin());
            throw new IllegalStateException(msg);
        }
    }

    private void requireOverrideOperationForPrimaryType(final DefinitionPropertyImpl definitionProperty,
                                                        final ConfigurationPropertyImpl property,
                                                        final boolean isOverride) {
        if (property.getName().equals(JCR_PRIMARYTYPE)
                && !property.getValue().getString().equals(definitionProperty.getValue().getString())
                && !isOverride) {
            final String msg = String.format("Property %s is already defined on node %s as determined by %s, but change is requested in %s. "
                            + "Use 'operation: override' if you really intend to change the value of this property.",
                    JCR_PRIMARYTYPE, property.getParent().getPath(), property.getOrigin(), definitionProperty.getOrigin());
            throw new IllegalStateException(msg);
        }
    }

    private void requireOverrideOperationForMixinTypes(final DefinitionPropertyImpl definitionProperty,
                                                       final ConfigurationPropertyImpl property,
                                                       final PropertyOperation op) {
        if (property.getName().equals(JCR_MIXINTYPES) && op != ADD) {
            final List<String> replacedMixins = Arrays.stream(definitionProperty.getValues())
                    .map(ValueImpl::getString)
                    .collect(Collectors.toList());
            final List<String> missingMixins = Arrays.stream(property.getValues())
                    .map(ValueImpl::getString)
                    .filter(mixin -> !replacedMixins.contains(mixin))
                    .collect(Collectors.toList());

            if (missingMixins.size() > 0 && op != OVERRIDE) {
                final String msg = String.format("Property %s is already defined on node %s, and replace operation of "
                                + "%s would remove values %s. Use 'operation: override' if you really intend to remove "
                                + "these values.",
                        JCR_MIXINTYPES, property.getParent().getPath(), definitionProperty.getOrigin(), missingMixins.toString());
                throw new IllegalStateException(msg);
            }
        }
    }

    private void requirePrimaryType(final ConfigurationNodeImpl node, final DefinitionNodeImpl definitionNode) {
        if (!node.getProperties().containsKey(JCR_PRIMARYTYPE)) {
            final String msg = String.format("Node '%s' defined at '%s' is missing the required %s property.",
                    definitionNode.getPath(), definitionNode.getOrigin(), JCR_PRIMARYTYPE);
            throw new IllegalStateException(msg);
        }
    }

    private void addValues(final DefinitionPropertyImpl definitionProperty, final ConfigurationPropertyImpl property) {
        // TODO: need to handle PropertyType.SET?

        final ValueImpl[] existingValues = property.getValues();
        if (existingValues == null) {
            logger.warn("Property '{}' defined in '{}' claims to ADD values, but property doesn't exist yet. Applying default behaviour.",
                    definitionProperty.getPath(), definitionProperty.getOrigin());
            property.setValues(definitionProperty.getValues());
        } else {
            List<ValueImpl> values = Arrays.stream(existingValues).collect(Collectors.toList());
            values.addAll(Arrays.asList(definitionProperty.getValues()));
            property.setValues(values.toArray(new ValueImpl[values.size()]));
        }
    }

    private void warnIfValuesAreEqual(final DefinitionPropertyImpl definitionProperty,
                                      final ConfigurationPropertyImpl property) {
        if (PropertyType.SINGLE == property.getType()) {
            final ValueImpl existingValue = property.getValue();
            if (existingValue != null) {
                if (definitionProperty.getValue().equals(property.getValue())) {
                    logger.warn("Property '{}' defined in '{}' specifies value equivalent to existing property.",
                            definitionProperty.getPath(), definitionProperty.getOrigin());
                }
            }
        } else {
            final ValueImpl[] existingValues = property.getValues();
            if (existingValues != null) {
                final ValueImpl[] definitionValues = definitionProperty.getValues();
                if (existingValues.length == definitionValues.length) {
                    for (int i = 0; i < existingValues.length; i++) {
                        if (!existingValues[i].equals(definitionValues[i])) {
                            return;
                        }
                    }
                    logger.warn("Property '{}' defined in '{}' specifies values equivalent to existing property.",
                            definitionProperty.getPath(), definitionProperty.getOrigin());
                }
            }
        }
    }

    private String[] getPathSegments(final String path) {
        if ("/".equals(path)) {
            return new String[0];
        }

        final String[] pathSegments = path.substring(1).split("/");
        for (int i = 0; i < pathSegments.length; i++) {
            pathSegments[i] = SnsUtils.createIndexedName(pathSegments[i]);
        }
        return pathSegments;
    }

    private void pruneDeletedItems(final ConfigurationNodeImpl node) {
        if (node.isDeleted()) {
            node.getParent().removeNode(node.getName(), false);
            return;
        }

        final Map<String, ConfigurationPropertyImpl> propertyMap = node.getModifiableProperties();
        final List<String> deletedProperties = propertyMap.keySet().stream()
                .filter(propertyName -> propertyMap.get(propertyName).isDeleted())
                .collect(Collectors.toList());
        deletedProperties.forEach(node::removeProperty);

        final Map<String, ConfigurationNodeImpl> childMap = node.getModifiableNodes();
        final List<String> children = new ArrayList<>(childMap.keySet());
        children.forEach(name -> pruneDeletedItems(childMap.get(name)));
    }

}
