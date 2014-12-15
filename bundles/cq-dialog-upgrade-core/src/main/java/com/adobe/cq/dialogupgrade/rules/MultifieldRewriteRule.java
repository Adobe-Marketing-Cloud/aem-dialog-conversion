package com.adobe.cq.dialogupgrade.rules;

import com.adobe.cq.dialogupgrade.api.DialogRewriteRule;
import com.adobe.cq.dialogupgrade.treerewriter.RewriteException;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Set;

import static com.adobe.cq.dialogupgrade.DialogUpgradeUtils.hasXtype;
import static com.adobe.cq.dialogupgrade.treerewriter.TreeRewriterUtils.copyProperty;
import static com.adobe.cq.dialogupgrade.treerewriter.TreeRewriterUtils.renameToTemp;

/**
 * Rewrites widgets of xtype "multifield". The "fieldConfig" subnode (if existing) is renamed to "field" and
 * will be handled by subsequent passes of the algorithm.
 */
@Component
@Service
@Properties({
        @Property(name="service.ranking", intValue = 11)
})
public class MultifieldRewriteRule extends AbstractRewriteRule {

    private static final String XTYPE = "multifield";
    private static final String GRANITEUI_MULTIFIELD_RT = "granite/ui/components/foundation/form/multifield";
    private static final String GRANITEUI_TEXTFIELD_RT = "granite/ui/components/foundation/form/textfield";

    public boolean matches(Node root)
            throws RepositoryException {
        return hasXtype(root, XTYPE);
    }

    public Node applyTo(Node root, Set<Node> finalNodes)
            throws RewriteException, RepositoryException {
        Node parent = root.getParent();
        String name = root.getName();
        renameToTemp(root);

        // add node for multifield
        Node newRoot = parent.addNode(name, "nt:unstructured");
        finalNodes.add(newRoot);
        newRoot.setProperty("sling:resourceType", GRANITEUI_MULTIFIELD_RT);
        // set properties
        copyProperty(root, "fieldLabel", newRoot, "fieldLabel");
        copyProperty(root, "fieldDescription", newRoot, "fieldDescription");

        Node field;
        if (root.hasNode("fieldConfig")) {
            field = JcrUtil.copy(root.getNode("fieldConfig"), newRoot, "field");
            field.setPrimaryType("cq:Widget");
            copyProperty(root, "name", field, "name");
        } else {
            field = newRoot.addNode("field", "nt:unstructured");
            finalNodes.add(field);
            field.setProperty("sling:resourceType", GRANITEUI_TEXTFIELD_RT);
        }

        // remove old root and return new root
        root.remove();
        return newRoot;
    }

}
