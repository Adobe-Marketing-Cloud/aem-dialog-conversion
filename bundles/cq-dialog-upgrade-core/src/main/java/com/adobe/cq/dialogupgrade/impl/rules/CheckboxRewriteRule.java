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
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Set;

import static com.adobe.cq.dialogupgrade.DialogUpgradeUtils.copyProperty;
import static com.adobe.cq.dialogupgrade.DialogUpgradeUtils.hasType;
import static com.adobe.cq.dialogupgrade.DialogUpgradeUtils.hasXtype;
import static com.adobe.cq.dialogupgrade.DialogUpgradeUtils.renameToTemp;

/**
 * Rewrites checkbox widgets to Granite UI checkboxes. Additionally, a hidden field is added as a sibling node,
 * which is needed to make unchecking the checkbox work.
 */
@Component
@Service
public class CheckboxRewriteRule extends AbstractDialogRewriteRule {

    private static final String XTYPE = "selection";
    private static final String TYPE = "checkbox";
    private static final String GRANITEUI_CHECKBOX_RT = "granite/ui/components/foundation/form/checkbox";
    private static final String GRANITEUI_HIDDEN_RT = "granite/ui/components/foundation/form/hidden";

    public boolean matches(Node root)
            throws RepositoryException {
        return (hasXtype(root, XTYPE) && hasType(root, TYPE)) || hasXtype(root, TYPE);
    }

    public Node applyTo(Node root, Set<Node> finalNodes)
            throws DialogRewriteException, RepositoryException {
        Node parent = root.getParent();
        String name = root.getName();
        renameToTemp(root);

        // add node for checkbox
        Node newRoot = parent.addNode(name, "nt:unstructured");
        finalNodes.add(newRoot);
        newRoot.setProperty("sling:resourceType", GRANITEUI_CHECKBOX_RT);
        // set properties
        if (copyProperty(root, "fieldLabel", newRoot, "text") == null) {
            copyProperty(root, "boxLabel", newRoot, "text");
        }
        copyProperty(root, "fieldDescription", newRoot, "fieldDescription");
        copyProperty(root, "name", newRoot, "name");
        if (copyProperty(root, "inputValue", newRoot, "value") == null) {
            if (copyProperty(root, "value", newRoot, "value") == null) {
                copyProperty(root, "inputValue", newRoot, "value");
            }
        }
        copyProperty(root, "disabled", newRoot, "disabled");
        copyProperty(root, "readOnly", newRoot, "renderReadOnly");
        copyProperty(root, "checked", newRoot, "checked");

        // add hidden field to enable unchecking the checkbox
        name = JcrUtil.createValidChildName(parent, name + "-delete");
        Node hidden = parent.addNode(name, "nt:unstructured");
        finalNodes.add(hidden);
        hidden.setProperty("sling:resourceType", GRANITEUI_HIDDEN_RT);
        hidden.setProperty("name", newRoot.getProperty("name").getString() + "@Delete");
        hidden.setProperty("value", true);

        // remove old root and return new root
        root.remove();
        return newRoot;
    }

}
