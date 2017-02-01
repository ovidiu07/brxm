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
package org.onehippo.cm.impl.model.builder;

import java.util.Collection;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.onehippo.cm.impl.model.builder.sorting.DefinitionSorter;
import org.onehippo.cm.impl.model.builder.sorting.Sorter;

/**
 * Class that is capable of transforming a {@link Configuration} into its composite {@link ConfigurationNode} model
 * representation
 */
public class ModelBuilder {

    private DependencyVerifier dependencyVerifier = new DependencyVerifier();
    private ConfigurationTreeBuilder configurationTreeBuilder = new ConfigurationTreeBuilder();
    private DefinitionSorter definitionSorter = new DefinitionSorter();

    public MergedModel build(final Collection<ConfigurationImpl> configurations) {
//        dependencyVerifier.verifyConfigurationDependencies(configurations);

        final MergedModel mergedModel =  new MergedModel();
        new Sorter<ConfigurationImpl>()
                .sort(configurations)
                .forEach(configuration -> augmentModelWithConfiguration(mergedModel, configuration));
        return mergedModel;
    }

    private void augmentModelWithConfiguration(final MergedModel mergedModel, final ConfigurationImpl configuration) {
        new Sorter<ProjectImpl>()
                .sort(configuration.getModifiableProjects())
                .forEach(project -> augmentModelWithProject(mergedModel, project));
    }

    private void augmentModelWithProject(final MergedModel mergedModel, final ProjectImpl project) {
        new Sorter<ModuleImpl>()
                .sort(project.getModifiableModules())
                .forEach(module -> augmentModelWithModule(mergedModel, module));
    }

    private void augmentModelWithModule(final MergedModel mergedModel, final ModuleImpl module) {
        definitionSorter.sort(module, mergedModel);
        augmentModelWithContentDefinitions(mergedModel, module);
    }

    private void augmentModelWithContentDefinitions(final MergedModel mergedModel, final ModuleImpl module) {
//        module.getSortedContentDefinitions()
//                .forEach(definition -> configurationTreeBuilder.addDefinition(definition, mergedModel.getConfigurationRootNode()));
    }

    /**
     * Setters for unit testing purposes (dependency injection):
     */

    void setDependencyVerifier(final DependencyVerifier dependencyVerifier) {
        this.dependencyVerifier = dependencyVerifier;
    }

    void setConfigurationTreeBuilder(final ConfigurationTreeBuilder configurationTreeBuilder) {
        this.configurationTreeBuilder = configurationTreeBuilder;
    }

    void setDefinitionSorter(final DefinitionSorter definitionSorter) {
        this.definitionSorter = definitionSorter;
    }
}
