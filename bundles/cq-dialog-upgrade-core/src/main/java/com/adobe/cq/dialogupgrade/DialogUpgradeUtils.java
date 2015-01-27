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

import com.day.cq.commons.jcr.JcrUtil;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Provides helper methods to be used by dialog rewrite rules.
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

    /**
     * Shortcut method to check if a node has a certain primary type.
     *
     * @param node The node to check
     * @param typeName The name of the primary type to check
     * @return true if the node has the specified primary type, false otherwise
     * @throws RepositoryException
     */
    public static boolean hasPrimaryType(Node node, String typeName)
            throws RepositoryException {
        return typeName != null && typeName.equals(node.getPrimaryNodeType().getName());
    }

    /**
     * Renames the specified node to a temporary name.
     *
     * @param node The node to be renamed
     * @throws RepositoryException
     */
    public static void renameToTemp(Node node)
            throws RepositoryException {
        moveToTemp(node, node.getParent());
    }

    /**
     * Moves the specified node to a be a child of the destination with a temporary name.
     *
     * @param node The node to be moved
     * @param destination The destination where the node should be added as a child
     * @throws RepositoryException
     */
    public static void moveToTemp(Node node, Node destination)
            throws RepositoryException {
        Session session = node.getSession();
        String tmpName = JcrUtil.createValidChildName(destination, "tree-rewriter-tmp-" + System.currentTimeMillis());
        String tmpPath = destination.getPath() + "/" + tmpName;
        session.move(node.getPath(), tmpPath);
    }

    /**
     * Copies a property from a source to a destination node.
     *
     * @param source The source node
     * @param relPropertyPath The relative path to the property to be copied
     * @param destination The destination node
     * @param name The name for the copy of the property
     * @return The copied property or null if the source property doesn't exist
     * @throws RepositoryException
     */
    public static Property copyProperty(Node source, String relPropertyPath, Node destination, String name)
            throws RepositoryException {
        if (!source.hasProperty(relPropertyPath)) {
            return null;
        }
        return JcrUtil.copy(source.getProperty(relPropertyPath), destination, name);
    }

}
