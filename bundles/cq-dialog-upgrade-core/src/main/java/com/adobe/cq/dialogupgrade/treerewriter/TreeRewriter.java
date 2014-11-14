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

import com.adobe.cq.dialogupgrade.treerewriter.rules.RewriteRule;
import org.apache.jackrabbit.commons.flat.TreeTraverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TreeRewriter {

    private Logger logger = LoggerFactory.getLogger(TreeRewriter.class);

    private List<RewriteRule> rules;

    public TreeRewriter(List<RewriteRule> rules) {
        this.rules = rules;
    }

    /**
     * Rewrites the specified tree according to the set of rules passed to the constructor.
     *
     * @param root The root of the tree to be rewritten
     * @return the root node of the rewritten tree, or null if it was removed
     * @throws RewriteException If the rewrite operation fails
     * @throws RepositoryException If there is a problem with the repository
     */
    public Node rewrite(Node root)
            throws RewriteException, RepositoryException {
        logger.info("Rewriting tree rooted at {}", root.getPath());
        long tick = System.currentTimeMillis();

        /**
         * Description of the algorithm:
         * - traverse the tree rooted at 'root' in pre-order
         * - for each node we check if one of the rules match
         * - on a match, the (sub)tree rooted at that node is rewritten according to that rule,
         *   and we restart the traversal from 'root'
         * - the algorithm stops when the whole tree has been traversed and no node has matched any rule
         * - some special care has to be taken to keep the orderings of child nodes when rewriting subtrees
         */

        Session session = root.getSession();
        // reference to the node where the pre-order traversal is started from
        Node startNode = root;
        // keeps track of whether or not there was a match during a traversal
        boolean foundMatch;
        // keeps track of whether or not the rewrite operation succeeded
        boolean success = false;

        try {
            // do a pre-order tree traversal until we found no match
            do {
                foundMatch = false;
                TreeTraverser traverser = new TreeTraverser(startNode);
                Iterator<Node> iterator = traverser.iterator();
                // traverse the tree in pre-order
                while (iterator.hasNext()) {
                    Node node = iterator.next();

                    // if this node and its siblings are ordered..
                    if (node.getParent().getPrimaryNodeType().hasOrderableChildNodes()) {
                        // ..then we move it to the end of its parent's list of children. This is necessary because
                        // any of its siblings might be rewritten (which might move it to the end of the list). Thus
                        // we do this for all siblings in order to keep the order.
                        node.getParent().orderBefore(node.getName(), null);
                    }
                    if (foundMatch) {
                        // we have previously found a match (and will start a new traversal from the start node)
                        // but we still need to finish this traversal in order not to change the order of nodes
                        continue;
                    }

                    // traverse all available rules
                    for (RewriteRule rule : rules) {
                        // check for a match
                        if (rule.matches(node)) {
                            // the rule matched, rewrite the tree
                            Node result = rule.applyTo(node);
                            // set the start node in case it was rewritten
                            if (node.equals(startNode)) {
                                startNode = result;
                            }
                            foundMatch = true;
                            break;
                        }
                    }
                }
            } while (foundMatch && startNode != null);
            success = true;
        } finally {
            if (!success) {
                // an exception has been thrown: try to revert changes
                try {
                    session.refresh(false);
                } catch (RepositoryException e) {
                    logger.warn("Could not revert changes", e);
                }
            }
        }

        // save changes
        session.save();

        long tack = System.currentTimeMillis();
        logger.info("Tree rooted at {} rewritten in {} ms", root.getPath(), tack - tick);

        return startNode;
    }

}
