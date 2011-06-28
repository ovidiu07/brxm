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
package org.hippoecm.hst.core.container;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResourceResponseImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstComponentInvokerImpl
 * 
 * @version $Id$
 */
public class HstComponentInvokerImpl implements HstComponentInvoker {
    
    private final static Logger log = LoggerFactory.getLogger(HstComponentInvokerImpl.class);

    protected boolean exceptionThrowable;
    
    protected String errorRenderPath;
    
    protected String dispatchUrlPrefix;
    
    public void setExceptionThrowable(boolean exceptionThrowable) {
        this.exceptionThrowable = exceptionThrowable;
    }
    
    public void setErrorRenderPath(String errorRenderPath) {
        this.errorRenderPath = errorRenderPath;
    }
    
    public void setDispatchUrlPrefix(String dispatchUrlPrefix) {
        if (dispatchUrlPrefix != null && !dispatchUrlPrefix.startsWith("/")) {
            log.info("The configured dispatchUrlPrefix '{}' does not start with a '/'. We prepend a '/' as the location should be a context relative path.");
            this.dispatchUrlPrefix = "/" + dispatchUrlPrefix;
        } else {
            this.dispatchUrlPrefix = dispatchUrlPrefix;
        }
    }
    
    public void invokeAction(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        HstComponent component = window.getComponent();
        
        if (component != null) {
            ClassLoader currentClassloader = switchToContainerClassloader(requestContainerConfig);

            try {
                if(log.isDebugEnabled()) {
                    log.debug("invoking action of component: {}", component.getClass().getName());
                }
                
                component.doAction(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                if (this.exceptionThrowable) {
                    throw e;
                }
                
                window.addComponentExcpetion(e);
                
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": " + e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": {}", e.toString());
                }
            } catch (Exception e) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(e);
                }
                
                window.addComponentExcpetion(new HstComponentException(e));
                
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: " + e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.toString());
                }
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }                
            }
        } else {
            window.addComponentExcpetion(new HstComponentException("The component is not available."));
        }
    }

    public void invokeBeforeRender(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        HstComponent component = window.getComponent();
        
        if (component != null) {
            ClassLoader currentClassloader = switchToContainerClassloader(requestContainerConfig);

            try {
                if(log.isDebugEnabled()) {
                    log.debug("invoking doBeforeRender of component: {}", component.getClass().getName());
                }
                
                component.doBeforeRender(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                if (this.exceptionThrowable) {
                    throw e;
                }
                
                window.addComponentExcpetion(e);
                
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: " + e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.toString());
                }
            } catch (Exception e) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(e);
                }
                
                window.addComponentExcpetion(new HstComponentException(e));
                
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": " + e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": {}", e.toString());
                }
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }                
            }
        } else {
            window.addComponentExcpetion(new HstComponentException("The component is not available."));
        }
    }

    public void invokeRender(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        HstComponent component = window.getComponent();
        boolean namedDispatching = false;
        String dispatchUrl = ((HstResponseImpl) hstResponse).getRenderPath(); 
        
        if (StringUtils.isBlank(dispatchUrl)) {
            dispatchUrl = window.getRenderPath();
        }
        
        if (dispatchUrl == null) {
            dispatchUrl = window.getNamedRenderer();
            namedDispatching = (dispatchUrl != null);
        }
        
        ServletRequest wrappedRequest = ((HstRequestImpl) hstRequest).getRequest();
        
        try {
            setHstObjectAttributesForServlet(wrappedRequest, hstRequest, hstResponse);
            
            // TODO : move the 'composer' logic to a better place
            /*
             * Below we check whether we need to output 'sample' content for the component instead of rendering the actual template. 
             * We only show sample content if and only if:
             * 1) There is already an HttpSession
             * 2) On this HttpSession there is a attribute called ContainerConstants.COMPOSERMODE_TEMPLATE_VIEW with value "true"
             * 3) The component is of type CONTAINER_ITEM_COMPONENT
             * 4) compInfo instanceof HstComponentConfigurationService
             * 5) mount is of type ContainerConstants.COMPOSERMODE && of type ContainerConstants.COMPOSERMODE_TEMPLATE_VIEW
             * 6) the container item component has sample content configured
             */
            
            HstComponentConfiguration compInfo = ((HstComponentConfiguration)window.getComponentInfo());
            Mount mount = hstRequest.getRequestContext().getResolvedMount().getMount();
            // do not create a http session!!
            HttpSession session = hstRequest.getSession(false);
            if (session != null 
                    && "true".equals(session.getAttribute(ContainerConstants.COMPOSERMODE_TEMPLATE_VIEW_ATTR_NAME))
                    && compInfo instanceof HstComponentConfigurationService
                    && compInfo.getComponentType() == Type.CONTAINER_ITEM_COMPONENT
                    && mount.isOfType(ContainerConstants.COMPOSERMODE)
                    && ((HstComponentConfigurationService) compInfo).getDummyContent() != null) {
               // We are in composermode && composer-template-view && the current component is a container item, and has sample content: hence, we do not render the actual template, but show sample content
               hstResponse.getWriter().append(((HstComponentConfigurationService)compInfo).getDummyContent());
               hstResponse.flushBuffer();
             /////// TODO END ////////////////////////////
            } else {
                invokeDispatcher(requestContainerConfig, servletRequest, servletResponse, namedDispatching, dispatchUrl, window);
            }
        } catch (HstComponentException e) {
            if (this.exceptionThrowable) {
                throw e;
            }
            
            window.addComponentExcpetion(e);
            
            if (log.isDebugEnabled()) {
                log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": " + e.toString(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": {}", e.toString());
            }
        } catch (Exception e) {
            if (this.exceptionThrowable) {
                throw new HstComponentException(e);
            }
            
            window.addComponentExcpetion(new HstComponentException(e));
            
            if (log.isDebugEnabled()) {
                log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": " + e.toString(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": {}", e.toString());
            }
        } finally {
            removeHstObjectAttributesForServlet(wrappedRequest, hstRequest, hstResponse);
        }
        
        if (window.hasComponentExceptions()) {
            renderErrorInformation(requestContainerConfig, servletRequest, servletResponse, window);
        }
    }

    public void invokeBeforeServeResource(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        HstComponent component = window.getComponent();
        
        if (component != null) {
            ClassLoader currentClassloader = switchToContainerClassloader(requestContainerConfig);

            try {
                component.doBeforeServeResource(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                if (this.exceptionThrowable) {
                    throw e;
                }
                
                window.addComponentExcpetion(e);
                
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": " + e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": {}", e.toString());
                }
            } catch (Exception e) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(e);
                }
                
                window.addComponentExcpetion(new HstComponentException(e));
                
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: " + e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.toString());
                }
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }                
            }
        } else {
            window.addComponentExcpetion(new HstComponentException("The component is not available."));
        }
    }

    public void invokeServeResource(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        HstComponent component = window.getComponent();
        boolean namedDispatching = false;
        String dispatchUrl = ((HstResourceResponseImpl) hstResponse).getServeResourcePath();
        
        if (StringUtils.isBlank(dispatchUrl)) {
            dispatchUrl = window.getServeResourcePath();
        }
        
        if (dispatchUrl == null) {
            dispatchUrl = window.getNamedResourceServer();
            namedDispatching = (dispatchUrl != null);
        }
        
        if (dispatchUrl == null) {
            dispatchUrl = window.getRenderPath();
        }
        
        if (dispatchUrl == null) {
            dispatchUrl = window.getNamedRenderer();
            namedDispatching = (dispatchUrl != null);
        }
        
        ServletRequest wrappedRequest = ((HstRequestImpl) hstRequest).getRequest();
        
        try {
            setHstObjectAttributesForServlet(wrappedRequest, hstRequest, hstResponse);
            
            invokeDispatcher(requestContainerConfig, servletRequest, servletResponse, namedDispatching, dispatchUrl, window);
            
        } catch (HstComponentException e) {
            if (this.exceptionThrowable) {
                throw e;
            }
            
            window.addComponentExcpetion(e);
            
            if (log.isDebugEnabled()) {
                log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": " + e.toString(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": {}", e.toString());
            }
        } catch (Exception e) {
            if (this.exceptionThrowable) {
                throw new HstComponentException(e);
            }
            
            window.addComponentExcpetion(new HstComponentException(e));
            
            if (log.isDebugEnabled()) {
                log.warn("Component exception caught: " + e.toString(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Component exception caught: {}", e.toString());
            }
        } finally {
            removeHstObjectAttributesForServlet(wrappedRequest, hstRequest, hstResponse);
        }

        if (window.hasComponentExceptions()) {
            renderErrorInformation(requestContainerConfig, servletRequest, servletResponse, window);
        }
    }

    protected void invokeDispatcher(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse, boolean namedDispatching, String dispatchUrl, HstComponentWindow window) throws Exception {
        RequestDispatcher disp = null;
        
        if (!StringUtils.isBlank(dispatchUrl)) {
            if (log.isDebugEnabled()) {
                log.debug("Invoking dispatcher of url: {}", dispatchUrl);
            }
            
            if (namedDispatching) {
                disp = requestContainerConfig.getServletContext().getNamedDispatcher(dispatchUrl);
            } else {
                if (dispatchUrl.startsWith("jcr:")) {
                    servletRequest.setAttribute(ContainerConstants.DISPATCH_URI_SCHEME, "jcr");
                    dispatchUrl = dispatchUrl.substring(4);
                } else if (dispatchUrl.startsWith("classpath:")) {
                    servletRequest.setAttribute(ContainerConstants.DISPATCH_URI_SCHEME, "classpath");
                    dispatchUrl = dispatchUrl.substring(10);
                    
                    if (!dispatchUrl.startsWith("/")) {
                        String resolvedDispatchUrl = "/" + window.getComponent().getClass().getPackage().getName().replace(".", "/") + "/" + dispatchUrl;
                        
                        if (log.isDebugEnabled()) {
                            log.debug("Relative classpath dispatch URL '{}' has been resolved to '{}'", dispatchUrl, resolvedDispatchUrl);
                        }
                        
                        dispatchUrl = resolvedDispatchUrl;
                    }
                } else {
                    if (!dispatchUrl.startsWith("/")) {
                        dispatchUrl = dispatchUrlPrefix + dispatchUrl;
                    }
                }
                
                disp = requestContainerConfig.getServletContext().getRequestDispatcher(dispatchUrl);
            }
        }
        
        if (disp == null) {
            if (log.isWarnEnabled()) {
                log.warn("The request dispatcher for dispatch url '{}' is null. Component name: '{}' . Component class: '"+window.getComponentName()+"'. Component id: '"+window.getComponentInfo().getId()+"'. If the component is inherited, the id might be a concatenation of some id's.", dispatchUrl, window.getName());
            }
            
            window.addComponentExcpetion(new HstComponentException("The dispatch url is null."));
        } else {
            ClassLoader currentClassloader = switchToContainerClassloader(requestContainerConfig);

            try {
                disp.include(servletRequest, servletResponse);
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }                
            }
        }
    }
    
    protected void renderErrorInformation(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse, HstComponentWindow window) {
        try {
            servletResponse.reset();
            
            if (errorRenderPath != null && errorRenderPath.length() != 0) {
                try {
                    servletRequest.setAttribute("errorComponentWindow", window);
                    invokeDispatcher(requestContainerConfig, servletRequest, servletResponse, false, errorRenderPath, window);
                    servletResponse.flushBuffer();
                } finally {
                    servletRequest.removeAttribute("errorComponentWindow");
                }
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to dispatch to error page: " + e);
            }
            
            try {
                servletResponse.reset();
            } catch (Exception ignore) {
            }
        }
    }
    
    private ClassLoader switchToContainerClassloader(HstContainerConfig requestContainerConfig) {
        ClassLoader containerClassloader = requestContainerConfig.getContextClassLoader();
        ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();
        
        if (containerClassloader != currentClassloader) {
            Thread.currentThread().setContextClassLoader(containerClassloader);
            return currentClassloader;
        } else {
            return null;
        }
    }
    
    private void setHstObjectAttributesForServlet(ServletRequest servletRequest, HstRequest hstRequest, HstResponse hstResponse) {
        // Needs to set hst request/response into attribute map
        // because hst request/response can be wrapped so it's not possible to use casting
        // in the servlet side such as tag library.
        servletRequest.setAttribute(ContainerConstants.HST_REQUEST, hstRequest);
        servletRequest.setAttribute(ContainerConstants.HST_RESPONSE, hstResponse);
    }
    
    private void removeHstObjectAttributesForServlet(ServletRequest servletRequest, HstRequest hstRequest, HstResponse hstResponse) {
        // Removes hst request/response into attribute map after dispatching
        servletRequest.removeAttribute(ContainerConstants.HST_REQUEST);
        servletRequest.removeAttribute(ContainerConstants.HST_RESPONSE);
    }
    
}
