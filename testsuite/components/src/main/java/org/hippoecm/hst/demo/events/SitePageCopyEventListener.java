/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.events;

import org.hippoecm.hst.configuration.channel.ChannelManagerEventListenerRegistry;
import org.hippoecm.hst.pagecomposer.jaxrs.api.BeforeChannelDeleteEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PageCopyEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.ProxiedServiceHolder;
import org.onehippo.cms7.services.ProxiedServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

public class SitePageCopyEventListener {

    private static Logger log = LoggerFactory.getLogger(SitePageCopyEventListener.class);

    private ProxiedServiceTracker<ChannelManagerEventListenerRegistry> cmEventListenerRegistryTracker;

    public void init() {
        cmEventListenerRegistryTracker = new ProxiedServiceTracker<ChannelManagerEventListenerRegistry>() {
            @Override
            public void serviceRegistered(final ProxiedServiceHolder<ChannelManagerEventListenerRegistry> holder) {
                holder.getServiceProxy().registerChannelEventListener(SitePageCopyEventListener.this);
            }

            @Override
            public void serviceUnregistered(final ProxiedServiceHolder<ChannelManagerEventListenerRegistry> holder) {
                holder.getServiceProxy().unregisterChannelEventListener(SitePageCopyEventListener.this);
            }
        };

        HippoServiceRegistry.addTracker(cmEventListenerRegistryTracker, ChannelManagerEventListenerRegistry.class);
    }

    public void destroy() {
        HippoServiceRegistry.removeTracker(cmEventListenerRegistryTracker, ChannelManagerEventListenerRegistry.class);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onPageCopyEvent(PageCopyEvent event) {
        log.info("SitePageCopyEventListener handling PageCopyEvent: {}", event);

        if (event.getException() != null) {
            return;
        }
    }

    @Subscribe
    public void onBeforeChannelDeleteEvent(BeforeChannelDeleteEvent event) {
        log.info("SitePageCopyEventListener handling BeforeChannelDeleteEvent on mounts: {}. {}", event.getMounts(),
                event);
    }

    @Subscribe
    public void onChannelEvent(ChannelEvent event) {
        if (event.getChannelEventType() != null) {
            log.info("SitePageCopyEventListener handling ChannelEvent. {}", event);
        }
    }

}
