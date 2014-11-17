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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Set;

@Component
@Service
@Properties({
        @Property(name="service.ranking", intValue = 5000)
})
public class CqIncludeRewriteRule implements DialogRewriteRule {

    public boolean matches(Node root) throws RepositoryException {
        return root.hasProperty("xtype") && "cqinclude".equals(root.getProperty("xtype").getString());
    }

    public Node applyTo(Node root, Set<Node> finalNodes)
            throws RewriteException, RepositoryException {
        if (!root.hasProperty("path")) {
            throw new RewriteException("Missing include path");
        }

        String path = root.getProperty("path").getString();
        // todo: improve
        path = path.replace(".overlay.infinity.json", "");
        path = path.replace(".infinity.json", "");

        Session session = root.getSession();
        if (!session.nodeExists(path)) {
            throw new RewriteException("Include path does not resolve");
        }
        Node parent = root.getParent();
        String name = root.getName();
        root.remove();
        return JcrUtil.copy(session.getNode(path), parent, name);
    }

}
