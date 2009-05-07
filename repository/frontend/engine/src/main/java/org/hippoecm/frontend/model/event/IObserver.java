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
package org.hippoecm.frontend.model.event;

import java.util.EventListener;
import java.util.Iterator;

import org.apache.wicket.IClusterable;

/**
 * This interface defines the contract for a service that update its internal state in
 * response to changes in an observable object (IObservable).
 */
public interface IObserver extends EventListener, IClusterable {
    final static String SVN_ID = "$Id$";
    
    IObservable getObservable();
    
    void onEvent(Iterator<? extends IEvent> event);

}
