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

package com.adobe.cq.dialogupgrade.treerewriter.impl;

import com.adobe.cq.dialogupgrade.treerewriter.RewriteRule;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.LinkedList;
import java.util.List;

/**
 * Factory class used to create rewrite rules.
 */
public class RewriteRulesFactory {

    /**
     * Creates a rewrite rule based on a tree structure.
     *
     * @see {@link NodeBasedRewriteRule}
     * @param ruleNode The root node of the tree structure defining the rule
     * @return the rewrite rule
     */
    public static RewriteRule createRule(Node ruleNode) {
        return new NodeBasedRewriteRule(ruleNode);
    }

    /**
     * Returns a list of rewrite rules. This is a shortcut of calling {@link #createRule(javax.jcr.Node)}
     * for all the child nodes of the specified container node.
     *
     * @param rulesContainerNode A container node, whose child nodes are tree structures that define rules
     * @return A list of rewrite rules
     * @throws RepositoryException if there is a problem with the repository
     */
    public static List<RewriteRule> createRules(Node rulesContainerNode)
            throws RepositoryException {
        List<RewriteRule> rules = new LinkedList<RewriteRule>();
        NodeIterator iterator = rulesContainerNode.getNodes();
        while (iterator.hasNext()) {
            rules.add(createRule(iterator.nextNode()));
        }
        return rules;
    }

}
