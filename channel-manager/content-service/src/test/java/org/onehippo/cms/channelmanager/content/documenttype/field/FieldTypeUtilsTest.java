/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype.field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.sort.FieldSorter;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.AbstractFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.ChoiceFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.ChoiceFieldUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.CompoundFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldsInformation;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FormattedTextFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.MultilineStringFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.RichTextFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.StringFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.ValidationService;
import org.onehippo.cms.services.validation.api.ValidatorInstance;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({NamespaceUtils.class, FieldTypeFactory.class, ChoiceFieldUtils.class, ContentTypeContext.class, HippoServiceRegistry.class})
public class FieldTypeUtilsTest {
    private static final String PROPERTY_FIELD_PLUGIN = "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin";
    private static final String NODE_FIELD_PLUGIN = "org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin";
    private static final String COMPOUND_FIELD_PLUGIN = "org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin";
    private static final String CHOICE_FIELD_PLUGIN = "org.onehippo.forge.contentblocks.ContentBlocksFieldPlugin";

    @Before
    public void setup() {
        PowerMock.mockStatic(ChoiceFieldUtils.class);
        PowerMock.mockStatic(ContentTypeContext.class);
        PowerMock.mockStatic(FieldTypeFactory.class);
        PowerMock.mockStatic(HippoServiceRegistry.class);
        PowerMock.mockStatic(NamespaceUtils.class);
    }

    @Test
    public void determineIgnoredValidator() {
        final ValidationService validationService = createMock(ValidationService.class);
        final FieldType fieldType = createMock(AbstractFieldType.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);

        expect(HippoServiceRegistry.getService(ValidationService.class)).andReturn(validationService);
        replayAll();

        FieldTypeUtils.determineValidators(fieldType, fieldContext, Collections.singletonList("optional"));
        verifyAll();
    }

    @Test
    public void determineRequiredValidator() {
        final ValidationService validationService = createMock(ValidationService.class);
        final FieldType fieldType = createMock(AbstractFieldType.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);

        expect(HippoServiceRegistry.getService(ValidationService.class)).andReturn(validationService);

        fieldType.setRequired(true);
        expectLastCall();

        replayAll();

        FieldTypeUtils.determineValidators(fieldType, fieldContext, Collections.singletonList("required"));
        verifyAll();
    }

    @Test
    public void determineZeroValidators() {
        final FieldType fieldType = createMock(AbstractFieldType.class);
        final FieldTypeContext fieldTypeContext = createMock(FieldTypeContext.class);

        FieldTypeUtils.determineValidators(fieldType, fieldTypeContext, Collections.emptyList());
    }

    @Test
    public void determineTwoSupportedValidators() {
        final ValidationService validationService = createMock(ValidationService.class);
        final ValidatorInstance email = createMock(ValidatorInstance.class);
        final ValidatorInstance references = createMock(ValidatorInstance.class);
        final FieldType fieldType = createMock(AbstractFieldType.class);
        final FieldTypeContext fieldTypeContext = createMock(FieldTypeContext.class);

        expect(HippoServiceRegistry.getService(ValidationService.class)).andReturn(validationService);
        expect(validationService.getValidator("email")).andReturn(email);
        expect(validationService.getValidator("references")).andReturn(references);

        fieldType.addValidatorName("email");
        expectLastCall();

        fieldType.addValidatorName("references");
        expectLastCall();

        replayAll();

        FieldTypeUtils.determineValidators(fieldType, fieldTypeContext, Arrays.asList("email", "references"));
        verifyAll();
    }

    @Test
    public void determineUnknownValidator() {
        final ValidationService validationService = createMock(ValidationService.class);
        final FieldType fieldType = createMock(AbstractFieldType.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final DocumentType docType = createMock(DocumentType.class);

        expect(HippoServiceRegistry.getService(ValidationService.class)).andReturn(validationService);
        expect(validationService.getValidator("unknown-validator")).andReturn(null);
        expect(fieldContext.getParentContext()).andReturn(parentContext);
        expect(parentContext.getDocumentType()).andReturn(docType);
        expect(fieldType.getId()).andReturn("fieldId");
        expect(docType.getId()).andReturn("docTypeId");
        docType.setReadOnlyDueToUnknownValidator(true);
        expectLastCall();
        replayAll();

        FieldTypeUtils.determineValidators(fieldType, fieldContext, Collections.singletonList("unknown-validator"));
        verifyAll();
    }

    @Test
    public void determineUnsupportedValidator() {
        final ValidationService validationService = createMock(ValidationService.class);
        final FieldType fieldType = createMock(AbstractFieldType.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);

        expect(HippoServiceRegistry.getService(ValidationService.class)).andReturn(validationService);

        fieldType.setUnsupportedValidator(eq(true));
        expectLastCall();

        replayAll();

        FieldTypeUtils.determineValidators(fieldType, fieldContext, Collections.singletonList(FieldValidators.IMAGE_REFERENCES));
        verifyAll();
    }

    @Test
    public void determineValidatorsWithoutValidationService() {
        final FieldType fieldType = createMock(AbstractFieldType.class);
        final FieldTypeContext fieldTypeContext = createMock(FieldTypeContext.class);

        expect(HippoServiceRegistry.getService(ValidationService.class)).andReturn(null);
        replayAll();

        FieldTypeUtils.determineValidators(fieldType, fieldTypeContext, Arrays.asList("email", "references"));
        verifyAll();
    }

    @Test
    public void getValidatorIgnoresBlankNames() {
        assertNull(FieldTypeUtils.getValidator(null));
        assertNull(FieldTypeUtils.getValidator(""));
        assertNull(FieldTypeUtils.getValidator(" "));
    }

    @Test
    public void getValidatorWithoutValidationService() {
        expect(HippoServiceRegistry.getService(ValidationService.class)).andReturn(null);
        replayAll();

        FieldTypeUtils.getValidator("test");
        verifyAll();
    }

    @Test
    public void getUnknownValidator() {
        ValidationService validationService = createMock(ValidationService.class);

        expect(HippoServiceRegistry.getService(ValidationService.class)).andReturn(validationService);
        expect(validationService.getValidator("test")).andReturn(null);

        replayAll();

        final ValidatorInstance test = FieldTypeUtils.getValidator("test");
        assertNull(test);
        verifyAll();
    }

    @Test
    public void getKnownValidator() throws ValidationContextException {
        ValidationService validationService = createMock(ValidationService.class);
        ValidatorInstance validator = createMock(ValidatorInstance.class);

        expect(HippoServiceRegistry.getService(ValidationService.class)).andReturn(validationService);
        expect(validationService.getValidator("test")).andReturn(validator);
        expectLastCall();
        replayAll();

        final ValidatorInstance test = FieldTypeUtils.getValidator("test");
        assertNotNull(test);
        verifyAll();
    }

    @Test
    public void populateFieldsNoSorter() {
        final List<FieldType> fields = new ArrayList<>();
        final ContentTypeContext context = createMock(ContentTypeContext.class);

        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.empty());
        expect(context.getContentTypeRoot()).andReturn(null);
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertFalse(fieldsInfo.isAllFieldsIncluded());
        assertFalse(fieldsInfo.getCanCreateAllRequiredFields());
        assertTrue(fieldsInfo.getUnsupportedFieldTypes().isEmpty());

        assertThat(fields.size(), equalTo(0));
        verifyAll();
    }

    @Test
    public void populateFieldsSorterNoFields() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);

        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.emptyList());
        expect(context.getContentTypeRoot()).andReturn(null);
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertTrue(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());
        assertTrue(fieldsInfo.getUnsupportedFieldTypes().isEmpty());

        assertThat(fields.size(), equalTo(0));
        verifyAll();
    }

    @Test
    public void populateFieldsUnsupportedFieldTypeUnknownProperty() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.isProperty()).andReturn(true);
        expect(fieldContext.getType()).andReturn("unknown").atLeastOnce();
        expect(fieldContext.getValidators()).andReturn(Collections.emptyList());
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertFalse(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());
        assertThat(fieldsInfo.getUnsupportedFieldTypes(), equalTo(Collections.singleton("Custom")));

        assertThat(fields.size(), equalTo(0));
        verifyAll();
    }

    @Test
    public void populateFieldsUnsupportedFieldTypePropertyNoEditorConfigNode() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getType()).andReturn("String").atLeastOnce();
        expect(fieldContext.getValidators()).andReturn(Collections.emptyList());
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.empty());
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertFalse(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());
        assertThat(fieldsInfo.getUnsupportedFieldTypes(), equalTo(Collections.singleton("Custom")));

        assertThat(fields.size(), equalTo(0));
        verifyAll();
    }

    @Test
    public void populateFieldsUnsupportedFieldTypePropertyWithoutPlugin() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final Node node = createMock(Node.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getType()).andReturn("String").atLeastOnce();
        expect(fieldContext.getValidators()).andReturn(Collections.emptyList());
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.empty());
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertFalse(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());
        assertThat(fieldsInfo.getUnsupportedFieldTypes(), equalTo(Collections.singleton("Custom")));

        assertThat(fields.size(), equalTo(0));
        verifyAll();
    }

    @Test
    public void populateFieldsUnsupportedFieldTypePropertyWithCustomPlugin() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final Node node = createMock(Node.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getType()).andReturn("String").atLeastOnce();
        expect(fieldContext.getValidators()).andReturn(Collections.emptyList());
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of("Custom plugin"));
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertFalse(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());
        assertThat(fieldsInfo.getUnsupportedFieldTypes(), equalTo(Collections.singleton("Custom")));

        assertThat(fields.size(), equalTo(0));
        verifyAll();
    }

    @Test
    public void populateFieldsStringFieldInstantiationFailure() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final Node node = createMock(Node.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getType()).andReturn("String").atLeastOnce();
        expect(fieldContext.getValidators()).andReturn(Collections.emptyList());
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(PROPERTY_FIELD_PLUGIN));
        expect(FieldTypeFactory.createFieldType(StringFieldType.class)).andReturn(Optional.empty());
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertFalse(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());

        assertThat(fields.size(), equalTo(0));
        verifyAll();
    }

    @Test
    public void populateFieldsRequiredStringFieldInstantiationFailure() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final Node node = createMock(Node.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getType()).andReturn("String").atLeastOnce();
        expect(fieldContext.getValidators()).andReturn(Collections.singletonList("required"));
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(PROPERTY_FIELD_PLUGIN));
        expect(FieldTypeFactory.createFieldType(StringFieldType.class)).andReturn(Optional.empty());
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertFalse(fieldsInfo.isAllFieldsIncluded());
        assertFalse(fieldsInfo.getCanCreateAllRequiredFields());

        assertThat(fields.size(), equalTo(0));
        verifyAll();
    }

    @Test
    public void populateFieldsInvalidStringField() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final Node node = createMock(Node.class);
        final StringFieldType fieldType = createMock(StringFieldType.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getType()).andReturn("String").atLeastOnce();
        expect(fieldContext.getValidators()).andReturn(Collections.singletonList("unknown-validator"));
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(PROPERTY_FIELD_PLUGIN));
        expect(FieldTypeFactory.createFieldType(StringFieldType.class)).andReturn(Optional.of(fieldType));
        expect(fieldType.init(fieldContext)).andReturn(FieldsInformation.allSupported());
        expect(fieldType.isSupported()).andReturn(false);
        expect(fieldType.hasUnsupportedValidator()).andReturn(true);
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertFalse(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());
        assertThat(fieldsInfo.getUnsupportedFieldTypes(), equalTo(Collections.singleton("Custom")));

        assertThat(fields.size(), equalTo(0));
        verifyAll();
    }

    @Test
    public void populateFieldsStringField() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final Node node = createMock(Node.class);
        final StringFieldType fieldType = createMock(StringFieldType.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getType()).andReturn("String");
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(PROPERTY_FIELD_PLUGIN));
        expect(FieldTypeFactory.createFieldType(StringFieldType.class)).andReturn(Optional.of(fieldType));
        expect(fieldType.init(fieldContext)).andReturn(FieldsInformation.allSupported());
        expect(fieldType.isSupported()).andReturn(true);
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertTrue(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());

        assertThat(fields.size(), equalTo(1));
        assertThat(fields.get(0), equalTo(fieldType));
        verifyAll();
    }

    @Test
    public void populateFieldsStringAndMultilineString() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext1 = createMock(FieldTypeContext.class);
        final FieldTypeContext fieldContext2 = createMock(FieldTypeContext.class);
        final Node node = createMock(Node.class);
        final StringFieldType stringField1 = createMock(StringFieldType.class);
        final MultilineStringFieldType multilineStringField = createMock(MultilineStringFieldType.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Arrays.asList(fieldContext1, fieldContext2));
        expect(fieldContext1.getType()).andReturn("String");
        expect(fieldContext2.getType()).andReturn("Text");
        expect(fieldContext1.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(fieldContext2.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(PROPERTY_FIELD_PLUGIN)).times(2);
        expect(FieldTypeFactory.createFieldType(StringFieldType.class)).andReturn(Optional.of(stringField1));
        expect(FieldTypeFactory.createFieldType(MultilineStringFieldType.class)).andReturn(Optional.of(multilineStringField));

        expect(stringField1.init(fieldContext1)).andReturn(FieldsInformation.allSupported());
        expect(stringField1.isSupported()).andReturn(true);

        expect(multilineStringField.init(fieldContext2)).andReturn(FieldsInformation.allSupported());
        expect(multilineStringField.isSupported()).andReturn(true);

        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertTrue(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());

        assertThat(fields.size(), equalTo(2));
        assertThat(fields.get(0), equalTo(stringField1));
        assertThat(fields.get(1), equalTo(multilineStringField));

        verifyAll();
    }

    @Test
    public void populateFieldsHtml() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext1 = createMock(FieldTypeContext.class);
        final FieldTypeContext fieldContext2 = createMock(FieldTypeContext.class);
        final Node node = createMock(Node.class);
        final FormattedTextFieldType formattedTextField = createMock(FormattedTextFieldType.class);
        final RichTextFieldType richTextField = createMock(RichTextFieldType.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Arrays.asList(fieldContext1, fieldContext2));

        expect(fieldContext1.getType()).andReturn("Html");
        expect(fieldContext2.getType()).andReturn("hippostd:html");

        expect(fieldContext1.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(fieldContext2.getEditorConfigNode()).andReturn(Optional.of(node));

        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(PROPERTY_FIELD_PLUGIN)).times(1);
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(NODE_FIELD_PLUGIN)); // Specifically for rich text field

        expect(FieldTypeFactory.createFieldType(FormattedTextFieldType.class)).andReturn(Optional.of(formattedTextField));
        expect(FieldTypeFactory.createFieldType(RichTextFieldType.class)).andReturn(Optional.of(richTextField));

        expect(formattedTextField.init(fieldContext1)).andReturn(FieldsInformation.allSupported());
        expect(formattedTextField.isSupported()).andReturn(true);

        expect(richTextField.init(fieldContext2)).andReturn(FieldsInformation.allSupported());
        expect(richTextField.isSupported()).andReturn(true);

        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertTrue(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());
        assertTrue(fieldsInfo.getUnsupportedFieldTypes().isEmpty());

        assertThat(fields.size(), equalTo(2));
        assertThat(fields.get(0), equalTo(formattedTextField));
        assertThat(fields.get(1), equalTo(richTextField));

        verifyAll();
    }

    @Test
    public void populateFieldsCompoundField() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final Node node = createMock(Node.class);
        final CompoundFieldType fieldType = createMock(CompoundFieldType.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getType()).andReturn("project:compoundtype").anyTimes();
        expect(fieldContext.isProperty()).andReturn(false);
        expect(ChoiceFieldUtils.isChoiceField(fieldContext)).andReturn(false);
        expect(ContentTypeContext.getContentType("project:compoundtype")).andReturn(Optional.of(contentType));
        expect(contentType.isCompoundType()).andReturn(true);
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(COMPOUND_FIELD_PLUGIN));
        expect(FieldTypeFactory.createFieldType(CompoundFieldType.class)).andReturn(Optional.of(fieldType));
        expect(fieldType.init(fieldContext)).andReturn(FieldsInformation.allSupported());
        expect(fieldType.isSupported()).andReturn(true);
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertTrue(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());
        assertTrue(fieldsInfo.getUnsupportedFieldTypes().isEmpty());

        assertThat(fields.size(), equalTo(1));
        assertThat(fields.get(0), equalTo(fieldType));
        verifyAll();
    }

    @Test
    public void populateFieldsEmptyCompoundField() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final Node node = createMock(Node.class);
        final CompoundFieldType fieldType = createMock(CompoundFieldType.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getType()).andReturn("project:compoundtype").anyTimes();
        expect(fieldContext.isProperty()).andReturn(false);
        expect(ChoiceFieldUtils.isChoiceField(fieldContext)).andReturn(false);
        expect(ContentTypeContext.getContentType("project:compoundtype")).andReturn(Optional.of(contentType));
        expect(contentType.isCompoundType()).andReturn(true);
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(COMPOUND_FIELD_PLUGIN));
        expect(FieldTypeFactory.createFieldType(CompoundFieldType.class)).andReturn(Optional.of(fieldType));
        expect(fieldType.init(fieldContext)).andReturn(FieldsInformation.allSupported());
        expect(fieldType.isSupported()).andReturn(false);
        expect(fieldType.hasUnsupportedValidator()).andReturn(false);
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertTrue(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());
        assertTrue(fieldsInfo.getUnsupportedFieldTypes().isEmpty());
        assertTrue(fields.isEmpty());
        verifyAll();
    }

    @Test
    public void populateFieldsInvalidCompoundField() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final Node node = createMock(Node.class);
        final CompoundFieldType fieldType = createMock(CompoundFieldType.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getType()).andReturn("project:compoundtype").anyTimes();
        expect(fieldContext.isProperty()).andReturn(false);
        expect(ChoiceFieldUtils.isChoiceField(fieldContext)).andReturn(false);
        expect(ContentTypeContext.getContentType("project:compoundtype")).andReturn(Optional.of(contentType));
        expect(contentType.isCompoundType()).andReturn(true);
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(COMPOUND_FIELD_PLUGIN));
        expect(FieldTypeFactory.createFieldType(CompoundFieldType.class)).andReturn(Optional.of(fieldType));
        expect(fieldType.init(fieldContext)).andReturn(FieldsInformation.noneSupported());
        expect(fieldType.isSupported()).andReturn(false);
        expect(fieldType.hasUnsupportedValidator()).andReturn(false);
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertFalse(fieldsInfo.isAllFieldsIncluded());
        assertFalse(fieldsInfo.getCanCreateAllRequiredFields());
        assertTrue(fieldsInfo.getUnsupportedFieldTypes().isEmpty());
        assertTrue(fields.isEmpty());
        verifyAll();
    }

    @Test
    public void populateFieldsChoiceField() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final Node node = createMock(Node.class);
        final ChoiceFieldType fieldType = createMock(ChoiceFieldType.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getType()).andReturn("project:choicefieldtype");
        expect(fieldContext.isProperty()).andReturn(false);
        expect(ChoiceFieldUtils.isChoiceField(fieldContext)).andReturn(true);
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(CHOICE_FIELD_PLUGIN));
        expect(FieldTypeFactory.createFieldType(ChoiceFieldType.class)).andReturn(Optional.of(fieldType));
        expect(fieldType.init(fieldContext)).andReturn(FieldsInformation.allSupported());
        expect(fieldType.isSupported()).andReturn(true);
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertTrue(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());
        assertTrue(fieldsInfo.getUnsupportedFieldTypes().isEmpty());

        assertThat(fields.size(), equalTo(1));
        assertThat(fields.get(0), equalTo(fieldType));
        verifyAll();
    }

    @Test
    public void populateFieldsUnknownField() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getType()).andReturn("project:compoundtype").anyTimes();
        expect(fieldContext.isProperty()).andReturn(false);
        expect(fieldContext.getValidators()).andReturn(Collections.emptyList());
        expect(ChoiceFieldUtils.isChoiceField(fieldContext)).andReturn(false);
        expect(ContentTypeContext.getContentType("project:compoundtype")).andReturn(Optional.of(contentType));
        expect(contentType.isCompoundType()).andReturn(false);
        replayAll();

        final FieldsInformation fieldsInfo = FieldTypeUtils.populateFields(fields, context);
        assertFalse(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());
        assertThat(fieldsInfo.getUnsupportedFieldTypes(), equalTo(Collections.singleton("Custom")));

        assertTrue(fields.isEmpty());
        verifyAll();
    }

    @Test
    public void readFieldValues() {
        final StringFieldType field1 = createMock(StringFieldType.class);
        final StringFieldType field2 = createMock(StringFieldType.class);
        final Node node = createMock(Node.class);
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();
        final FieldValue value1 = new FieldValue("one");
        final FieldValue value2 = new FieldValue("two");

        expect(field1.readFrom(node)).andReturn(Optional.empty());
        expect(field2.getId()).andReturn("field2");
        expect(field2.readFrom(node)).andReturn(Optional.of(Arrays.asList(value1, value2)));
        replayAll();

        FieldTypeUtils.readFieldValues(node, Arrays.asList(field1, field2), valueMap);

        assertFalse(valueMap.containsKey("field1"));
        assertThat(valueMap.get("field2").size(), equalTo(2));
        assertThat(valueMap.get("field2").get(0).getValue(), equalTo("one"));
        assertThat(valueMap.get("field2").get(1).getValue(), equalTo("two"));
        verifyAll();
    }

    @Test
    public void writeFieldValues() throws Exception {
        final StringFieldType field1 = createMock(StringFieldType.class);
        final StringFieldType field2 = createMock(StringFieldType.class);
        final Node node = createMock(Node.class);
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();
        final FieldValue value1 = new FieldValue("one");
        final FieldValue value2 = new FieldValue("two");
        valueMap.put("field2", Arrays.asList(value1, value2));

        expect(field1.hasUnsupportedValidator()).andReturn(false);
        expect(field1.getId()).andReturn("field1");
        field1.writeTo(node, Optional.empty());
        expectLastCall();
        expect(field2.hasUnsupportedValidator()).andReturn(false);
        expect(field2.getId()).andReturn("field2");
        field2.writeTo(node, Optional.of(Arrays.asList(value1, value2)));
        expectLastCall();
        replayAll();

        FieldTypeUtils.writeFieldValues(valueMap, Arrays.asList(field1, field2), node);

        verifyAll();
    }

    @Test
    public void writeFieldValuesWithUnknownValidator() throws Exception {
        final StringFieldType field1 = createMock(StringFieldType.class);
        final StringFieldType field2 = createMock(StringFieldType.class);
        final Node node = createMock(Node.class);
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();
        final FieldValue value1 = new FieldValue("one");
        final FieldValue value2 = new FieldValue("two");
        valueMap.put("field2", Arrays.asList(value1, value2));

        expect(field1.hasUnsupportedValidator()).andReturn(true);
        expect(field2.hasUnsupportedValidator()).andReturn(false);
        expect(field2.getId()).andReturn("field2");
        field2.writeTo(node, Optional.of(Arrays.asList(value1, value2)));
        expectLastCall();

        replayAll();

        FieldTypeUtils.writeFieldValues(valueMap, Arrays.asList(field1, field2), node);

        verifyAll();
    }

    @Test
    public void writeFieldValueWithEmptyFieldPath() throws ErrorWithPayloadException {
        final FieldPath emptyFieldPath = new FieldPath("");
        final List<FieldValue> fieldValues = Collections.emptyList();
        final List<FieldType> fields = Collections.emptyList();
        final Node node = createMock(Node.class);

        assertFalse(FieldTypeUtils.writeFieldValue(emptyFieldPath, fieldValues, fields, node));
    }

    @Test
    public void writeFieldValueReturnsAfterFirstSuccessfulWrite() throws ErrorWithPayloadException {
        final FieldPath fieldPath = new FieldPath("ns:field");
        final FieldValue value = new FieldValue("value");
        final List<FieldValue> fieldValues = Collections.singletonList(value);
        final FieldType field1 = createMock(FieldType.class);
        final FieldType field2 = createMock(FieldType.class);
        final FieldType field3 = createMock(FieldType.class);

        final List<FieldType> fields = Arrays.asList(field1, field2, field3);
        final Node node = createMock(Node.class);

        expect(field1.writeField(node, fieldPath, fieldValues)).andReturn(false);
        expect(field2.writeField(node, fieldPath, fieldValues)).andReturn(true);

        replayAll();

        assertTrue(FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, fields, node));

        verifyAll();
    }

    @Test
    public void writeFieldValueWhenAllWritesFail() throws ErrorWithPayloadException {
        final FieldPath fieldPath = new FieldPath("ns:field");
        final FieldValue value = new FieldValue("value");
        final List<FieldValue> fieldValues = Collections.singletonList(value);
        final FieldType field1 = createMock(FieldType.class);
        final FieldType field2 = createMock(FieldType.class);

        final List<FieldType> fields = Arrays.asList(field1, field2);
        final Node node = createMock(Node.class);

        expect(field1.writeField(node, fieldPath, fieldValues)).andReturn(false);
        expect(field2.writeField(node, fieldPath, fieldValues)).andReturn(false);

        replayAll();

        assertFalse(FieldTypeUtils.writeFieldValue(fieldPath, fieldValues, fields, node));

        verifyAll();
    }

    @Test
    public void validateFieldValuesNoFields() {
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();

        assertThat(FieldTypeUtils.validateFieldValues(valueMap, Collections.emptyList()), equalTo(0));
    }

    @Test
    public void validateFieldValuesTwoValid() {
        final StringFieldType field1 = createMock(StringFieldType.class);
        final StringFieldType field2 = createMock(StringFieldType.class);
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();
        final List<FieldValue> validValueList = Collections.singletonList(new FieldValue("valid"));
        valueMap.put("field2", validValueList);

        expect(field1.getId()).andReturn("field1");
        expect(field2.getId()).andReturn("field2");
        expect(field2.validate(validValueList)).andReturn(0);
        replayAll();

        assertThat(FieldTypeUtils.validateFieldValues(valueMap, Arrays.asList(field1, field2)), equalTo(0));
        verifyAll();
    }

    @Test
    public void validateFieldValuesFirstInvalid() {
        final StringFieldType field1 = createMock(StringFieldType.class);
        final StringFieldType field2 = createMock(StringFieldType.class);
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();
        final List<FieldValue> invalidValueList = Collections.singletonList(new FieldValue("invalid"));
        final List<FieldValue> validValueList = Collections.singletonList(new FieldValue("valid"));
        valueMap.put("field1", invalidValueList);
        valueMap.put("field2", validValueList);

        expect(field1.getId()).andReturn("field1");
        expect(field1.validate(invalidValueList)).andReturn(1);
        expect(field2.getId()).andReturn("field2");
        expect(field2.validate(validValueList)).andReturn(0);
        replayAll();

        assertThat(FieldTypeUtils.validateFieldValues(valueMap, Arrays.asList(field1, field2)), equalTo(1));
        verifyAll();
    }

    @Test
    public void validateFieldValuesSecondInvalid() {
        final StringFieldType field1 = createMock(StringFieldType.class);
        final StringFieldType field2 = createMock(StringFieldType.class);
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();
        final List<FieldValue> invalidValueList = Collections.singletonList(new FieldValue("invalid"));
        final List<FieldValue> validValueList = Collections.singletonList(new FieldValue("valid"));
        valueMap.put("field1", validValueList);
        valueMap.put("field2", invalidValueList);

        expect(field1.getId()).andReturn("field1");
        expect(field1.validate(validValueList)).andReturn(0);
        expect(field2.getId()).andReturn("field2");
        expect(field2.validate(invalidValueList)).andReturn(1);
        replayAll();

        assertThat(FieldTypeUtils.validateFieldValues(valueMap, Arrays.asList(field1, field2)), equalTo(1));
        verifyAll();
    }

    @Test
    public void validateFieldValuesBothInvalid() {
        final StringFieldType field1 = createMock(StringFieldType.class);
        final StringFieldType field2 = createMock(StringFieldType.class);
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();
        final List<FieldValue> invalidValueList = Collections.singletonList(new FieldValue("invalid"));
        valueMap.put("field1", invalidValueList);
        valueMap.put("field2", invalidValueList);

        expect(field1.getId()).andReturn("field1");
        expect(field1.validate(invalidValueList)).andReturn(1);
        expect(field2.getId()).andReturn("field2");
        expect(field2.validate(invalidValueList)).andReturn(1);
        replayAll();

        assertThat(FieldTypeUtils.validateFieldValues(valueMap, Arrays.asList(field1, field2)), equalTo(2));
        verifyAll();
    }

    @Test
    public void pluginsCheckBasic() {
        final FieldsInformation fieldsInformation = FieldsInformation.allSupported();
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final Node rootNode = createMock(Node.class);

        expect(context.getContentTypeRoot()).andReturn(rootNode);
        expect(NamespaceUtils.getEditorFieldConfigNodes(rootNode)).andReturn(Collections.emptyList());

        replayAll();

        FieldTypeUtils.checkPluginsWithoutFieldDefinition(fieldsInformation, context);
        assertTrue(fieldsInformation.isAllFieldsIncluded());

        verifyAll();
    }

    @Test
    public void pluginsCheckWithFieldTemplate() throws RepositoryException {
        final FieldsInformation fieldsInformation = FieldsInformation.allSupported();
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final Node rootNode = createMock(Node.class);
        final Node editorConfigNode = createMock(Node.class);
        final List<Node> nodeList = Collections.singletonList(editorConfigNode);
        final Property property = createMock(Property.class);

        expect(context.getContentTypeRoot()).andReturn(rootNode);
        expect(NamespaceUtils.getEditorFieldConfigNodes(rootNode)).andReturn(nodeList);
        expect(editorConfigNode.hasProperty("field")).andReturn(true);
        expect(editorConfigNode.getProperty("field")).andReturn(property);
        expect(property.getString()).andReturn("abc");

        replayAll();

        FieldTypeUtils.checkPluginsWithoutFieldDefinition(fieldsInformation, context);
        assertTrue(fieldsInformation.isAllFieldsIncluded());

        verifyAll();
    }

    @Test
    public void pluginsCheckIgnoresStructurePlugins() throws RepositoryException {
        final FieldsInformation fieldsInformation = FieldsInformation.allSupported();
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final Node rootNode = createMock(Node.class);
        final Node editorRootNode = createMock(Node.class);
        final Node editorColumnNode = createMock(Node.class);
        final List<Node> nodeList = Arrays.asList(editorRootNode, editorColumnNode);
        final Property property = createMock(Property.class);

        expect(context.getContentTypeRoot()).andReturn(rootNode);
        expect(NamespaceUtils.getEditorFieldConfigNodes(rootNode)).andReturn(nodeList);
        expect(editorRootNode.hasProperty("field")).andReturn(false);
        expect(editorRootNode.hasProperty("plugin.class")).andReturn(true);
        expect(editorRootNode.getProperty("plugin.class")).andReturn(property);
        expect(property.getString()).andReturn("org.hippoecm.frontend.editor.layout.");
        expect(editorColumnNode.hasProperty("field")).andReturn(false);
        expect(editorColumnNode.hasProperty("plugin.class")).andReturn(true);
        expect(editorColumnNode.getProperty("plugin.class")).andReturn(property);
        expect(property.getString()).andReturn("org.hippoecm.frontend.service.render.ListViewPlugin");

        replayAll();

        FieldTypeUtils.checkPluginsWithoutFieldDefinition(fieldsInformation, context);
        assertTrue(fieldsInformation.isAllFieldsIncluded());

        verifyAll();
    }

    @Test
    public void pluginsCheckWithoutFieldDefinitionIsReported() throws RepositoryException {
        final FieldsInformation fieldsInformation = FieldsInformation.allSupported();
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final Node rootNode = createMock(Node.class);
        final Node editorConfigNode = createMock(Node.class);
        final List<Node> nodeList = Collections.singletonList(editorConfigNode);
        final Property property = createMock(Property.class);

        expect(context.getContentTypeRoot()).andReturn(rootNode);
        expect(NamespaceUtils.getEditorFieldConfigNodes(rootNode)).andReturn(nodeList);
        expect(editorConfigNode.hasProperty("field")).andReturn(false);
        expect(editorConfigNode.hasProperty("plugin.class")).andReturn(true);
        expect(editorConfigNode.getProperty("plugin.class")).andReturn(property);
        expect(property.getString()).andReturn("org.hippoecm.test");

        replayAll();

        FieldTypeUtils.checkPluginsWithoutFieldDefinition(fieldsInformation, context);

        assertFalse(fieldsInformation.isAllFieldsIncluded());
        assertThat(fieldsInformation.getUnsupportedFieldTypes().size(), equalTo(1));
        assertThat(fieldsInformation.getUnsupportedFieldTypes().iterator().next(), equalTo("org.hippoecm.test"));

        verifyAll();

    }
}
