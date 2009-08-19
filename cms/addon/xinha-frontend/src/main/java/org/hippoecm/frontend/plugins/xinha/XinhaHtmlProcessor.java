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
package org.hippoecm.frontend.plugins.xinha;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XinhaHtmlProcessor {

    private static Pattern LINK_PATTERN = Pattern.compile("<(a|img)(\\s+)(.*?)(src|href)=\"(.*?)\"(.*?)>",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    private static Pattern IMG_PATTERN = Pattern.compile("<img[^>]+>", Pattern.CASE_INSENSITIVE);
    private static Pattern SRC_PATTERN = Pattern.compile("src=\"[^\"]+\"", Pattern.CASE_INSENSITIVE);

    public static String prefixImageLinks(String text, String prefix) {
        StringBuffer processed = new StringBuffer();
        Matcher m = IMG_PATTERN.matcher(text);
        while (m.find()) {
            String img = m.group();
            StringBuffer newImg = new StringBuffer();
            Matcher s = SRC_PATTERN.matcher(img);
            if (s.find()) {
                String src = s.group();
                // skip src=
                String link = src.substring(4);
                if (link.charAt(0) == '"') {
                    link = link.substring(1);
                }
                if (link.charAt(link.length() - 1) == '"') {
                    link = link.substring(0, link.length() - 1);
                }
                if (!link.startsWith("http:") && !link.startsWith("https:")) {
                    s.appendReplacement(newImg, ("src=\"" + prefix + link + "\"").replace("\\", "\\\\").replace("$",
                            "\\$"));
                } else {
                    s.appendReplacement(newImg, src.replace("\\", "\\\\").replace("$", "\\$"));
                }
            }
            s.appendTail(newImg);
            m.appendReplacement(processed, newImg.toString().replace("\\", "\\\\").replace("$", "\\$"));
        }
        m.appendTail(processed);

        return processed.toString();
    }

    public static Set<String> getInternalLinks(String text) {
        Set<String> links = new TreeSet<String>();
        Matcher m = LINK_PATTERN.matcher(text);
        while (m.find()) {
            String link = m.group(5);
            if (link.startsWith("http:") || link.startsWith("https:")) {
                continue;
            }
            if (link.indexOf('/') > 0) {
                links.add(link.substring(0, link.indexOf('/')));
            } else {
                links.add(link);
            }
        }
        return links;
    }

}
