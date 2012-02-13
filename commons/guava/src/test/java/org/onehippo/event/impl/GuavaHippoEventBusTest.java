/*
 *  Copyright 2012 Hippo.
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
package org.onehippo.event.impl;

import com.google.common.eventbus.Subscribe;

import org.junit.Test;
import org.onehippo.event.HippoEventBus;

import static junit.framework.Assert.assertTrue;

public class GuavaHippoEventBusTest {

    public static class Listener {
        boolean fired = false;

        @Subscribe
        public void eventFired(Object payload) {
            fired = true;
        }
    }

    @Test
    public void testEventBus() {
        Listener listener = new Listener();
        HippoEventBus.register(listener);
        HippoEventBus.post(new Object());
        assertTrue(listener.fired);
    }

}
