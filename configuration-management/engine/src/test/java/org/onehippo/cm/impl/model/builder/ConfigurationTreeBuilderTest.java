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

package org.onehippo.cm.impl.model.builder;

import java.util.List;

import org.junit.Test;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.ConfigurationProperty;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.impl.model.ConfigurationNodeImpl;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ConfigurationTreeBuilderTest extends AbstractBuilderBaseTest {

    private final ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder();

    @Test
    public void simple_single_definition() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property1: bla1\n"
                + "    - property2: bla2";

        final List<Definition> definitions = parseNoSort(yaml);
        final ContentDefinitionImpl definition = (ContentDefinitionImpl)definitions.get(0);
        builder.push(definition);
        final ConfigurationNodeImpl root = builder.build();

                assertEquals("[]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[property1, property2]", sortedCollectionToString(a.getProperties()));
        assertEquals("[]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void complex_single_definition() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property2: bla2\n"
                + "    - property1: bla1\n"
                + "    - /b:\n"
                + "      - /d:\n"
                + "        - property4: bla4\n"
                + "      - property3: bla3\n"
                + "      - /c:\n"
                + "        - property5: bla5";

        final List<Definition> definitions = parseNoSort(yaml);
        final ContentDefinitionImpl definition = (ContentDefinitionImpl)definitions.get(0);
        builder.push(definition);
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[property2, property1]", sortedCollectionToString(a.getProperties()));
        assertEquals("[b]", sortedCollectionToString(a.getNodes()));
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property3]", sortedCollectionToString(b.getProperties()));
        assertEquals("[d, c]", sortedCollectionToString(b.getNodes()));
        final ConfigurationNode c = b.getNodes().get("c");
        assertEquals("[property5]", sortedCollectionToString(c.getProperties()));
        assertEquals("[]", sortedCollectionToString(c.getNodes()));
        final ConfigurationNode d = b.getNodes().get("d");
        assertEquals("[property4]", sortedCollectionToString(d.getProperties()));
        assertEquals("[]", sortedCollectionToString(d.getNodes()));
    }

    @Test
    public void invalid_single_definition() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a/b:\n"
                + "    - property1: bla1";

        final List<Definition> definitions = parseNoSort(yaml);
        final ContentDefinitionImpl definition = (ContentDefinitionImpl)definitions.get(0);

        try {
            builder.push(definition);
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-configuration/test-project/test-module [string] contains definition rooted at unreachable node '/a/b'. Closest ancestor is at '/'.", e.getMessage());
        }
    }

    @Test
    public void root_node_property() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /:\n"
                + "    - property1: bla1\n"
                + "    - /a:\n"
                + "      - property2: [bla2, bla3]";

        final List<Definition> definitions = parseNoSort(yaml);
        final ContentDefinitionImpl definition = (ContentDefinitionImpl)definitions.get(0);
        builder.push(definition);
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[property1]", sortedCollectionToString(root.getProperties()));
        assertEquals(PropertyType.SINGLE, root.getProperties().get("property1").getType());
        assertEquals("[a]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[property2]", sortedCollectionToString(a.getProperties()));
        assertEquals(PropertyType.LIST, a.getProperties().get("property2").getType());
        assertEquals("[]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void merge_two_definitions_same_module() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property2: bla2\n"
                + "    - property1: bla1\n"
                + "    - /b:\n"
                + "      - /d:\n"
                + "        - property4: bla4\n"
                + "      - property8: bla8\n"
                + "      - /c:\n"
                + "        - property5: bla5\n"
                + "  - /a/d:\n"
                + "    - property7: bla7";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[property2, property1]", sortedCollectionToString(a.getProperties()));
        assertEquals("[b, d]", sortedCollectionToString(a.getNodes()));
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property8]", sortedCollectionToString(b.getProperties()));
        assertEquals("[d, c]", sortedCollectionToString(b.getNodes()));
        final ConfigurationNode d = a.getNodes().get("d");
        assertEquals("[property7]", sortedCollectionToString(d.getProperties()));
        assertEquals("[]", sortedCollectionToString(d.getNodes()));
    }

    @Test
    public void merge_two_definitions_separate_modules() throws Exception {
        final String yaml1 = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property2: bla2\n"
                + "    - property1: bla1\n"
                + "    - /b:\n"
                + "      - /d:\n"
                + "        - property4: bla4\n"
                + "      - property8: bla8\n"
                + "      - /c:\n"
                + "        - property5: bla5";

        final List<Definition> definitions1 = parseNoSort(yaml1);
        builder.push((ContentDefinitionImpl)definitions1.get(0));

        final String yaml2 = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property3: bla3\n"
                + "    - /e:\n"
                + "      - property6: bla6\n"
                + "    - /b:\n"
                + "      - property7: bla7\n"
                + "      - /f:\n"
                + "        - property9: bla9";

        final List<Definition> definitions2 = parseNoSort(yaml2);
        builder.push((ContentDefinitionImpl)definitions2.get(0));
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[property2, property1, property3]", sortedCollectionToString(a.getProperties()));
        assertEquals("[b, e]", sortedCollectionToString(a.getNodes()));
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property8, property7]", sortedCollectionToString(b.getProperties()));
        assertEquals("[d, c, f]", sortedCollectionToString(b.getNodes()));
        final ConfigurationNode e = a.getNodes().get("e");
        assertEquals("[property6]", sortedCollectionToString(e.getProperties()));
        assertEquals("[]", sortedCollectionToString(e.getNodes()));
        final ConfigurationNode f = b.getNodes().get("f");
        assertEquals("[property9]", sortedCollectionToString(f.getProperties()));
        assertEquals("[]", sortedCollectionToString(f.getNodes()));
    }

    @Test
    public void override_single_property() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property1: bla1\n"
                + "    - /b:\n"
                + "      - property2: bla2\n"
                + "  - /a/b:\n"
                + "    - property2: bla3";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[property1]", sortedCollectionToString(a.getProperties()));
        assertEquals("[b]", sortedCollectionToString(a.getNodes()));
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property2]", sortedCollectionToString(b.getProperties()));
        assertEquals("[]", sortedCollectionToString(b.getNodes()));
        final ConfigurationProperty property2 = b.getProperties().get("property2");
        assertEquals(PropertyType.SINGLE, property2.getType());
        assertEquals(ValueType.STRING, property2.getValueType());
        assertEquals("bla3", property2.getValue().getString());
    }

    @Test
    public void override_list_property() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property1: bla1\n"
                + "    - /b:\n"
                + "      - property2: [bla2]\n"
                + "  - /a/b:\n"
                + "    - property2: [bla3, bla4]";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property2]", sortedCollectionToString(b.getProperties()));
        final ConfigurationProperty property2 = b.getProperties().get("property2");
        assertEquals(PropertyType.LIST, property2.getType());
        assertEquals(ValueType.STRING, property2.getValueType());
        assertEquals("bla3", property2.getValues()[0].getString());
        assertEquals("bla4", property2.getValues()[1].getString());
    }

    @Test
    public void reject_property_if_different_multiplicity() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property1: bla1\n"
                + "    - /b:\n"
                + "      - property2: bla2\n"
                + "  - /a/b:\n"
                + "    - property2: [bla3, bla4]";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));

        try {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property /a/b/property2 already exists with type single, but type list is requested in test-configuration/test-project/test-module [string].", e.getMessage());
        }
    }

    @Test
    public void reject_property_if_different_value_type() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property1: bla1\n"
                + "    - /b:\n"
                + "      - property2: [bla2]\n"
                + "  - /a/b:\n"
                + "    - property2: [34]";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));

        try {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property /a/b/property2 already exists with value type string, but value type long is requested in test-configuration/test-project/test-module [string].", e.getMessage());
        }
    }

    @Test
    public void reorder_existing_node_to_first() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: [bla1]\n"
                + "    - /c:\n"
                + "      - property2: [bla2]\n"
                + "    - /d:\n"
                + "      - property3: [bla3]\n"
                + "  - /a/d:\n"
                + "    - .meta:order-before: b";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[d, b, c]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void reorder_existing_node_to_earlier() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: [bla1]\n"
                + "    - /c:\n"
                + "      - property2: [bla2]\n"
                + "    - /d:\n"
                + "      - property3: [bla3]\n"
                + "    - /e:\n"
                + "      - property4: [bla4]\n"
                + "    - /f:\n"
                + "      - property5: [bla5]\n"
                + "  - /a/e:\n"
                + "    - .meta:order-before: c";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[b, e, c, d, f]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void reorder_existing_node_to_later() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: [bla1]\n"
                + "    - /c:\n"
                + "      - property2: [bla2]\n"
                + "    - /d:\n"
                + "      - property3: [bla3]\n"
                + "    - /e:\n"
                + "      - property4: [bla4]\n"
                + "    - /f:\n"
                + "      - property5: [bla5]\n"
                + "  - /a/c:\n"
                + "    - .meta:order-before: f";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[b, d, e, c, f]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void reorder_new_root() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - /c:\n"
                + "        - property1: [bla1]\n"
                + "      - /d:\n"
                + "        - property2: [bla2]\n"
                + "  - /a/b/e:\n"
                + "    - .meta:order-before: d";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[c, e, d]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void reorder_new_child() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - /c:\n"
                + "        - property1: [bla1]\n"
                + "      - /d:\n"
                + "        - property2: [bla2]\n"
                + "  - /a/b:\n"
                + "    - /e:\n"
                + "      - .meta:order-before: c";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[e, c, d]", sortedCollectionToString(b.getNodes()));
    }
}
