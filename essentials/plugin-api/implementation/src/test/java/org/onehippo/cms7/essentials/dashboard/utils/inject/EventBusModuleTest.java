/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils.inject;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class EventBusModuleTest {

    private static Logger log = LoggerFactory.getLogger(EventBusModuleTest.class);

    @Test
    public void testConfigure() throws Exception {
        final Injector injector = Guice.createInjector(new EventBusModule());
        final TestEventsListener testEventListener = new TestEventsListener();
        injector.injectMembers(testEventListener);
        final TestEventsApplication application = testEventListener.getApplication();
        for (int i = 0; i < 100; i++) {
            application.postEvent();
        }
        assertEquals(100, application.getCounter());

    }


}
