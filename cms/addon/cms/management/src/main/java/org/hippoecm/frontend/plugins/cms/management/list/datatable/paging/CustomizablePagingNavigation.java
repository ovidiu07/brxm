/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.cms.management.list.datatable.paging;

import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigation;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigationLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.navigation.paging.IPagingLabelProvider;

/**
 * @deprecated use org.hippoecm.frontend.plugins.standards.sa.* instead
 */
@Deprecated
public class CustomizablePagingNavigation extends AjaxPagingNavigation{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param id
     *            See Component
     * @param pageable
     *            The underlying pageable component to navigate
     * @param labelProvider
     *            The label provider for the text that the links should be displaying.
     */
    public CustomizablePagingNavigation(final String id, final IPageable pageable, final IPagingLabelProvider labelProvider)
    {
        super(id, pageable, labelProvider);
    }

    /**
     * Factory method for creating ajaxian page number links.
     *
     * @param id
     *            link id
     * @param pageable
     *            the pageable
     * @param pageIndex
     *            the index the link points to
     * @return the ajaxified page number link.
     */
    protected Link newPagingNavigationLink(String id, IPageable pageable, int pageIndex)
    {
        return new AjaxPagingNavigationLink(id, pageable, pageIndex);
    }
}
