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
import java.util.Set;

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
     * After the application of the rule, the rewrite algorithm will recursively proceed on all nodes of
     * the rewritten subtree (including the root). Therefore, care has to be taken during the rewrite
     * operation: if it leaves the original tree unchanged, the algorithm will get stuck in an infinite loop.
     *
     * Optionally, the <code>finalNodes</code> parameter can be used to control and optimize the rewrite algorithm.
     * The implementation can add to this set all nodes of the rewritten subtree which are final and therefore
     * safe for the algorithm to subsequently ignore.
     *
     * todo: describe rename
     *
     * @param root The root of the subtree to be rewritten
     * @return the root node of the rewritten tree, or null if it was removed
     */
    Node applyTo(Node root, Set<Node> finalNodes)
            throws RewriteException, RepositoryException;

}
