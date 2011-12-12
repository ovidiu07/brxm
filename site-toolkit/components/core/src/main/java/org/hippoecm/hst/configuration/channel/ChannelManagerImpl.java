/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.configuration.channel;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.security.auth.Subject;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.security.HstSubject;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelManagerImpl implements MutableChannelManager {

    private static final String DEFAULT_HST_ROOT_PATH = "/hst:hst";
    private static final String DEFAULT_HST_SITES = "hst:sites";
    private static final String DEFAULT_CONTENT_ROOT = "/content/documents";

    static final Logger log = LoggerFactory.getLogger(ChannelManagerImpl.class.getName());

    private String rootPath = DEFAULT_HST_ROOT_PATH;
    private String hostGroup = null;
    private String sites = DEFAULT_HST_SITES;

    private Map<String, BlueprintService> blueprints;
    private Map<String, Channel> channels;
    private Credentials credentials;
    private Repository repository;
    private String channelsRoot = DEFAULT_HST_ROOT_PATH + "/" + HstNodeTypes.NODENAME_HST_CHANNELS + "/";
    private String contentRoot = DEFAULT_CONTENT_ROOT;

    /**
     * The codec which is used for the channel ID
     */
    private StringCodec channelIdCodec = new StringCodecFactory.UriEncoding();

    public ChannelManagerImpl() {
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath.trim();
        channelsRoot = rootPath + "/" + HstNodeTypes.NODENAME_HST_CHANNELS + "/";
    }

    public void setContentRoot(final String contentRoot) {
        this.contentRoot = contentRoot.trim();
    }

    private void loadBlueprints(final Node configNode) throws RepositoryException {
        if (configNode.hasNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS)) {
            Node blueprintsNode = configNode.getNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS);
            NodeIterator blueprintIterator = blueprintsNode.getNodes();
            while (blueprintIterator.hasNext()) {
                Node blueprint = blueprintIterator.nextNode();
                blueprints.put(blueprint.getName(), new BlueprintService(blueprint));
            }
        }
    }

    private void loadChannels(final Node configNode) throws RepositoryException {
        if (configNode.hasNode(HstNodeTypes.NODENAME_HST_CHANNELS)) {
            Node channelsFolder = configNode.getNode(HstNodeTypes.NODENAME_HST_CHANNELS);
            NodeIterator rootChannelNodes = channelsFolder.getNodes();
            while (rootChannelNodes.hasNext()) {
                Node hgNode = rootChannelNodes.nextNode();
                loadChannel(hgNode);
            }
        } else {
            log.warn("Cannot load channels because node '{}' does not exist", configNode.getPath() + "/" + HstNodeTypes.NODENAME_HST_CHANNELS);
        }
    }

    private void loadChannel(Node currNode) throws RepositoryException {
        Channel channel = ChannelPropertyMapper.readChannel(currNode);
        channels.put(channel.getId(), channel);
    }

    private void loadFromMount(MutableMount mount) {
        String channelPath = mount.getChannelPath();
        if (channelPath == null) {
            // mount does not have an associated channel
            log.debug("Ignoring mount '" + mount.getName() + "' since it does not have a channel path");
            return;
        }
        if (!channelPath.startsWith(channelsRoot)) {
            log.warn("Channel path '" + channelPath + "' is not part of the HST configuration under " + rootPath +
                    ", ignoring channel info for mount " + mount.getName() +
                    ".  Use the full repository path for identification.");
            return;
        }
        Channel channel = channels.get(channelPath.substring(channelsRoot.length()));
        if (channel == null) {
            log.warn("Unknown channel " + channelPath + ", ignoring mount " + mount.getName());
            return;
        }
        if (channel.getUrl() != null) {
            // We already encountered this channel while walking over all the mounts. This mount
            // therefore points to the same channel as another mount, which is not allowed (each channel has only
            // one mount)
            log.warn("Channel " + channelPath + " contains multiple mounts - analysing mount " + mount.getName() + ", found url " + channel.getUrl() + " in channel");
            return;
        }

        String mountPoint = mount.getMountPoint();
        if (mountPoint != null) {
            channel.setHstMountPoint(mountPoint);
            String configurationPath = mount.getHstSite().getConfigurationPath();
            if (configurationPath != null) {
                channel.setHstConfigPath(configurationPath);
            }

            channel.setContentRoot(mount.getCanonicalContentPath());
        }

        String mountPath = mount.getMountPath();

        channel.setLocale(mount.getLocale());
        channel.setMountId(mount.getIdentifier());
        channel.setMountPath(mountPath);

        VirtualHost virtualHost = mount.getVirtualHost();
        channel.setCmsPreviewPrefix(virtualHost.getVirtualHosts().getCmsPreviewPrefix());
        channel.setContextPath(mount.onlyForContextPath());
        channel.setHostname(virtualHost.getHostName());

        StringBuilder url = new StringBuilder();
        url.append(mount.getScheme());
        url.append("://");
        url.append(virtualHost.getHostName());
        if (mount.isPortInUrl()) {
            int port = mount.getPort();
            if (port != 0) {
                url.append(':');
                url.append(mount.getPort());
            }
        }
        if (virtualHost.isContextPathInUrl() && mount.onlyForContextPath() != null) {
            url.append(mount.onlyForContextPath());
        }
        if (StringUtils.isNotEmpty(mountPath)) {
            if (!mountPath.startsWith("/")) {
                url.append('/');
            }
            url.append(mountPath);
        }
        channel.setUrl(url.toString());
    }

    /**
     * Make sure that hst manager is initialized.
     *
     * @throws ChannelException
     */
    void load() throws ChannelException {
        if (channels == null) {
            HstManager manager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
            try {
                manager.getVirtualHosts();
            } catch (RepositoryNotAvailableException e) {
                throw new ChannelException("could not build channels");
            }
        }
    }

    public void load(VirtualHosts virtualHosts) throws RepositoryNotAvailableException {
        Session session = null;
        try {
            session = getSession(false);
            Node configNode = session.getNode(rootPath);
 
            blueprints = new HashMap<String, BlueprintService>();
            loadBlueprints(configNode);

            channels = new HashMap<String, Channel>();
            
            hostGroup = virtualHosts.getChannelManagerHostGroupName();
            sites = virtualHosts.getChannelManagerSitesName();
            
            if(hostGroup == null) {
                log.warn("Cannot load the Channel Manager because no host group configured on hst:hosts node");
            }
            // load all the channels, even if they are not used by the current hostGroup
            loadChannels(configNode);
            
            // in channel manager only the mounts for at most ONE single hostGroup are shown
            List<Mount> mounts = virtualHosts.getMountsByHostGroup(hostGroup);
            for (Mount mount : mounts) {
                if (mount instanceof MutableMount) {
                    loadFromMount((MutableMount) mount);
                }
            }
            
            // for ALL the mounts in ALL the host groups, set the channel Info if available
            for(String hostGroupName : virtualHosts.getHostGroupNames()) {
                for (Mount mount : virtualHosts.getMountsByHostGroup(hostGroupName)) {
                    if (mount instanceof MutableMount) {
                        try {
                            String channelPath = mount.getChannelPath();
                            if (StringUtils.isEmpty(channelPath)) {
                                log.debug("Mount '{}' does not have a channelpath configured. Skipping setting channelInfo. ", mount);
                                continue;
                            }
                            String channelNodeName = channelPath.substring(channelPath.lastIndexOf("/")+1);
                            Channel channel = this.channels.get(channelNodeName);
                            if (channel == null) {
                                log.debug("Mount '{}' has channelpath configured that does not point to a channel info. Skipping setting channelInfo. ", mount);
                                continue;
                            }
                            log.debug("Setting channel info for mount '{}'.", mount);
                            ((MutableMount)mount).setChannelInfo(getChannelInfo(channel));
                        } catch (ChannelException e) {
                            log.error("Could not set channel info to mount", e);
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryNotAvailableException("Could not load channels and/or blueprints", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    protected Session getSession(boolean writable) throws RepositoryException {
        Credentials credentials = this.credentials;
        if (writable) {
            Subject subject = HstSubject.getSubject(null);
            if (subject != null) {
                Set<Credentials> repoCredsSet = subject.getPrivateCredentials(Credentials.class);
                if (!repoCredsSet.isEmpty()) {
                    credentials = repoCredsSet.iterator().next();
                } else {
                    throw new LoginException("Repository credentials for the subject is not found.");
                }
            } else {
                throw new LoginException("No subject available to obtain writable session");
            }
        }

        javax.jcr.Session session;

        if (credentials == null) {
            session = this.repository.login();
        } else {
            session = this.repository.login(credentials);
        }

        // session can come from a pooled event based pool so always refresh before building configuration:
        session.refresh(false);

        return session;
    }

    public synchronized Channel getChannel(String jcrPath) throws ChannelException {
        load();
        if (jcrPath == null || !jcrPath.startsWith(channelsRoot)) {
            throw new ChannelException("Expected a JCR path starting with '" + channelsRoot + "', but got '" + jcrPath + "' instead");
        }
        final String channelId = jcrPath.substring(channelsRoot.length());
        return channels.get(channelId);
    }

    // PUBLIC interface; all synchronised to guarantee consistent state

    @Override
    public synchronized Map<String, Channel> getChannels() throws ChannelException {
        load();

        Map<String, Channel> result = new HashMap<String, Channel>();
        for (Map.Entry<String, Channel> entry : channels.entrySet()) {
            result.put(entry.getKey(), new Channel(entry.getValue()));
        }
        return result;
    }

    @Override
    public synchronized String persist(final String blueprintId, Channel channel) throws ChannelException {
       
        if (!blueprints.containsKey(blueprintId)) {
            throw new ChannelException("Blueprint id " + blueprintId + " is not valid");
        }
        BlueprintService bps = blueprints.get(blueprintId);

        Session session = null;
        try {
            session = getSession(true);
            Node configNode = session.getNode(rootPath);
            String channelId = createUniqueChannelId(channel.getName(), session);
            createChannel(configNode, bps, session, channelId, channel);

            channels = null;

            session.save();

            return channelId;
        } catch (RepositoryException e) {
            throw new ChannelException("Unable to save channel to the repository", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    protected String createUniqueChannelId(String channelName, Session session) throws ChannelException {
        if (StringUtils.isBlank(channelName)) {
            throw new ChannelException("Cannot create channel ID: channel name is blank");
        }
        try {
            String channelId = channelIdCodec.encode(channelName);
            int retries = 0;
            Node channelsNode = session.getNode(channelsRoot);
            
            while (channelsNode.hasNode(channelId)) {
                retries += 1;
                StringBuilder builder = new StringBuilder(channelName);
                builder.append('-');
                builder.append(retries);
                channelId = channelIdCodec.encode(builder.toString());
            }

            return channelId;
        } catch (RepositoryException e) {
            throw new ChannelException("Cannot create channel ID for channelName '" + channelName + "'", e);
        }
    }

    @Override
    public synchronized void save(final Channel channel) throws ChannelException {
        load();
        if (!channels.containsKey(channel.getId())) {
            throw new ChannelException("No channel with id " + channel.getId() + " was found");
        }
        Session session = null;
        try {
            session = getSession(true);
            Node configNode = session.getNode(rootPath);
            updateChannel(configNode, channel);

            channels = null;

            session.save();
        } catch (RepositoryException e) {
            throw new ChannelException("Unable to save channel to the repository", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    @Override
    public synchronized List<Blueprint> getBlueprints() throws ChannelException {
        load();
        return new ArrayList<Blueprint>(blueprints.values());
    }

    @Override
    public synchronized Blueprint getBlueprint(final String id) throws ChannelException {
        load();
        if (!blueprints.containsKey(id)) {
            throw new ChannelException("Blueprint " + id + " does not exist");
        }
        return blueprints.get(id);
    }

    @Override
    public Class<? extends ChannelInfo> getChannelInfoClass(Channel channel) throws ChannelException {
        String channelInfoClassName = channel.getChannelInfoClassName();
        if (channelInfoClassName == null) {
            log.debug("No channelInfoClassName defined. Return just the ChannelInfo interface class");
            return ChannelInfo.class;
        }
        try {
            return (Class<? extends ChannelInfo>) ChannelPropertyMapper.class.getClassLoader().loadClass(channelInfoClassName);
        } catch (ClassNotFoundException cnfe) {
            throw new ChannelException("Configured class " + channelInfoClassName + " was not found", cnfe);
        } catch (ClassCastException cce) {
            throw new ChannelException("Configured class " + channelInfoClassName + " does not extend ChannelInfo", cce);
        }
    }

    @Override
    public <T extends ChannelInfo> T getChannelInfo(Channel channel) throws ChannelException {
        Class<? extends ChannelInfo> channelInfoClass = getChannelInfoClass(channel);
        return (T) ChannelUtils.getChannelInfo(channel.getProperties(), channelInfoClass);
    }

    @Override
    public List<HstPropertyDefinition> getPropertyDefinitions(Channel channel) {
        try {
            if (channel.getChannelInfoClassName() != null) {
                Class<? extends ChannelInfo> channelInfoClass = getChannelInfoClass(channel);
                if (channelInfoClass != null) {
                    return ChannelInfoClassProcessor.getProperties(channelInfoClass);
                }
            }
        } catch (ChannelException ex) {
            log.warn("Could not load properties", ex);
        }
        return Collections.emptyList();
    }

    @Override
    public ResourceBundle getResourceBundle(Channel channel, Locale locale) {
        String channelInfoClassName = channel.getChannelInfoClassName();
        if (channelInfoClassName != null) {
            return ResourceBundle.getBundle(channelInfoClassName, locale);
        }
        return null;
    }

    @Override
    public synchronized boolean canUserModifyChannels() {
        Session session = null;
        try {
            session = getSession(true);
            return session.hasPermission(rootPath + "/" + HstNodeTypes.NODENAME_HST_CHANNELS + "/accesstest", Session.ACTION_ADD_NODE);
        } catch (RepositoryException e) {
            log.error("Repository error when determining channel manager access", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
        return false;
    }

    public synchronized void invalidate() {
        channels = null;
        blueprints = null;
    }

    // private - internal - methods

    private void createChannel(Node configRoot, BlueprintService bps, Session session, final String channelId, final Channel channel) throws ChannelException, RepositoryException {
        Node blueprintNode = bps.getNode(session);

        URI channelUri = getChannelUri(channel);
        Node virtualHost = getOrCreateVirtualHost(configRoot, channelUri.getHost());

        if (!configRoot.hasNode(HstNodeTypes.NODENAME_HST_CHANNELS)) {
            configRoot.addNode(HstNodeTypes.NODENAME_HST_CHANNELS, HstNodeTypes.NODETYPE_HST_CHANNELS);
        }
        Node channelNode = configRoot.getNode(HstNodeTypes.NODENAME_HST_CHANNELS).addNode(channelId, HstNodeTypes.NODETYPE_HST_CHANNEL);
        ChannelPropertyMapper.saveChannel(channelNode, channel);

        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_CONFIGURATION)) {
            copyNodes(blueprintNode.getNode(HstNodeTypes.NODENAME_HST_CONFIGURATION), configRoot.getNode(HstNodeTypes.NODENAME_HST_CONFIGURATIONS), channelId);
        }

        Session jcrSession = configRoot.getSession();
        String mountPointPath = null;
        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_SITE)) {
            Node siteNode = copyNodes(blueprintNode.getNode(HstNodeTypes.NODENAME_HST_SITE), configRoot.getNode(sites), channelId);
            mountPointPath = siteNode.getPath();
            channel.setHstMountPoint(siteNode.getPath());

            if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_CONFIGURATION)) {
                siteNode.setProperty(HstNodeTypes.SITE_CONFIGURATIONPATH, configRoot.getNode(HstNodeTypes.NODENAME_HST_CONFIGURATIONS).getPath() + "/" + channelId);
            } else {
                // reuse the configuration path specified in the hst:site node, if it exists
                String configurationPath = siteNode.getProperty(HstNodeTypes.SITE_CONFIGURATIONPATH).getString();
                if (!jcrSession.nodeExists(configurationPath)) {
                    throw new ChannelException("The hst:site node in blueprint '" + blueprintNode.getPath()
                            + "' does not have a custom HST configuration in a child node 'hst:configuration' and property '" + HstNodeTypes.SITE_CONFIGURATIONPATH + "' points to a non-existing node");
                }
            }
            channel.setHstConfigPath(siteNode.getProperty(HstNodeTypes.SITE_CONFIGURATIONPATH).getString());
            Node previewSiteNode = copyNodes(siteNode, configRoot.getNode(sites), channelId + "-preview");

            Node previewContentMirrorNode = previewSiteNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);
            previewContentMirrorNode.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {"hippo:availability"});
            previewContentMirrorNode.setProperty(HippoNodeType.HIPPO_VALUES, new String[] {"preview"});
            previewContentMirrorNode.setProperty(HippoNodeType.HIPPO_MODES, new String[] {"single"});

            // set up content
            final String contentRootPath;
            if (bps.hasContentPrototype()) {
                String blueprintContentPath = bps.getContentRoot();
                if(blueprintContentPath == null) {
                    blueprintContentPath = contentRoot;
                }
                FolderWorkflow fw = (FolderWorkflow) getWorkflow("subsite", session.getNode(blueprintContentPath));
                try {
                    contentRootPath = fw.add("new-subsite", bps.getId(), channelId);
                    session.refresh(true);

                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflow("core", session.getNode(contentRootPath));
                    defaultWorkflow.localizeName(channel.getName());

                    session.refresh(true);
                } catch (WorkflowException e) {
                    throw new ChannelException("Could not create content root", e);
                } catch (RemoteException e) {
                    throw new ChannelException("Could not create content root", e);
                }
            } else {
                contentRootPath = channel.getContentRoot();
            }
            if (contentRootPath != null) {
                final Node contentMirrorNode = siteNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);
                previewContentMirrorNode = previewSiteNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);
                if (jcrSession.itemExists(contentRootPath)) {
                    contentMirrorNode.setProperty(HippoNodeType.HIPPO_DOCBASE, jcrSession.getNode(contentRootPath).getIdentifier());
                    previewContentMirrorNode.setProperty(HippoNodeType.HIPPO_DOCBASE, jcrSession.getNode(contentRootPath).getIdentifier());
                } else {
                    log.warn("Specified content root '" + contentRootPath + "' does not exist");
                    contentMirrorNode.setProperty(HippoNodeType.HIPPO_DOCBASE, jcrSession.getNode(contentRootPath).getIdentifier());
                    previewContentMirrorNode.setProperty(HippoNodeType.HIPPO_DOCBASE, jcrSession.getNode(contentRootPath).getIdentifier());
                }

            }
        }

        // create mount
        Node mount = createMountNode(virtualHost, blueprintNode, channelUri.getPath());
        mount.setProperty(HstNodeTypes.MOUNT_PROPERTY_CHANNELPATH, channelsRoot + channelId);
        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_SITE)) {
            mount.setProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT, mountPointPath);

            final String locale = channel.getLocale();
            if (locale != null) {
                mount.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE, locale);
            }
        } else if (mount.hasProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
            mount.getProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT).remove();
        }
    }

    private Node createMountNode(Node virtualHost, final Node blueprintNode, final String mountPath) throws MountNotFoundException, RepositoryException {
        ArrayList<String> mountPathElements = new ArrayList<String>();
        mountPathElements.add(HstNodeTypes.MOUNT_HST_ROOTNAME);
        mountPathElements.addAll(Arrays.asList(StringUtils.split(mountPath, '/')));

        Node mount = virtualHost;

        for (int i = 0; i < mountPathElements.size() - 1; i++) {
            String mountPathElement = mountPathElements.get(i);
            if (mount.hasNode(mountPathElement)) {
                mount = mount.getNode(mountPathElement);
            } else {
                throw new MountNotFoundException(mount.getPath() + "/" + mountPathElement);
            }
        }

        String lastMountPathElementName = mountPathElements.get(mountPathElements.size() - 1);

        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_MOUNT)) {
            mount = copyNodes(blueprintNode.getNode(HstNodeTypes.NODENAME_HST_MOUNT), mount, lastMountPathElementName);
        } else {
            mount = mount.addNode(lastMountPathElementName, HstNodeTypes.NODETYPE_HST_MOUNT);
        }

        return mount;
    }

    private Node getOrCreateVirtualHost(final Node configRoot, final String hostName) throws RepositoryException {
        final String[] elements = hostName.split("[.]");

        Node mount = configRoot.getNode(HstNodeTypes.NODENAME_HST_HOSTS + "/" + hostGroup);

        for (int i = elements.length - 1; i >= 0; i--) {
            mount = getOrAddNode(mount, elements[i], HstNodeTypes.NODETYPE_HST_VIRTUALHOST);
        }

        return mount;
    }

    private static Node getOrAddNode(Node parent, String nodeName, String nodeType) throws RepositoryException {
        if (parent.hasNode(nodeName)) {
            return parent.getNode(nodeName);
        } else {
            return parent.addNode(nodeName, nodeType);
        }
    }

    private static String getStringPropertyOrDefault(Node node, String propName, String defaultValue) throws RepositoryException {
        if (node.hasProperty(propName)) {
            return node.getProperty(propName).getString();
        }
        return defaultValue;
    }

    static Node copyNodes(Node source, Node parent, String name) throws RepositoryException {
        Node clone = parent.addNode(name, source.getPrimaryNodeType().getName());
        for (NodeType mixin : source.getMixinNodeTypes()) {
            clone.addMixin(mixin.getName());
        }
        for (PropertyIterator pi = source.getProperties(); pi.hasNext(); ) {
            Property prop = pi.nextProperty();
            if (prop.getDefinition().isProtected()) {
                continue;
            }
            if (prop.isMultiple()) {
                clone.setProperty(prop.getName(), prop.getValues());
            } else {
                clone.setProperty(prop.getName(), prop.getValue());
            }
        }
        for (NodeIterator ni = source.getNodes(); ni.hasNext(); ) {
            Node node = ni.nextNode();
            if (isVirtual(node)) {
                continue;
            }

            copyNodes(node, clone, node.getName());
        }
        return clone;
    }

    public Workflow getWorkflow(String category, Node node) throws RepositoryException {
        Workspace workspace = node.getSession().getWorkspace();
        WorkflowManager wfm = ((HippoWorkspace) workspace).getWorkflowManager();
        return wfm.getWorkflow(category, node);
    }

    private static boolean isVirtual(final Node node) throws RepositoryException {
        // skip virtual nodes
        if (node instanceof HippoNode) {
            HippoNode hn = (HippoNode) node;
            try {
                Node canonicalNode = hn.getCanonicalNode();
                if (canonicalNode == null) {
                    return true;
                }
                if (!canonicalNode.isSame(hn)) {
                    return true;
                }
            } catch (ItemNotFoundException infe) {
                return true;
            }
        }
        return false;
    }

    private void updateChannel(Node configRoot, final Channel channel) throws ChannelException, RepositoryException {
        URI channelUri = getChannelUri(channel);
        Node virtualHost = getOrCreateVirtualHost(configRoot, channelUri.getHost());

        // resolve mount
        Node mount;
        if (virtualHost.hasNode(HstNodeTypes.MOUNT_HST_ROOTNAME)) {
            mount = virtualHost.getNode(HstNodeTypes.MOUNT_HST_ROOTNAME);
        } else {
            throw new MountNotFoundException(virtualHost.getPath() + "/" + HstNodeTypes.MOUNT_HST_ROOTNAME);
        }
        final String mountPath = channel.getMountPath();
        if (mountPath != null) {
            for (String mountPathElement : StringUtils.split(mountPath, '/')) {
                if (mount.hasNode(mountPathElement)) {
                    mount = mount.getNode(mountPathElement);
                } else {
                    throw new MountNotFoundException(mount.getPath() + "/" + mountPathElement);
                }
            }
        }

        ChannelPropertyMapper.saveChannel(configRoot.getNode(HstNodeTypes.NODENAME_HST_CHANNELS + "/" + channel.getId()), channel);
    }

    /**
     * Returns the channel's URL is a URI object. The returned URI has a supported scheme and a host name.
     *
     * @param channel the channel
     * @return the validated URI of the channel
     * @throws ChannelException if the channel URL is not a valid URI, does not have a supported scheme or does not
     *                          contain a host name.
     */
    private URI getChannelUri(final Channel channel) throws ChannelException {
        URI uri;

        try {
            uri = new URI(channel.getUrl());
        } catch (URISyntaxException e) {
            throw new ChannelException("Invalid channel URL: '" + channel.getUrl() + "'");
        }

        if (!"http".equals(uri.getScheme())) {
            throw new ChannelException("Illegal channel URL scheme: '" + uri.getScheme() + "'. Only 'http' is currently supported");
        }

        if (StringUtils.isBlank(uri.getHost())) {
            throw new ChannelException("Channel URL '" + uri + "' does not contain a host name");
        }

        return uri;
    }



}
