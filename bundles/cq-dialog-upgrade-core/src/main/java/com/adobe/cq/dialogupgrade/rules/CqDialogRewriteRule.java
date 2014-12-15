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

package com.adobe.cq.dialogupgrade.rules;

import com.adobe.cq.dialogupgrade.api.DialogRewriteRule;
import com.adobe.cq.dialogupgrade.treerewriter.RewriteException;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.Set;

import static com.adobe.cq.dialogupgrade.DialogUpgradeUtils.hasXtype;
import static com.adobe.cq.dialogupgrade.treerewriter.TreeRewriterUtils.hasPrimaryType;

/**
 * Rule that rewrites the basic structure of dialogs. It creates a Granite UI container using either a "tabs" or
 * "fixedcolumns" layout. The components (tabs or widgets) of the original dialog are copied over and will be handled
 * by subsequent passes of the algorithm.
 */
@Component
@Service
@Properties({
    @Property(name="service.ranking", intValue = 1)
})
public class CqDialogRewriteRule extends AbstractRewriteRule {

    private static final String PRIMARY_TYPE = "cq:Dialog";

    public boolean matches(Node root)
            throws RepositoryException {
        return hasPrimaryType(root, PRIMARY_TYPE);
    }

    public Node applyTo(Node root, Set<Node> finalNodes)
            throws RewriteException, RepositoryException {
        // Granite UI dialog already exists at this location
        Node parent = root.getParent();
        if (parent.hasNode("cq:dialog")) {
            throw new RewriteException("Could not rewrite dialog: cq:dialog node already exists");
        }

        boolean isTabbed = isTabbed(root);
        // get the items: in case of a tabbed dialog, these represent tabs, otherwise widgets
        Node dialogItems = getDialogItems(root);
        if (dialogItems == null) {
            throw new RewriteException("Unable to find the dialog items");
        }

        // cq:dialog
        Node cqDialog = parent.addNode("cq:dialog", "nt:unstructured");
        finalNodes.add(cqDialog);
        if (root.hasProperty("helpPath")) {
            cqDialog.setProperty("helpPath", root.getProperty("helpPath").getValue());
        }
        if (root.hasProperty("title")) {
            cqDialog.setProperty("jcr:title", root.getProperty("title").getValue());
        }
        cqDialog.setProperty("sling:resourceType", "cq/gui/components/authoring/dialog");
        // cq:dialog/content
        Node content = cqDialog.addNode("content", "nt:unstructured");
        finalNodes.add(content);
        content.setProperty("sling:resourceType", "granite/ui/components/foundation/container");
        // cq:dialog/content/layout
        Node layout = content.addNode("layout", "nt:unstructured");
        finalNodes.add(layout);
        // cq:dialog/content/items
        Node items = content.addNode("items", "nt:unstructured");
        finalNodes.add(items);

        if (isTabbed) {
            // tab layout
            layout.setProperty("sling:resourceType", "granite/ui/components/foundation/layouts/tabs");
            layout.setProperty("type", "nav");
        } else {
            // fixedcolumn layout
            layout.setProperty("sling:resourceType", "granite/ui/components/foundation/layouts/fixedcolumns");
            // cq:dialog/content/items/column
            Node column = items.addNode("column", "nt:unstructured");
            finalNodes.add(column);
            column.setProperty("sling:resourceType", "granite/ui/components/foundation/container");
            // cq:dialog/content/items/column/items
            items = column.addNode("items", "nt:unstructured");
            finalNodes.add(items);
        }
        // copy items
        NodeIterator iterator = dialogItems.getNodes();
        while (iterator.hasNext()) {
            Node item = iterator.nextNode();
            JcrUtil.copy(item, items, item.getName());
        }

        // we do not remove the original tree (as we want to keep the old dialog), so
        // we simply return the new root
        return cqDialog;
    }

    /**
     * Returns true if this dialog contains tabs, false otherwise.
     */
    private boolean isTabbed(Node dialog)
            throws RepositoryException {
        if (isTabPanel(dialog)) {
            return true;
        }
        Node items = getChild(dialog, "items");
        if (isTabPanel(items)) {
            return true;
        }
        if (items != null && isTabPanel(getChild(items, "tabs"))) {
            return true;
        }
        return false;
    }

    /**
     * Returns the items that this dialog consists of. These might be components, or - in case of a tabbed
     * dialog - tabs.
     */
    private Node getDialogItems(Node dialog)
            throws RepositoryException {
        // find first sub node called "items" of type "cq:WidgetCollection"
        Node items = dialog;
        do {
            items = getChild(items, "items");
        } while (items != null && !"cq:WidgetCollection".equals(items.getPrimaryNodeType().getName()));
        if (items == null) {
            return null;
        }

        // check if there is a tab panel child called "tabs"
        Node tabs = getChild(items, "tabs");
        if (tabs != null && isTabPanel(tabs)) {
            return getChild(tabs, "items");
        }

        return items;
    }

    /**
     * Returns the child with the given name or null if it doesn't exist.
     */
    private Node getChild(Node node, String name)
            throws RepositoryException {
        if (node.hasNode(name)) {
            return node.getNode(name);
        }
        return null;
    }

    /**
     * Returns true if the specified node is a tab panel, false otherwise.
     */
    private boolean isTabPanel(Node node)
            throws RepositoryException {
        if (node == null) {
            return false;
        }
        if ("cq:TabPanel".equals(node.getPrimaryNodeType().getName())) {
            return true;
        }
        if (hasXtype(node, "tabpanel")) {
            return true;
        }
        return false;
    }

}
