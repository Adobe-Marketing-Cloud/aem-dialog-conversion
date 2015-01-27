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

package com.adobe.cq.dialogconversion.impl;

import com.adobe.cq.dialogconversion.DialogRewriteException;
import com.adobe.cq.dialogconversion.DialogRewriteRule;
import org.apache.jackrabbit.commons.flat.TreeTraverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DialogTreeRewriter {

    private Logger logger = LoggerFactory.getLogger(DialogTreeRewriter.class);

    private List<DialogRewriteRule> rules;

    public DialogTreeRewriter(List<DialogRewriteRule> rules) {
        this.rules = rules;
    }

    private void check(Node root)
            throws DialogRewriteException, RepositoryException {
        // verify that the node has a primary type of cq:Dialog
        if (!"cq:Dialog".equals(root.getPrimaryNodeType().getName())) {
            logger.debug("{} is not a Classic UI dialog", root.getPath());
            throw new DialogRewriteException("Node is not a cq:Dialog");
        }

        // verify that the Touch UI version of the dialog doesn't exist
        if (root.getParent().hasNode("cq:dialog")) {
            logger.debug("Dialog {} already has a Touch UI counterpart", root.getPath());
            throw new DialogRewriteException("Touch UI dialog already exists");
        }
    }

    /**
     * Rewrites the specified dialog tree according to the set of rules passed to the constructor.
     *
     * @param root The root of the dialog be rewritten (must be of primary type cq:Dialog)
     * @return the root node of the rewritten dialog tree, or null if it was removed
     * @throws com.adobe.cq.dialogconversion.DialogRewriteException If the rewrite operation fails
     * @throws RepositoryException If there is a problem with the repository
     */
    public Node rewrite(Node root)
            throws DialogRewriteException, RepositoryException {
        logger.debug("Rewriting dialog tree rooted at {}", root.getPath());
        check(root);
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
        // collect paths of nodes that are final and can be skipped by the algorithm
        Set<String> finalPaths = new LinkedHashSet<String>();

        try {
            // do a pre-order tree traversal until we found no match
            do {
                foundMatch = false;
                TreeTraverser traverser = new TreeTraverser(startNode);
                Iterator<Node> iterator = traverser.iterator();
                logger.debug("Starting new pre-order tree traversal");
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

                    // we have previously found a match (and will start a new traversal from the start node)
                    // but we still need to finish this traversal in order not to change the order of nodes
                    if (foundMatch) {
                        continue;
                    }

                    // check if we should skip this node
                    if (finalPaths.contains(node.getPath())) {
                        continue;
                    }

                    // traverse all available rules
                    Set<Node> finalNodes = new LinkedHashSet<Node>();
                    for (DialogRewriteRule rule : rules) {
                        // check for a match
                        if (rule.matches(node)) {
                            logger.debug("Rule {} matched subtree rooted at {}", rule, node.getPath());
                            // the rule matched, rewrite the tree
                            Node result = rule.applyTo(node, finalNodes);
                            // set the start node in case it was rewritten
                            if (node.equals(startNode)) {
                                startNode = result;
                            }
                            addPaths(finalPaths, finalNodes);
                            foundMatch = true;
                            break;
                        }
                    }

                    // if we have found no match for this node, we can ignore it
                    // in subsequent traversals
                    if (!foundMatch) {
                        finalNodes.add(node);
                        addPaths(finalPaths, finalNodes);
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
        logger.debug("Rewrote dialog tree rooted at {} in {} ms", root.getPath(), tack - tick);

        return startNode;
    }

    private void addPaths(Set<String> paths, Set<Node> nodes)
            throws RepositoryException {
        for (Node node : nodes) {
            paths.add(node.getPath());
        }
    }

}
