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

package com.adobe.cq.dialogupgrade.treerewriter.rules;

import com.adobe.cq.dialogupgrade.treerewriter.RewriteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * A rewrite rule rewrites a tree by replacing specific subtrees that it matches.
 */
public interface RewriteRule {

    /**
     * Determines if this rule applies to the specified subtree.
     *
     * @param root The root of the subtree to be checked for a match
     * @return true if this rule applies, false otherwise
     */
    boolean matches(Node root)
            throws RepositoryException;

    /**
     * Applies this rule to the specified subtree. This rewrites the subtree according
     * to the definition of this rule.
     *
     * Care has to be taken about the rewrite operation. If it leaves the original tree unchanged,
     * the rewrite algorithm will get stuck in an infinite loop.
     *
     * todo: describe rename
     *
     * @param root The root of the subtree to be rewritten
     * @return the root node of the rewritten tree, or null if it was removed
     */
    Node applyTo(Node root)
            throws RewriteException, RepositoryException;

}
