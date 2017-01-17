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
package org.onehippo.cm.engine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;
import org.onehippo.cm.api.model.ConfigDefinition;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;

import static org.junit.Assert.assertEquals;

public class ParserValueTest extends AbstractBaseTest {

    @Test
    public void expect_value_test_loads() throws IOException, ParserException {
        final FileConfigurationReader.ReadResult result = readFromResource("/parser/value_test/repo-config.yaml");
        final Map<String, Configuration> configurations = result.getConfigurations();

        assertEquals(1, configurations.size());

        final Configuration base = assertConfiguration(configurations, "base", new String[0], 1);
        final Project project = assertProject(base, "project1", new String[0], 1);
        final Module module = assertModule(project, "module1", new String[0], 1);
        final Source source = assertSource(module, "base.yaml", 3);

        final ConfigDefinition baseDefinition = assertDefinition(source, 0, ConfigDefinition.class);
        final DefinitionNode baseNode = assertNode(baseDefinition, "/base", "base", baseDefinition, false, 0, 6);
        assertProperty(baseNode, "/base/binary", "binary", baseDefinition, "hello world".getBytes());
        assertProperty(baseNode, "/base/boolean", "boolean", baseDefinition, true);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        calendar.set(2015, 9, 21, 7, 28, 0);
        assertProperty(baseNode, "/base/date", "date", baseDefinition, calendar);
        assertProperty(baseNode, "/base/double", "double", baseDefinition, 3.1415);
        assertProperty(baseNode, "/base/long", "long", baseDefinition, (long) 42);
        assertProperty(baseNode, "/base/string", "string", baseDefinition, "hello world");

        final ConfigDefinition stringDefinition = assertDefinition(source, 1, ConfigDefinition.class);
        final DefinitionNode stringNode = assertNode(stringDefinition, "/string", "string", stringDefinition, false, 0, 8);
        assertProperty(stringNode, "/string/strBoolean", "strBoolean", stringDefinition, "true");
        assertProperty(stringNode, "/string/strDate", "strDate", stringDefinition, "2015-10-21T07:28:00+8:00");
        assertProperty(stringNode, "/string/strDouble", "strDouble", stringDefinition, "3.1415");
        assertProperty(stringNode, "/string/strLong", "strLong", stringDefinition, "42");
        assertProperty(stringNode, "/string/strWithQuotes", "strWithQuotes", stringDefinition, "string ' \"");
        assertProperty(stringNode, "/string/strWithLeadingSingleQuote", "strWithLeadingSingleQuote", stringDefinition, "' \" string");
        assertProperty(stringNode, "/string/strWithLeadingDoubleQuote", "strWithLeadingDoubleQuote", stringDefinition, "\" ' string");
        assertProperty(stringNode, "/string/strWithLineBreaks", "strWithLineBreaks", stringDefinition, "line one\nline two\n");

        final ConfigDefinition emptyDefinition = assertDefinition(source, 2, ConfigDefinition.class);
        final DefinitionNode emptyNode = assertNode(emptyDefinition, "/empty", "empty", emptyDefinition, false, 0, 6);
        assertProperty(emptyNode, "/empty/emptyBinary", "emptyBinary", emptyDefinition, new Object[0]);
        assertProperty(emptyNode, "/empty/emptyBoolean", "emptyBoolean", emptyDefinition, new Object[0]);
        assertProperty(emptyNode, "/empty/emptyDate", "emptyDate", emptyDefinition, new Object[0]);
        assertProperty(emptyNode, "/empty/emptyDouble", "emptyDouble", emptyDefinition, new Object[0]);
        assertProperty(emptyNode, "/empty/emptyLong", "emptyLong", emptyDefinition, new Object[0]);
        assertProperty(emptyNode, "/empty/emptyString", "emptyString", emptyDefinition, new Object[0]);
    }

    @Test
    public void expect_property_value_map_without_type_to_yield_string() throws ParserException {
        final SourceParser sourceParser = new SourceParser();
        final ConfigurationImpl configuration = new ConfigurationImpl("configuration");
        final ProjectImpl project = new ProjectImpl("project", configuration);
        final ModuleImpl module = new ModuleImpl("module", project);

        final String yaml =
                "instructions:\n" +
                "  - config:\n" +
                "      - /node:\n" +
                "          - property:\n" +
                "              value: []";
        final InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        sourceParser.parse("dummy.yaml", inputStream, module);

        final Source source = assertSource(module, "dummy.yaml", 1);
        final ConfigDefinition definition = assertDefinition(source, 0, ConfigDefinition.class);
        final DefinitionNode node = assertNode(definition, "/node", "node", definition, false, 0, 1);
        assertProperty(node, "/node/property", "property", definition, new Object[0]);
    }

}
