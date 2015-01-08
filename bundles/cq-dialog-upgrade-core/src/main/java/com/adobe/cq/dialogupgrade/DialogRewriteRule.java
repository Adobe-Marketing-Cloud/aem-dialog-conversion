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

import com.adobe.cq.dialogupgrade.treerewriter.RewriteRule;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Set;

/**
 * Interface for classes that implement a dialog rewrite rule.
 * @see com.adobe.cq.dialogupgrade.treerewriter.RewriteRule
 */
public interface DialogRewriteRule extends RewriteRule {

    /**
     * @throws DialogRewriteException if the dialog rewrite operation failed or cannot be completed
     * @see com.adobe.cq.dialogupgrade.treerewriter.RewriteRule#applyTo
     */
    Node applyTo(Node root, Set<Node> finalNodes)
            throws DialogRewriteException, RepositoryException;

}