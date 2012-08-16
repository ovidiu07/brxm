/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.hst.demo.components;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Search extends AbstractSearchComponent {

    public static final Logger log = LoggerFactory.getLogger(Search.class);

    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        String query = request.getParameter("query");
        response.setRenderParameter("query", query);

        String pageSize = request.getParameter("pageSize");
        response.setRenderParameter("pageSize", pageSize);
    }

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        String query = getPublicRequestParameter(request, "query");
        String pageSizeString = getPublicRequestParameter(request, "pageSize");

        if (StringUtils.isBlank(query)) {
            return;
        }

        int pageSize;
        try {
            pageSize = Integer.parseInt(pageSizeString);

            if (pageSize <= 0) {
                pageSize = DEFAULT_PAGE_SIZE;
            }
        } catch (NumberFormatException e) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        doSearch(request, response, query, null, null, pageSize, getSiteContentBaseBean(request));
    }

}
