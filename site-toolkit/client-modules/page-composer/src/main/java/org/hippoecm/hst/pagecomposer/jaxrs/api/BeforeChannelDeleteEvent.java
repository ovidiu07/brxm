/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.api;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeforeChannelDeleteEvent extends RuntimeExceptionEvent {
    private static final Logger log = LoggerFactory.getLogger(BeforeChannelDeleteEvent.class);

    private transient final HstRequestContext requestContext;

    public BeforeChannelDeleteEvent(final Channel channel, final HstRequestContext requestContext) {
        super(channel);
        this.requestContext = requestContext;
    }

    public Channel getChannel() {
        return (Channel) getSource();
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    public HstRequestContext getRequestContext() {
        return requestContext;
    }

    @Override
    public String toString() {
        return "BeforeChannelDeleteEvent{" +
                ", channel=" + getChannel().toString() +
                ", exception=" + getException() +
                '}';
    }
}
