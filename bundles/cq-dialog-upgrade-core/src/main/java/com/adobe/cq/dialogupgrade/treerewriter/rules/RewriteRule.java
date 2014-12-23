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
     * The rewrite algorithm does not detect rewrite loops, therefore rewrite rules must not rewrite trees in a
     * circular fashion. Also, a rewrite rule must not leave the original tree unchanged, as the algorithm will
     * get stuck in an infinite loop (unless <code>finalNodes</code> is used appropriately, see below).
     *
     * Optionally, the <code>finalNodes</code> parameter can be used to control and optimize the rewrite algorithm.
     * The implementation can add to this set all nodes of the rewritten subtree which are final and therefore
     * safe for the algorithm to ignore in subsequent traversals of the tree.
     *
     * {@link com.adobe.cq.dialogupgrade.treerewriter.TreeRewriterUtils} provides utility methods that can
     * be used to temporarily rename (move) the original subtree, so that the resulting subtree can be built
     * while still having the original around.
     *
     * @param root The root of the subtree to be rewritten
     * @return the root node of the rewritten tree, or null if it was removed
     * @throws RewriteException if the rewrite operation failed or cannot be completed
     */
    Node applyTo(Node root, Set<Node> finalNodes)
            throws RewriteException, RepositoryException;

    /**
     * Determines the ranking of this rule (the lower the ranking the higher the priority).
     *
     * @return The ranking
     */
    int getRanking();

}
