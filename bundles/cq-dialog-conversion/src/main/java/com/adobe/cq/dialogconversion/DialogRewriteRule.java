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

package com.adobe.cq.dialogconversion;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Set;

/**
 * Interface for services that implement a dialog rewrite rule. A rewrite rule matches certain subtrees of the
 * dialog tree (usually corresponding to one dialog component) and rewrites (i.e. modifies or replaces) them.
 */
public interface DialogRewriteRule {

    /**
     * Returns true if this rule matches the given subtree.
     *
     * @param root The root of the subtree to be checked for a match
     * @return true if this rule applies, false otherwise
     */
    boolean matches(Node root)
            throws RepositoryException;

    /**
     * <p>Applies this rule to the subtree rooted at the specified <code>root</code> node. The implementation of this
     * method may either modify the properties and nodes contained in that tree, or replace it by adding a new child
     * to the parent of <code>root</code>. In the latter case, the implementation is responsible for removing the
     * original subtree (without saving).</p>
     *
     * <p>Rewrite rules must not rewrite trees in a circular fashion, as this might lead to infinite loops.</p>
     *
     * <p>Optionally, the implementation can indicate which nodes of the resulting tree are final and therefore
     * safe for the algorithm to skip in subsequent traversals of the tree. Add the paths of final nodes to the
     * specified set.
     *
     * <p>{@link DialogRewriteUtils} provides utility methods that can
     * be used to temporarily rename (move) the original subtree, so that the resulting subtree can be built
     * while still having the original around.</p>
     *
     * @param root The root of the subtree to be rewritten
     * @return the root node of the rewritten tree, or null if it was removed
     * @throws DialogRewriteException if the rewrite operation failed or cannot be completed
     */
    Node applyTo(Node root, Set<Node> finalNodes)
            throws DialogRewriteException, RepositoryException;

    /**
     * Returns the ranking of this rule (the lower the ranking the higher the priority). If the return value
     * is negative, then the rule will have the lowest priority possible. The order of rules with equal rankings
     * is arbitrary.
     *
     * @return The ranking
     */
    int getRanking();

}
