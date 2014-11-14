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

package com.adobe.cq.dialogupgrade;

import com.adobe.cq.dialogupgrade.api.DialogRewriteRule;
import com.adobe.cq.dialogupgrade.treerewriter.RewriteException;
import com.adobe.cq.dialogupgrade.treerewriter.rules.RewriteRule;
import com.adobe.cq.dialogupgrade.treerewriter.rules.RewriteRulesFactory;
import com.adobe.cq.dialogupgrade.treerewriter.TreeRewriter;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@SlingServlet(
        methods = {"GET","POST"},
        paths = "/libs/cq/ui/dialogupgrade/content/upgrade",
        extensions = "json"
)
public class DialogUpgradeServlet extends SlingAllMethodsServlet {

    private static final String RULES_PATH = "cq/ui/dialogupgrade/rules";

    private Logger logger = LoggerFactory.getLogger(DialogUpgradeServlet.class);

    /**
     * Keeps track of OSGi services implementing dialog rewrite rules
     */
    @Reference(
            referenceInterface = DialogRewriteRule.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            bind = "bindRule",
            unbind = "unbindRule"
    )
    private List<ServiceReference> rulesReferences = new LinkedList<ServiceReference>();

    /**
     * Keeps track whether or not the references are currently in sorted order
     */
    private boolean referencesSorted;

    /**
     * Used to retrieve the service from the service references
     */
    private ComponentContext context;

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
    }

    @SuppressWarnings("unused")
    protected void bindRule(ServiceReference reference) {
        rulesReferences.add(reference);
        referencesSorted = false;
    }

    @SuppressWarnings("unused")
    protected void unbindRule(ServiceReference reference) {
        rulesReferences.remove(reference);
        referencesSorted = false;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        logger.info("get");
        // if the references were updated
        if (!referencesSorted) {
            // sort them, in order to respect the service ranking
            Collections.sort(rulesReferences);
            referencesSorted = true;
        }

        // collect rules
        List<RewriteRule> rules;
        try {
            rules = collectRules(request.getResourceResolver());
        } catch (RepositoryException e) {
            throw new ServletException("Caught exception while collecting rewrite rules", e);
        }

        try {
            RequestParameter[] paths = request.getRequestParameters("paths");
            if (paths == null) {
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            TreeRewriter rewriter = new TreeRewriter(rules);
            Session session = request.getResourceResolver().adaptTo(Session.class);
            String path = "";
            for (RequestParameter parameter : paths) {
                path = parameter.getString();
                // todo: validate path
                Node dialog = session.getNode(path);
                Node parent = dialog.getParent();
                try {
                    rewriter.rewrite(dialog);
                } catch (RewriteException e) {
                    logger.warn("Upgrading dialog {} failed", path, e);
                }
            }
        } catch (Exception e) {
            throw new ServletException("Caught exception while rewriting dialog", e);
        }
    }

    private List<RewriteRule> collectRules(ResourceResolver resolver)
            throws RepositoryException {
        List<RewriteRule> rules = new LinkedList<RewriteRule>();

        // rules provided as OSGi services
        if (context != null) {
            for (ServiceReference reference : rulesReferences) {
                rules.add((DialogRewriteRule) context.getBundleContext().getService(reference));
            }
        }

        // node-based rules
        Resource resource = resolver.getResource(RULES_PATH);
        if (resource != null) {
            rules.addAll(RewriteRulesFactory.createRules(resource.adaptTo(Node.class)));
        }

        return rules;
    }

}
