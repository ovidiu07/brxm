/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.service;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.onehippo.cms7.essentials.dashboard.model.TargetPom;

/**
 * ProjectService provides access to the project, and in particular to the file resources of a project.
 *
 * It can be @Inject-ed into an Essentials plugin's REST resource or custom {@code Instruction}.
 */
public interface ProjectService {
    String GROUP_ID_COMMUNITY = "org.onehippo.cms7";
    String GROUP_ID_ENTERPRISE = "com.onehippo.cms7";

    Path getBasePathForModule(TargetPom module);

    Path getPomPathForModule(TargetPom module);

    Path getJavaRootPathForModule(TargetPom module);

    Path getResourcesRootPathForModule(TargetPom module);

    Path getWebApplicationRootPathForModule(TargetPom module);

    Path getWebInfPathForModule(TargetPom module);

    Path getBeansRootPath();

    Path getBeansPackagePath();

    Path getRestPackagePath();

    Path getComponentsPackagePath();

    Path getContextXmlPath();

    Path getAssemblyFolderPath();

    /**
     * Retrieve a list of the log4j2 files of the project.
     */
    List<File> getLog4j2Files();
}
