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
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.jackrabbit.commons.flat.TreeTraverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.adobe.cq.dialogupgrade.treerewriter.TreeRewriterUtils.hasPrimaryType;
import static com.adobe.cq.dialogupgrade.treerewriter.TreeRewriterUtils.renameToTemp;

/**
 * A rule that rewrites a tree based on a given node structure. The node structure
 * has the following form:
 *
 * <pre>
 * rule
 *   - jcr:primaryType = nt:unstructured
 *   + patterns
 *     - jcr:primaryType = nt:unstructured
 *     + foo
 *       - ...
 *       + ...
 *     + foo1
 *       - ...
 *       + ...
 *   + replacement
 *     + bar
 *       - ...
 *       + ...
 * </pre>
 *
 * If a tree matches any of the defined patterns (i.e. child nodes of the pattern nodes), then it will
 * be replaced by the tree specified below the replacement node. A pattern is an arbitrary tree containing
 * nodes and properties. It matches a tree, if the tree contains the the same nodes as the pattern, and all
 * properties defined in the pattern match the properties on the tree.
 *
 * The replacement tree can define mapped properties of type <code>string</code> that will inherit the value of a
 * property in the original tree. For instance, the following property <code>one</code> will receive the value
 * of the property <code>three</code> of the subnode <code>two</code> of the matched tree.
 *
 * <pre>
 * ...
 *   + replacement
 *     + bar
 *       - one = ${./two/three}
 * </pre>
 *
 * todo: multivalue, special properties
 *
 */
public class NodeBasedRewriteRule implements RewriteRule {

    // pattern that matches the regex for mapped properties: ${<path>}
    private static final Pattern MAPPED_PATTERN = Pattern.compile("^\\$\\{(.*)\\}$");

    private Logger logger = LoggerFactory.getLogger(NodeBasedRewriteRule.class);

    private Node ruleNode;

    public NodeBasedRewriteRule(Node ruleNode) {
        this.ruleNode = ruleNode;
    }

    public boolean matches(Node root)
            throws RepositoryException {
        if (!ruleNode.hasNode("patterns")) {
            // the 'patterns' subnode does not exist
            return false;
        }
        Node patterns = ruleNode.getNode("patterns");
        if (!patterns.hasNodes()) {
            // no patterns are defined
            return false;
        }
        // iterate over all defined patterns
        NodeIterator iterator = patterns.getNodes();
        while (iterator.hasNext()) {
            Node pattern = iterator.nextNode();
            if (matches(root, pattern)) {
                return true;
            }
        }
        // no pattern matched
        return false;
    }

    private boolean matches(Node root, Node pattern)
            throws RepositoryException {
        // check if the primary types match
        if (!hasPrimaryType(root, pattern.getPrimaryNodeType().getName())) {
            return false;
        }

        // check that all properties of the pattern match
        PropertyIterator propertyIterator = pattern.getProperties();
        while (propertyIterator.hasNext()) {
            Property property = propertyIterator.nextProperty();
            if (property.getDefinition().isProtected()) {
                // skip protected properties
                continue;
            }
            if (!root.hasProperty(property.getName())) {
                // property present on pattern does not exist in tree
                return false;
            }
            if (!root.getProperty(property.getName()).getValue().equals(property.getValue())) {
                // property values on pattern and tree differ
                return false;
            }
        }

        // check that the tree contains all children defined in the pattern
        NodeIterator nodeIterator = pattern.getNodes();
        while (nodeIterator.hasNext()) {
            Node child = nodeIterator.nextNode();
            if (!root.hasNode(child.getName())) {
                // this child is not present in subject tree
                return false;
            }
        }

        // check child nodes recursively
        nodeIterator = pattern.getNodes();
        while (nodeIterator.hasNext()) {
            Node child = nodeIterator.nextNode();
            return matches(root.getNode(child.getName()), child);
        }

        // base case (leaf node)
        return true;
    }

    public Node applyTo(Node root, Set<Node> finalNodes)
            throws RewriteException, RepositoryException {
        // check if the 'replacement' node exists
        if (!ruleNode.hasNode("replacement")) {
            throw new RewriteException("The rule does not define a replacement node");
        }

        // if the replacement node has no children, we replace the tree by the empty tree,
        // i.e. we remove the original tree
        Node replacement = ruleNode.getNode("replacement");
        if (!replacement.hasNodes()) {
            root.remove();
            return null;
        }

        // true if the replacement tree is final and all its nodes are excluded from
        // further processing by the algorithm
        boolean treeIsFinal = false;
        if (replacement.hasProperty("cq:rewriteIsFinal")) {
            treeIsFinal = replacement.getProperty("cq:rewriteIsFinal").getBoolean();
        }

        /**
         * Approach:
         * - we move (rename) the tree to be rewritten to a temporary name
         * - we copy the replacement tree to be a new child of the original tree's parent
         * - we process the copied replacement tree (mapped properties, children etc)
         * - at the end, we remove the original tree
         */

        // move (rename) original tree
        Node parent = root.getParent();
        String rootName = root.getName();
        renameToTemp(root);

        // copy replacement to original tree under original name
        replacement = replacement.getNodes().nextNode();
        Node copy = JcrUtil.copy(replacement, parent, rootName);

        // collect mappings: (node in original tree) -> (node in replacement tree)
        Map<String, String> mappings = new HashMap<String, String>();
        // traverse nodes of newly copied replacement tree
        TreeTraverser traverser = new TreeTraverser(copy);
        Iterator<Node> nodeIterator = traverser.iterator();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.next();
            // iterate over all properties
            PropertyIterator propertyIterator = node.getProperties();
            while (propertyIterator.hasNext()) {
                Property property = propertyIterator.nextProperty();
                // skip protected properties
                if (property.getDefinition().isProtected()) {
                    continue;
                }
                // add mapping to collection
                if ("cq:mapsTo".equals(property.getName())) {
                    mappings.put(property.getString(), node.getPath());
                    // remove property, as we don't want it to be part of the result
                    property.remove();
                    continue;
                }
                // add single node to final nodes
                if ("cq:rewriteIsFinal".equals(property.getName())) {
                    if (!treeIsFinal) {
                        finalNodes.add(node);
                    }
                    property.remove();
                    continue;
                }
                // set value from original tree in case this is a mapped property
                mapProperty(root, property);
            }
        }

        // copy children from original tree to replacement tree according to the mappings found
        Session session = root.getSession();
        for (Map.Entry<String, String> mapping : mappings.entrySet()) {
            if (!root.hasNode(mapping.getKey())) {
                // the node specified in the mapping does not exist in the original tree
                continue;
            }
            Node source = root.getNode(mapping.getKey());
            Node destination = session.getNode(mapping.getValue());
            NodeIterator iterator = source.getNodes();
            // copy over the source's children to the destination
            while (iterator.hasNext()) {
                Node child = iterator.nextNode();
                JcrUtil.copy(child, destination, child.getName());
            }
        }

        // we add the complete subtree to the final nodes
        if (treeIsFinal) {
            traverser = new TreeTraverser(copy);
            nodeIterator = traverser.iterator();
            while (nodeIterator.hasNext()) {
                finalNodes.add(nodeIterator.next());
            }
        }

        // remove original tree and return rewritten tree
        root.remove();
        return copy;
    }

    /**
     * Replaces the value of a mapped property with a value from the original tree.
     *
     * @param root the root node of the original tree
     * @param property the (potentially) mapped property in the replacement copy tree
     */
    private void mapProperty(Node root, Property property)
            throws RepositoryException {
        if (property.getType() != PropertyType.STRING) {
            // a mapped property must be of type string
            return;
        }

        // array containing the expressions: ${<path>}
        Value[] values;
        if (property.isMultiple()) {
            values = property.getValues();
        } else {
            values = new Value[1];
            values[0] = property.getValue();
        }

        boolean deleteProperty = false;
        for (Value value : values) {
            Matcher matcher = MAPPED_PATTERN.matcher(value.getString());
            if (matcher.matches()) {
                // this is a mapped property, we will delete it if the mapped destination
                // property doesn't exist
                deleteProperty = true;
                String path = matcher.group(1);
                if (root.hasProperty(path)) {
                    // replace property by mapped value in the original tree
                    Property originalProperty = root.getProperty(path);
                    String name = property.getName();
                    Node parent = property.getParent();
                    property.remove();
                    JcrUtil.copy(originalProperty, parent, name);
                    // the mapping was successful
                    deleteProperty = false;
                    break;
                }
            }
        }
        if (deleteProperty) {
            // mapped destination does not exist, we don't include the property in replacement tree
            property.remove();
        }
    }

}
