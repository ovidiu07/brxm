/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.channel;

import java.util.function.BiPredicate;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyChannelFilter implements BiPredicate<Session, Channel> {

    Logger log = LoggerFactory.getLogger(DummyChannelFilter.class);

    @Override
    public boolean test(final Session session, final Channel channel) {
        try {
            log.info("DummyChannelFilter invoked and jcr session is : {}", session.getUserID());
            final HstRequestContext requestContext = RequestContextProvider.get();
            if (requestContext == null) {
                return true;
            }
            if (requestContext.getSession() != session) {
                throw new IllegalStateException("The session from the request context SHOULD be same as the session " +
                        "which is provided as an argument!");
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return true;
    }
}
