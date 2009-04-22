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
package org.hippoecm.addon.workflow;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;

class MenuHierarchy {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private Map<String, MenuHierarchy> submenus = new LinkedHashMap<String, MenuHierarchy>();
    private List<ActionDescription> items = new LinkedList<ActionDescription>();

    MenuHierarchy() {
    }

    public void put(String[] classifiers, ActionDescription action) {
        if (!submenus.containsKey(classifiers[0])) {
            submenus.put(classifiers[0], new MenuHierarchy());
        }
        submenus.get(classifiers[0]).put(action);
    }

    private void put(ActionDescription action) {
        items.add(action);
    }

    public void restructure() {
        Map<String, MenuHierarchy> submenus = this.submenus;
        this.submenus = new LinkedHashMap<String, MenuHierarchy>();
        this.items = new LinkedList<ActionDescription>();
        if(submenus.containsKey("default")) {
            MenuHierarchy submenu = submenus.get("default");
            for(ActionDescription action : submenu.items) {
                if(!action.isVisible()) {
                    continue;
                }
                if(action.getId().equals("info")) {
                    put(action);
                } else if(action.getId().equals("edit")) {
                    put(action);
                } else if(action.getId().equals("delete")) {
                    put(new String[] { "document" }, action);
                } else if(action.getId().equals("copy")) {
                    put(new String[] { "document" }, action);
                } else if(action.getId().equals("move")) {
                    put(new String[] { "document" }, action);
                } else if(action.getId().equals("rename")) {
                    put(new String[] { "document" }, action);
                } else if(action.getId().toLowerCase().contains("publi")) {
                    put(new String[] { "publication" }, action);
                } else {
                    put(new String[] { "miscelleneous" }, action);
                }
            }
        }
        if(submenus.containsKey("editing")) {
            MenuHierarchy submenu = submenus.get("editing");
            for(ActionDescription action : submenu.items) {
                put(action);
            }
        }
        if(submenus.containsKey("threepane")) {
            MenuHierarchy submenu = submenus.get("editing");
            for(ActionDescription action : submenu.items) {
                put(action);
            }
        }
    }

    public void flatten() {
        Map<String, MenuHierarchy> submenus = this.submenus;
        List<ActionDescription> items = this.items;
        this.submenus = new LinkedHashMap<String, MenuHierarchy>();
        this.items = new LinkedList<ActionDescription>();
        for(MenuHierarchy submenu : submenus.values()) {
            for(ActionDescription action : submenu.items) {
                if(action.isVisible()) {
                    put(action);
                }
            }
        }
    }

    List<Component> list(MenuComponent context) {
        List<Component> list = new LinkedList<Component>();
        if (context instanceof MenuBar) {
            for (ActionDescription item : items) {
                list.add(new MenuAction("item", item));
            }
             for (Map.Entry<String, MenuHierarchy> submenu : submenus.entrySet()) {
                list.add(new MenuButton("item", submenu.getKey(), submenu.getValue()));
            }
       } else {
            for (ActionDescription item : items) {
                list.add(new MenuItem("item", item));
            }
        }
        return list;
    }
}
