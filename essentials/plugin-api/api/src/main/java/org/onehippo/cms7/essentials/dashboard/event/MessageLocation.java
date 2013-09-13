package org.onehippo.cms7.essentials.dashboard.event;

/**
 * Enum which indicates where message should be shown
 *
 * @version "$Id: MessageLocation.java 174583 2013-08-21 17:05:38Z mmilicevic $"
 */
public enum MessageLocation {
    /**
     * Indicates message should be shown in global toolbar
     */
    GLOBAL_TOOLBAR,
    /**
     * Indicates message should be shown in plugin toolbar (header)
     */
    PLUGIN_HEADER,
    /**
     * Indicates message should be shown in plugin content area
     */
    PLUGIN_CONTENT,
    /**
     * Indicates message is a system message (hidden)
     */
    SYSTEM
}
