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
package org.onehippo.cm.backend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.jcr.Session;

import org.onehippo.cm.api.model.ConfigurationModel;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.action.ActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Comparator.comparing;

/**
 * Saves content definitions per source
 */
public class ContentService {

    private static final Logger log = LoggerFactory.getLogger(ContentService.class);

    private final Session session;
    private final ValueConverter valueConverter;

    public ContentService(final Session session) {
        this.session = session;
        this.valueConverter = new ValueConverter();

    }

    public void apply(final ConfigurationModel model)
            throws Exception {
        try {
            final Collection<ContentDefinition> contentDefinitions = model.getContentDefinitions();
            final Map<Source, List<ContentDefinition>> contentMap = contentDefinitions.stream()
                    .collect(Collectors.groupingBy(Definition::getSource, toSortedList(comparing(e -> e.getNode().getPath()))));
            for (final Source source : contentMap.keySet()) {
                final ContentProcessingService contentProcessingService = new JcrContentProcessingService(valueConverter);
                contentProcessingService.apply(contentMap.get(source).get(0).getNode(), ActionType.APPEND, session);

                session.save();
            }
        } catch (Exception e) {
            log.warn("Failed to apply configuration", e);
            throw e;
        }
    }

    private static <T> Collector<T,?,List<T>> toSortedList(Comparator<? super T> c) {
        return Collectors.collectingAndThen(Collectors.toCollection(ArrayList::new), l->{ l.sort(c); return l; } );
    }
}
