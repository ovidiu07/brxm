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
package org.hippoecm.hst.content.beans.support.jackson;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;

import com.fasterxml.jackson.databind.util.StdConverter;

public class HstLinkConverter extends StdConverter<HstLink, HstLinkRepresentation> {

    @Override
    public HstLinkRepresentation convert(HstLink value) {
        HstRequestContext requestContext = RequestContextProvider.get();

        HstLinkRepresentation representation = new HstLinkRepresentation();

        representation.setPath(value.getPath());
        representation.setSubPath(value.getSubPath());

        if (requestContext != null) {
            representation.setUrl(value.toUrlForm(requestContext, true));
        }

        return representation;
    }

}
