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

package com.adobe.cq.dialogupgrade.impl.rules;

import com.adobe.cq.dialogupgrade.AbstractDialogRewriteRule;
import com.adobe.cq.dialogupgrade.DialogRewriteException;
import com.day.cq.commons.PathInfo;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.request.RequestPathInfo;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Set;

import static com.adobe.cq.dialogupgrade.DialogUpgradeUtils.hasPrimaryType;
import static com.adobe.cq.dialogupgrade.DialogUpgradeUtils.hasXtype;

/**
 * Rewrites widgets of xtype "cqinclude". The referenced widget is copied over and will be handled by
 * subsequent passes of the algorithm.
 */
@Component
@Service
@Properties({
        @Property(name="service.ranking", intValue = 2)
})
public class IncludeRewriteRule extends AbstractDialogRewriteRule {

    private static final String XTYPE = "cqinclude";

    public boolean matches(Node root)
            throws RepositoryException {
        return hasXtype(root, XTYPE);
    }

    public Node applyTo(Node root, Set<Node> finalNodes)
            throws DialogRewriteException, RepositoryException {
        // check if the 'path property exists
        if (!root.hasProperty("path")) {
            throw new DialogRewriteException("Missing include path");
        }

        // get path to included node
        RequestPathInfo info = new PathInfo(root.getProperty("path").getString());
        String path = info.getResourcePath();

        // check if the path is valid
        Session session = root.getSession();
        if (!session.nodeExists(path)) {
            throw new DialogRewriteException("Include path does not exist");
        }

        // remove original
        Node parent = root.getParent();
        String name = root.getName();
        root.remove();

        Node node = session.getNode(path);
        // check if referenced node is a widget collection
        if (hasPrimaryType(node, "cq:WidgetCollection")) {
            NodeIterator iterator = node.getNodes();
            Node newRoot = null;
            // copy all items of the widget collection
            while (iterator.hasNext()) {
                Node item = iterator.nextNode();
                Node copy = JcrUtil.copy(item, parent, JcrUtil.createValidChildName(parent, item.getName()));
                if (newRoot == null) {
                    newRoot = copy;
                }
            }
            // we return the first item as the new root
            return newRoot;
        } else {
            return JcrUtil.copy(session.getNode(path), parent, name);
        }
    }

}
