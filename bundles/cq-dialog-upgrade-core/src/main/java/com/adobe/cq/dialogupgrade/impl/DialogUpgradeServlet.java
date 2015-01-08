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

package com.adobe.cq.dialogupgrade.impl;

import com.adobe.cq.dialogupgrade.DialogRewriteRule;
import com.adobe.cq.dialogupgrade.treerewriter.RewriteException;
import com.adobe.cq.dialogupgrade.treerewriter.RewriteRule;
import com.adobe.cq.dialogupgrade.treerewriter.RewriteRulesFactory;
import com.adobe.cq.dialogupgrade.treerewriter.TreeRewriter;
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
import org.apache.sling.commons.json.JSONObject;
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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static com.adobe.cq.dialogupgrade.DialogUpgradeConstants.BASE_PATH;
import static com.adobe.cq.dialogupgrade.DialogUpgradeConstants.RULES_SEARCH_PATH;

@SlingServlet(
        methods = "POST",
        paths = BASE_PATH + "/content/upgrade",
        extensions = "json"
)
public class DialogUpgradeServlet extends SlingAllMethodsServlet {

    public static final String PARAM_PATHS = "paths";
    private static final String KEY_RESULT = "result";
    private static final String KEY_PATH = "path";
    private static final String KEY_MESSAGE = "message";

    private static enum UpgradeResult {
        PATH_NOT_FOUND,
        NOT_A_DIALOG,
        ALREADY_UPGRADED,
        SUCCESS,
        ERROR
    }

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
    private List<ServiceReference> rulesReferences = Collections.synchronizedList(new LinkedList<ServiceReference>());

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
    }

    @SuppressWarnings("unused")
    protected void unbindRule(ServiceReference reference) {
        rulesReferences.remove(reference);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        // collect rules
        List<RewriteRule> rules;
        try {
            rules = collectRules(request.getResourceResolver());
        } catch (RepositoryException e) {
            throw new ServletException("Caught exception while collecting rewrite rules", e);
        }

        try {
            // validate 'paths' parameter
            RequestParameter[] paths = request.getRequestParameters(PARAM_PATHS);
            if (paths == null) {
                logger.warn("Missing parameter '" + PARAM_PATHS + "'");
                response.setContentType("text/html");
                response.getWriter().println("Missing parameter '" + PARAM_PATHS + "'");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            long tick = System.currentTimeMillis();
            Session session = request.getResourceResolver().adaptTo(Session.class);
            TreeRewriter rewriter = new TreeRewriter(rules);
            String path = "";
            JSONObject results = new JSONObject();
            logger.debug("Upgrading {} dialogs", paths.length);
            // iterate over all paths
            for (RequestParameter parameter : paths) {
                path = parameter.getString();
                JSONObject result = new JSONObject();
                results.put(path, result);

                // path doesn't exist
                if (!session.nodeExists(path)) {
                    result.put(KEY_RESULT, UpgradeResult.PATH_NOT_FOUND);
                    logger.debug("Path {} doesn't exist", path);
                    continue;
                }

                Node dialog = session.getNode(path);
                // path does not point to a dialog
                if (!"cq:Dialog".equals(dialog.getPrimaryNodeType().getName())) {
                    result.put(KEY_RESULT, UpgradeResult.NOT_A_DIALOG);
                    logger.debug("{} is not a Classic UI dialog", path);
                    continue;
                }

                // Touch UI dialog already exists
                if (dialog.getParent().hasNode("cq:dialog")) {
                    result.put(KEY_RESULT, UpgradeResult.ALREADY_UPGRADED);
                    logger.debug("Dialog {} already has a Touch UI counterpart", path);
                    continue;
                }

                // do the upgrade
                try {
                    Node upgradedDialog = rewriter.rewrite(dialog);
                    result.put(KEY_RESULT, UpgradeResult.SUCCESS);
                    result.put(KEY_PATH, upgradedDialog.getPath());
                    logger.debug("Successfully upgraded dialog {} to {}", path,  upgradedDialog.getPath());
                } catch (RewriteException e) {
                    result.put(KEY_RESULT, UpgradeResult.ERROR);
                    result.put(KEY_MESSAGE, e.getMessage());
                    logger.warn("Upgrading dialog {} failed", path, e);
                }
            }
            response.setContentType("application/json");
            response.getWriter().write(results.toString());

            long tack = System.currentTimeMillis();
            logger.debug("Rewrote {} dialogs in {} ms", paths.length, tack - tick);
        } catch (Exception e) {
            throw new ServletException("Caught exception while rewriting dialogs", e);
        }
    }

    private List<RewriteRule> collectRules(ResourceResolver resolver)
            throws RepositoryException {
        final List<RewriteRule> rules = new LinkedList<RewriteRule>();

        // rules provided as OSGi services
        if (context != null) {
            synchronized (rulesReferences) {
                for (ServiceReference reference : rulesReferences) {
                    rules.add((DialogRewriteRule) context.getBundleContext().getService(reference));
                }
            }
        }
        int nb = rules.size();

        // node-based rules
        Resource resource = resolver.getResource(RULES_SEARCH_PATH);
        if (resource != null) {
            rules.addAll(RewriteRulesFactory.createRules(resource.adaptTo(Node.class)));
        }

        // sort rules according to their ranking
        Collections.sort(rules, new Comparator<RewriteRule>() {

            public int compare(RewriteRule rule1, RewriteRule rule2) {
                int ranking1 = rule1.getRanking();
                ranking1 = ranking1 < 0 ? Integer.MAX_VALUE : ranking1;
                int ranking2 = rule2.getRanking();
                ranking2 = ranking2 < 0 ? Integer.MAX_VALUE : ranking2;
                return Double.compare(ranking1, ranking2);
            }

        });

        logger.debug("Found {} rules ({} Java-based, {} node-based)", nb, rules.size() - nb);
        return rules;
    }

}
