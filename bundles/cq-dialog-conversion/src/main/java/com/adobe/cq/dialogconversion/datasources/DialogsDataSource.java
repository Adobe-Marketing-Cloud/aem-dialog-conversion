/*************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * __________________
 *
 *  Copyright 2017 Adobe Systems Incorporated
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
 *
 **************************************************************************/
package com.adobe.cq.dialogconversion.datasources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.ServletException;

import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.Externalizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Returns a list of dialogs found on the given path
 */
@SlingServlet(
        resourceTypes = "cq/dialogconversion/components/dialogs/datasource",
        methods = { "GET" })
public final class DialogsDataSource extends SlingSafeMethodsServlet {

    private final static Logger log = LoggerFactory.getLogger(DialogsDataSource.class);

    private static final String NN_CQ_DIALOG = "cq:dialog";
    private static final String DIALOG_CONVERSION_CONTENT_PATH = "/libs/cq/dialogconversion/content/render";
    private static final String CRX_LIGHT_PATH = "/crx/de/index";

    @Reference
    private ExpressionResolver expressionResolver;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        try {
            Resource resource = request.getResource();
            ResourceResolver resolver = request.getResourceResolver();
            ValueMap properties = resource.getValueMap();
            String itemResourceType = properties.get("itemResourceType", String.class);

            String path = properties.get("path", String.class);

            if (StringUtils.isEmpty(path)) {
                log.warn("Path unavailable");
                return;
            }

            path = expressionResolver.resolve(path, request.getLocale(), String.class, request);

            setDataSource(resource, path, resolver, request, itemResourceType);
        } catch (RepositoryException e) {
            log.warn("Unable to list classic dialogs", e.getMessage());
        }
    }

    private void setDataSource(Resource resource, String path, ResourceResolver resourceResolver, SlingHttpServletRequest request, String itemResourceType) throws RepositoryException {
        List<Resource> resources = new ArrayList<Resource>();

        if (StringUtils.isNotEmpty(path)) {
            Externalizer externalizer = resourceResolver.adaptTo(Externalizer.class);
            Session session = request.getResourceResolver().adaptTo(Session.class);
            List<Node> nodes = new LinkedList<Node>();

            // sanitize path
            path = path.trim();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            // First check if the supplied path is a dialog node itself
            if (session.nodeExists(path)) {
                Node node = session.getNode(path);
                if ("dialog".equals(node.getName()) && "cq:Dialog".equals(node.getPrimaryNodeType().getName())) {
                    nodes.add(node);
                }
            }

            // If the path does not point to a dialog node: we query for dialog nodes
            if (nodes.isEmpty()) {
                String encodedPath = "/".equals(path) ? "" : ISO9075.encodePath(path);
                if (encodedPath.length() > 1 && encodedPath.endsWith("/")) {
                    encodedPath = encodedPath.substring(0, encodedPath.length() - 1);
                }
                String xpath = "/jcr:root" + encodedPath + "//element(dialog, cq:Dialog) order by @jcr:path";
                QueryManager queryManager = session.getWorkspace().getQueryManager();
                Query query = queryManager.createQuery(xpath, Query.XPATH);

                NodeIterator iterator = query.execute().getNodes();
                while (iterator.hasNext()) {
                    nodes.add(iterator.nextNode());
                }
            }

            for (int index = 0; index < nodes.size(); index++) {
                Node dialog = nodes.get(index);

                if (dialog == null) {
                    continue;
                }

                Node parent = dialog.getParent();

                if (parent == null) {
                    continue;
                }

                String dialogPath = dialog.getPath();
                String href = externalizer.relativeLink(request, dialogPath) + ".html";
                String crxHref = externalizer.relativeLink(request, CRX_LIGHT_PATH) + ".jsp#" + dialogPath;
                boolean touchDialog = parent.hasNode(NN_CQ_DIALOG);

                Map<String, Object> map = new HashMap<String, Object>();
                map.put("dialogPath", dialogPath);
                map.put("href", href);
                map.put("touchDialog", touchDialog);
                map.put("crxHref", crxHref);

                if (touchDialog) {
                    Node touchDialogNode = parent.getNode(NN_CQ_DIALOG);
                    String touchHref = externalizer.relativeLink(request, DIALOG_CONVERSION_CONTENT_PATH) + ".html" + touchDialogNode.getPath();
                    String touchCrxHref = externalizer.relativeLink(request, CRX_LIGHT_PATH) + ".jsp#" + touchDialogNode.getPath().replaceAll(":", "%3A");

                    map.put("touchHref", touchHref);
                    map.put("touchCrxHref", touchCrxHref);
                }

                resources.add(new ValueMapResource(resourceResolver, resource.getPath() + "/dialog_" + index, itemResourceType, new ValueMapDecorator(map)));
            }
        }

        DataSource ds = new SimpleDataSource(resources.iterator());

        request.setAttribute(DataSource.class.getName(), ds);
    }
}
