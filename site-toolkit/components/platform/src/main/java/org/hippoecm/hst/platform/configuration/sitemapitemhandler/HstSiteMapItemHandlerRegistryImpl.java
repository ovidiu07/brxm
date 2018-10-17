/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.sitemapitemhandler;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;
// note we do not need to account for concurrency since this is all controlled by the virtualhosts building
public class HstSiteMapItemHandlerRegistryImpl {

    private final Map<String, WeakReference<HstSiteMapItemHandler>> siteMapItemHandlersMap = new HashMap<>();

    public HstSiteMapItemHandler getSiteMapItemHandler(final String handlerId) {

        final WeakReference<HstSiteMapItemHandler> weakRef = siteMapItemHandlersMap.get(handlerId);
        if (weakRef == null) {
            return null;
        }
        return weakRef.get();
    }

    public void registerSiteMapItemHandler(String handlerId, HstSiteMapItemHandler hstSiteMapItemHandler) {
        siteMapItemHandlersMap.put(handlerId, new WeakReference<>(hstSiteMapItemHandler));
    }

    public void expungeStaleEntries() {
        for (Map.Entry<String, WeakReference<HstSiteMapItemHandler>> entry : siteMapItemHandlersMap.entrySet()) {
            if (entry.getValue().get() == null) {
                // entry has been GC-ed
                siteMapItemHandlersMap.remove(entry.getKey());
            }
        }
    }
}
