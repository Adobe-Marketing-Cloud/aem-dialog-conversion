package com.adobe.cq.dialogupgrade.rules;

import com.adobe.cq.dialogupgrade.api.DialogRewriteRule;
import com.adobe.cq.dialogupgrade.treerewriter.RewriteException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.adobe.cq.dialogupgrade.DialogUpgradeUtils.hasXtype;
import static com.adobe.cq.dialogupgrade.treerewriter.TreeRewriterUtils.copyProperty;
import static com.adobe.cq.dialogupgrade.treerewriter.TreeRewriterUtils.renameToTemp;

@Component
@Service
@Properties({
        @Property(name="service.ranking", intValue = 11)
})
public class MultifieldRewriteRule implements DialogRewriteRule {

    private static final String XTYPE = "multifield";
    private static final String GRANITEUI_CHECKBOX_RT = "granite/ui/components/foundation/form/multifield";

    private static final Map<String,String> FIELDCONFIG_MAPPINGS = new HashMap<String,String>();
    static {
        FIELDCONFIG_MAPPINGS.put("textfield", "granite/ui/components/foundation/form/textfield");
        FIELDCONFIG_MAPPINGS.put("textarea", "granite/ui/components/foundation/form/textarea");
        FIELDCONFIG_MAPPINGS.put("pathfield", "granite/ui/components/foundation/form/pathbrowser");
        // todo: selection (type = combobox or select)
    }

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
        newRoot.setProperty("sling:resourceType", GRANITEUI_CHECKBOX_RT);
        // set properties
        copyProperty(root, "fieldLabel", newRoot, "fieldLabel");
        copyProperty(root, "fieldDescription", newRoot, "fieldDescription");

        // add 'field' subnode
        Node field = newRoot.addNode("field", "nt:unstructured");
        finalNodes.add(field);
        // use textfield as the default resource type
        String resourceType = FIELDCONFIG_MAPPINGS.get("textfield");
        if (root.hasProperty("fieldConfig/xtype")) {
            String xtype = root.getProperty("fieldConfig/xtype").getString();
            if (FIELDCONFIG_MAPPINGS.containsKey(xtype)) {
                resourceType = FIELDCONFIG_MAPPINGS.get(xtype);
            }
        }
        field.setProperty("sling:resourceType", resourceType);
        copyProperty(root, "name", field, "name");

        // remove old root and return new root
        root.remove();
        return newRoot;
    }

}
