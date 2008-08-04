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
package org.hippoecm.repository.security;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.CredentialsCallback;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.hippoecm.repository.security.domain.Domain;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;
import org.hippoecm.repository.security.principals.GroupPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoLoginModule implements LoginModule {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    // initial state
    private Subject subject;
    private CallbackHandler callbackHandler;
    @SuppressWarnings("unused")
    private Map<String, ?> sharedState;

    @SuppressWarnings("unused")
    private Map<String, ?> options;

    // configurable JAAS options
    @SuppressWarnings("unused")
    private final boolean debug = false;

    private final boolean tryFirstPass = false;
    private final boolean useFirstPass = false;
    private final boolean storePass = false;
    private final boolean clearPass = false;

    // local authentication state:
    // the principals, i.e. the authenticated identities
    private final Set<Principal> principals = new HashSet<Principal>();

    // the rootSession
    private Session rootSession;

    private SecurityManager securityManager;

    /**
     * Get Logger
     */
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    //----------------------------------------------------------< LoginModule >
    /**
     * {@inheritDoc}
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {

        // set state
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        // fetch default JAAS parameters
        /* disable for now untill we have real jaas support
        debug = "true".equalsIgnoreCase((String) options.get("debug"));
        tryFirstPass = "true".equalsIgnoreCase((String) options.get("tryFirstPass"));
        useFirstPass = "true".equalsIgnoreCase((String) options.get("useFirstPass"));
        storePass = "true".equalsIgnoreCase((String) options.get("storePass"));
        clearPass = "true".equalsIgnoreCase((String) options.get("clearPass"));

        if (log.isDebugEnabled()) {
            log.debug("HippoLoginModule JAAS config:");
            log.debug("* tryFirstPass   : " + tryFirstPass);
            log.debug("* useFirstPass   : " + useFirstPass);
            log.debug("* storePass      : " + storePass);
            log.debug("* clearPass      : " + clearPass);
        }
        */
    }

    /**
     * {@inheritDoc}
     */
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("no CallbackHandler available");
        }

        try {
            // Get credentials using a JAAS callback
            CredentialsCallback ccb = new CredentialsCallback();
            callbackHandler.handle(new Callback[] { ccb });
            SimpleCredentials creds = (SimpleCredentials) ccb.getCredentials();

            // TODO: impersonate from rootSession -> systemSession
            rootSession = (Session) creds.getAttribute("rootSession");
            if (rootSession == null) {
                throw new LoginException("RootSession not set.");
            }


            // check for impersonation
            Object attr = creds.getAttribute(SecurityConstants.IMPERSONATOR_ATTRIBUTE);
            if (attr != null && attr instanceof Subject) {
                Subject impersonator = (Subject) attr;

                // anonymous cannot impersonate
                if (!impersonator.getPrincipals(AnonymousPrincipal.class).isEmpty()) {
                    log.info("Denied Anymous impersonating as {}", creds.getUserID());
                    return false;
                }
                
                // system session impersonate
                if (!impersonator.getPrincipals(SystemPrincipal.class).isEmpty()) {
                    log.debug("SystemSession impersonating to new SystemSession");
                    principals.add(new SystemPrincipal());
                    return true;
                }

                // check for valid user
                if (impersonator.getPrincipals(UserPrincipal.class).isEmpty()) {
                    log.info("Denied unknown user impersonating as {}", creds.getUserID());
                    return false;
                }

                Principal iup = impersonator.getPrincipals(UserPrincipal.class).iterator().next();
                String impersonarorId = iup.getName();
                // TODO: check somehow if the user is allowed to imporsonate

                log.info("Impersonating as {} by {}", creds.getUserID(), impersonarorId);

                // get securityManager
                securityManager = SecurityManager.getInstance();
                securityManager.init(rootSession);
                
                setUserPrincipals(creds.getUserID());
                setGroupPrincipals(creds.getUserID());
                setFacetAuthPrincipals(creds.getUserID());
                return !principals.isEmpty();
            }

            // get securityManager
            securityManager = SecurityManager.getInstance();
            securityManager.init(rootSession);

            // check for anonymous login
            if (creds == null || creds.getUserID() == null) {
                // authenticate as anonymous
                principals.add(new AnonymousPrincipal());
                log.info("Authenticated as anonymous.");
                setUserPrincipals(null);
                setGroupPrincipals(null);
                setFacetAuthPrincipals(null);
                return true;
            }

            log.debug("Trying to authenticate as {}", creds.getUserID());
            if (authenticate(creds.getUserID(), creds.getPassword())) {
                log.info("Authenticated as {}", creds.getUserID());
                setUserPrincipals(creds.getUserID());
                setGroupPrincipals(creds.getUserID());
                setFacetAuthPrincipals(creds.getUserID());
                return !principals.isEmpty();
            } else {
                log.info("NOT Authenticated as {}", creds.getUserID());
                // authentication failed: clean out state
                principals.clear();
                throw new FailedLoginException("Wrong username or password.");
            }
        } catch (ClassCastException e) {
            log.error("Error during login", e);
            throw new LoginException(e.getMessage());
        } catch (java.io.IOException e) {
            log.error("Error during login", e);
            throw new LoginException(e.getMessage());
        } catch (UnsupportedCallbackException e) {
            log.error("Error during login", e);
            throw new LoginException(e.getCallback().toString() + " not available");
        } catch (RepositoryException e) {
            log.error("Error during login", e);
            throw new LoginException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean commit() throws LoginException {
        if (principals.isEmpty()) {
            return false;
        } else {
            // add a principals (authenticated identities) to the Subject
            subject.getPrincipals().addAll(principals);
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean abort() throws LoginException {
        if (principals.isEmpty()) {
            return false;
        } else {
            logout();
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        return true;
    }

    /**
     * TODO: move to authentication manager?
     * Authenticate the user against the cache or the repository
     * @param session A privileged session which can read usernames and passwords
     * @param userId
     * @param password
     * @return true when authenticated
     */
    private boolean authenticate(String userId, char[] password) {
        // check for anonymous user
        if (userId == null && password == null) {
            principals.add(new AnonymousPrincipal());
            log.debug("Authenticated as Anonymous user.");
            return true;
        }

        // basic security check
        if (userId == null || "".equals(userId) || password == null || password.length == 0) {
            log.debug("Empty username or password not allowed.");
            return false;
        }

        return securityManager.authenticate(userId, password);
    }

    private void setUserPrincipals(String userId) {
        if (userId == null) {
            return;
        }
        UserPrincipal userPrincipal;
        userPrincipal = new UserPrincipal(userId);
        log.debug("Adding principal: {}", userPrincipal);
        principals.add(userPrincipal);
    }

    private void setGroupPrincipals(String userId) {
        Set<String> memberships = securityManager.getMemeberships(userId);
        for (String groupId : memberships) {
            GroupPrincipal groupPrincipal = new GroupPrincipal(groupId);
            principals.add(groupPrincipal);
            log.debug("Adding principal: {}", groupPrincipal);
        }
    }

    private void setFacetAuthPrincipals(String userId) {
        // Find domains that the user is associated with
        Set<Domain> userDomains = new HashSet<Domain>();
        userDomains.addAll(securityManager.getDomainsForUser(userId));
        for (Principal principal : principals) {
            if (principal instanceof GroupPrincipal) {
                userDomains.addAll(securityManager.getDomainsForGroup(principal.getName()));
            }
        }

        // Add facet auth principals
        for (Domain domain : userDomains) {

            // get roles for a user for a domain
            log.debug("User {} has domain {}", userId, domain.getName());
            Set<String> roles = new HashSet<String>();
            roles.addAll(domain.getRolesForUser(userId));
            for (Principal principal : principals) {
                if (principal instanceof GroupPrincipal) {
                    roles.addAll(domain.getRolesForGroup(principal.getName()));
                }
            }

            // merge permissions for the roles for a domain
            int perms = 0;
            for (String roleId : roles) {
                perms |= securityManager.getJCRPermissionsForRole(roleId);
            }
            log.info("User {} has perms {} for domain {} ", new Object[] { userId, perms, domain.getName() });

            // create and add facet auth principal
            FacetAuthPrincipal fap = new FacetAuthPrincipal(domain.getName(), domain.getDomainRules(), roles, perms);
            principals.add(fap);
        }
    }
}
