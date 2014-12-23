/*************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * __________________
 *
 *  Copyright 2014 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/

package com.adobe.cq.dialogupgrade;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Provides utility methods.
 */
public class DialogUpgradeUtils {

    /**
     * Shortcut method to check if a node has a certain xtype.
     *
     * @param node The node to check
     * @param xtype The xtype to check or null to check if the node has no xtype
     * @return true if the node has the specified xtype, false otherwise
     * @throws RepositoryException
     */
    public static boolean hasXtype(Node node, String xtype)
            throws RepositoryException {
        if (xtype == null) {
            // if 'xtype' is null, we return true if the node does not have an xtype
            return !node.hasProperty("xtype");
        }
        return node.hasProperty("xtype") && xtype.equals(node.getProperty("xtype").getString());
    }

    /**
     * Shortcut method to check if a node has a certain type.
     *
     * @param node The node to check
     * @param type The type to check or null to check if the node has no type
     * @return true if the node has the specified type, false otherwise
     * @throws RepositoryException
     */
    public static boolean hasType(Node node, String type)
            throws RepositoryException {
        if (type == null) {
            // if 'type' is null, we return true if the node does not have a type
            return !node.hasProperty("type");
        }
        return node.hasProperty("type") && type.equals(node.getProperty("type").getString());
    }

}
