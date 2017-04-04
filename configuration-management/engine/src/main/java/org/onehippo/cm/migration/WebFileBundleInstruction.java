/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.migration;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.onehippo.cm.impl.model.SourceImpl;

public class WebFileBundleInstruction extends InitializeInstruction {

    public WebFileBundleInstruction(final EsvNode instructionNode, final Type type,
                                    final InitializeInstruction combinedWith) throws EsvParseException {
        super(instructionNode, type, combinedWith);
    }

    public void processWebFileBundle(final SourceImpl source, final File moduleRoot) throws IOException {
        final String bundleName = getResourcePath();
        log.info("Processing " + getType().getPropertyName() + " named '" + bundleName + "'");
        source.addWebFileBundleDefinition(getResourcePath());

        final File bundleDirectory = new File(moduleRoot, bundleName);
        if (bundleDirectory.exists()) {
            log.info("Deleting existing " + (bundleDirectory.isDirectory() ? "directory" : "file") + " '" + bundleDirectory.getAbsolutePath() + "'");
            try {
                FileUtils.forceDelete(bundleDirectory);
            } catch (IOException e) {
                log.error("Error deleting '{}'", bundleDirectory.getAbsolutePath(), e);
                throw e;
            }
        }

        try {
            FileUtils.copyDirectory(getResource(), bundleDirectory);
        } catch (IOException e) {
            log.error("Error copying web file bundle to directory '{}'", bundleDirectory.getAbsolutePath(), e);
            throw e;
        }
    }

}
