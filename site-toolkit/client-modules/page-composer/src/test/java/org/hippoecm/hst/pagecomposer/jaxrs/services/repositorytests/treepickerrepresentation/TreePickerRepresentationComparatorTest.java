/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.treepickerrepresentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.pagecomposer.jaxrs.model.TreePickerRepresentation;
import org.junit.Test;

import static org.hippoecm.hst.pagecomposer.jaxrs.model.TreePickerRepresentation.TreePickerRepresentationComparator;
import static org.junit.Assert.assertTrue;

public class TreePickerRepresentationComparatorTest {

    final static TreePickerRepresentationComparator comparator = new TreePickerRepresentationComparator();
    @Test
    public void sorting_first_folders_then_documents_alphabetically_case_insensitive() {

        TreePickerRepresentation folder1 = createTreePickerRepresentation("zzz", true);
        TreePickerRepresentation folder2 = createTreePickerRepresentation("ZZZa", true);
        TreePickerRepresentation doc1 = createTreePickerRepresentation("zzz", false);
        TreePickerRepresentation doc2= createTreePickerRepresentation("ZZZa", false);
        TreePickerRepresentation folder3 = createTreePickerRepresentation("aaa", true);
        TreePickerRepresentation doc3= createTreePickerRepresentation("aaa", false);

        final List<TreePickerRepresentation> presentations = new ArrayList();
        presentations.add(folder1);
        presentations.add(folder2);
        presentations.add(folder3);
        presentations.add(doc1);
        presentations.add(doc2);
        presentations.add(doc3);

        Collections.sort(presentations, comparator);

        // first folders sorted on display name and then documents
        assertTrue(presentations.get(0) == folder3);
        assertTrue(presentations.get(1) == folder1);
        assertTrue(presentations.get(2) == folder2);

        assertTrue(presentations.get(3) == doc3);
        assertTrue(presentations.get(4) == doc1);
        assertTrue(presentations.get(5) == doc2);

    }

    @Test(expected = NullPointerException.class)
    public void sorting_displayName_null_results_in_NPE() {
        TreePickerRepresentation folder1 = createTreePickerRepresentation(null, true);
        TreePickerRepresentation folder2 = createTreePickerRepresentation("ZZZ", true);
        final List<TreePickerRepresentation> presentations = new ArrayList();
        presentations.add(folder1);
        presentations.add(folder2);
        Collections.sort(presentations, comparator);
    }

    private TreePickerRepresentation createTreePickerRepresentation(final String displayName, final boolean folder) {
        TreePickerRepresentation presentation = new TreePickerRepresentation();
        presentation.setDisplayName(displayName);
        presentation.setFolder(folder);
        return presentation;
    }


}
