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

package com.adobe.cq.dialogupgrade.treerewriter;

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
     * <p>Applies this rule to the subtree rooted at the specified <code>root</code> node. The implementation of this
     * method may either modify the properties and nodes contained in this subtree, or replace it by adding a new child
     * to the parent of <code>root</code>. In the latter case, the implementation is responsible for removing the
     * original subtree (no need to save the changes though).</p>
     *
     * <p>The rewrite algorithm does not detect rewrite loops, therefore rewrite rules must not rewrite trees in a
     * circular fashion. Also, a rewrite rule must not leave the original tree unchanged, as the algorithm will
     * get stuck in an infinite loop.</p>
     *
     * <p>Optionally, the <code>finalNodes</code> parameter can be used to control and optimize the rewrite algorithm.
     * The implementation can add to this set all nodes of the rewritten subtree which are final and therefore
     * safe for the algorithm to ignore in subsequent traversals of the tree.</p>
     *
     * <p>{@link com.adobe.cq.dialogupgrade.treerewriter.TreeRewriterUtils} provides utility methods that can
     * be used to temporarily rename (move) the original subtree, so that the resulting subtree can be built
     * while still having the original around.</p>
     *
     * @param root The root of the subtree to be rewritten
     * @return the root node of the rewritten tree, or null if it was removed
     * @throws RewriteException if the rewrite operation failed or cannot be completed
     */
    Node applyTo(Node root, Set<Node> finalNodes)
            throws RewriteException, RepositoryException;

    /**
     * Determines the ranking of this rule (the lower the ranking the higher the priority). If the return value
     * is negative, then the rule will have the lowest priority possible.
     *
     * @return The ranking
     */
    int getRanking();

}
