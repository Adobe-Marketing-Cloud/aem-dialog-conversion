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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

@Component
@Service
@Properties({
    @Property(name="service.ranking", intValue = 5000)
})
public class CqDialogRewriteRule implements DialogRewriteRule {

    public boolean matches(Node root)
            throws RepositoryException {
        if ("cq:Dialog".equals(root.getPrimaryNodeType().getName())) {
            return true;
        }
        return false;
    }

    public Node applyTo(Node root)
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

        // add cq:content and cq:content/content nodes
        Node cqDialog = parent.addNode("cq:dialog", "nt:unstructured");
        // todo: remove
        cqDialog.setProperty("dialog-upgrade-tool", true);
        if (root.hasProperty("helpPath")) {
            cqDialog.setProperty("helpPath", root.getProperty("helpPath").getValue());
        }
        if (root.hasProperty("title")) {
            cqDialog.setProperty("jcr:title", root.getProperty("title").getValue());
        }
        cqDialog.setProperty("sling:resourceType", "cq/gui/components/authoring/dialog");
        Node content = cqDialog.addNode("content", "nt:unstructured");
        content.setProperty("sling:resourceType", "granite/ui/components/foundation/container");
        Node layout = content.addNode("layout", "nt:unstructured");
        Node items = content.addNode("items", "nt:unstructured");

        if (isTabbed) {
            layout.setProperty("sling:resourceType", "granite/ui/components/foundation/layouts/tabs");
            layout.setProperty("type", "nav");
        } else {
            layout.setProperty("sling:resourceType", "granite/ui/components/foundation/layouts/fixedcolumns");
            Node column = items.addNode("column", "nt:unstructured");
            column.setProperty("sling:resourceType", "granite/ui/components/foundation/container");
            items = column.addNode("items", "nt:unstructured");
        }
        NodeIterator iterator = dialogItems.getNodes();
        while (iterator.hasNext()) {
            Node item = iterator.nextNode();
            JcrUtil.copy(item, items, item.getName());
        }

        return cqDialog;
    }

    private boolean isTabbed(Node dialog)
            throws RepositoryException {
        String xtype = dialog.hasProperty("xtype") ? dialog.getProperty("xtype").getString() : null;

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

    private Node getChild(Node node, String name)
            throws RepositoryException {
        if (node.hasNode(name)) {
            return node.getNode(name);
        }
        return null;
    }

    private boolean isTabPanel(Node node)
            throws RepositoryException {
        if (node == null) {
            return false;
        }
        if ("cq:TabPanel".equals(node.getPrimaryNodeType().getName())) {
            return true;
        }
        if (isXtype(node, "tabpanel")) {
            return true;
        }
        return false;
    }

    private boolean isXtype(Node node, String xtype)
            throws RepositoryException {
        return node.hasProperty("xtype") && xtype != null && xtype.equals(node.getProperty("xtype").getString());
    }

}
