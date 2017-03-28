<%--
  ADOBE CONFIDENTIAL
  __________________

   Copyright 2014 Adobe Systems Incorporated
   All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Adobe Systems Incorporated and its suppliers,
  if any.  The intellectual and technical concepts contained
  herein are proprietary to Adobe Systems Incorporated and its
  suppliers and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe Systems Incorporated.
--%><%
%><%@page session="false" import="com.day.cq.commons.Externalizer,
                                  org.apache.jackrabbit.util.ISO9075,
                                  javax.jcr.Node,
                                  javax.jcr.NodeIterator,
                                  javax.jcr.RepositoryException,
                                  javax.jcr.Session,
                                  javax.jcr.query.InvalidQueryException,
                                  javax.jcr.query.Query,
                                  javax.jcr.query.QueryManager,
                                  java.util.LinkedList, java.util.List" %><%
%><%@include file="/libs/granite/ui/global.jsp"%>

<div id="content">

    <sling:include path="showButton" resourceType="granite/ui/components/coral/foundation/button"/>

    <div class="left">
        <sling:include path="path" resourceType="granite/ui/components/foundation/form/pathbrowser"/>
    </div>

    <br /><br /><br />

    <%

        String path = request.getParameter("path");
        if (path != null && !path.isEmpty()) {
            Externalizer externalizer = resourceResolver.adaptTo(Externalizer.class);
            Session session = slingRequest.getResourceResolver().adaptTo(Session.class);
            List<Node> nodes = new LinkedList<Node>();

            // sanitize path
            path = path.trim();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            // first check if the supplied path is a dialog node itself
            try {
                if (session.nodeExists(path)) {
                    Node node = session.getNode(path);
                    if ("dialog".equals(node.getName()) && "cq:Dialog".equals(node.getPrimaryNodeType().getName())) {
                        nodes.add(node);
                    }
                }
            } catch (RepositoryException e) {
                // ignore
            }

            // the path does not point to a dialog node: we query for dialog nodes
            if (nodes.isEmpty()) {
                String encodedPath = "/".equals(path) ? "" : ISO9075.encodePath(path);
                if (encodedPath.length() > 1 && encodedPath.endsWith("/")) {
                    encodedPath = encodedPath.substring(0, encodedPath.length()-1);
                }
                String xpath = "/jcr:root" + encodedPath + "//element(dialog, cq:Dialog) order by @jcr:path";
                QueryManager queryManager = session.getWorkspace().getQueryManager();
                Query query = queryManager.createQuery(xpath, Query.XPATH);
                try {
                    NodeIterator iterator = query.execute().getNodes();
                    while (iterator.hasNext()) {
                        nodes.add(iterator.nextNode());
                    }
                } catch (InvalidQueryException e) {
                    // ignore
                }
            }
    %>
            <div class="js-cq-DialogConverter-infoText">
                Found <b><%= nodes.size() %></b> dialog<%= nodes.size() > 1 ? "s" : "" %> below <b><%= path %></b>
            </div>

            <br />

            <div class="js-cq-DialogConverter-dialogsContainer" data-search-path="<%= xssAPI.getValidHref(path) %>">
                <table is="coral-table" class="cq-DialogConverter-dialogs">
                    <thead is="coral-table-head">
                        <tr is="coral-table-row">
                            <th is="coral-table-headercell">
                                <coral-checkbox class="js-cq-DialogConverter-toggleDialogPaths" type="checkbox"/>
                            </th>
                            <th is="coral-table-headercell" class="cq-DialogConverter-Header--title"><%= i18n.get("Dialog (Classic UI)") %></th>
                            <th is="coral-table-headercell" class="cq-DialogConverter-cell--centered"><%= i18n.get("Links") %></th>
                            <th is="coral-table-headercell" class="cq-DialogConverter-cell--centered"><%= i18n.get("Dialog (Touch UI)") %></th>
                        </tr>
                    </thead>
                    <tbody is="coral-table-body">
        <%
            for (Node dialog : nodes) {
                Node parent = dialog.getParent();
                String href = externalizer.relativeLink(slingRequest, dialog.getPath()) + ".html";
                String crxHref = externalizer.relativeLink(slingRequest, "/crx/de/index") + ".jsp#" + dialog.getPath();
                String disabled = parent.hasNode("cq:dialog") ? "disabled=\"disabled\"" : "";
                %>
                        <tr is="coral-table-row">
                            <td is="coral-table-cell" colspan="2">
                                <label class="coral-Checkbox">
                                    <input class="coral-Checkbox-input js-cq-DialogConverter-path" type="checkbox" value="<%= dialog.getPath()%>" <%= disabled %>>
                                    <span class="coral-Checkbox-checkmark"></span>
                                    <span class="coral-Checkbox-description"><%= dialog.getPath()%></span>
                                </label>
                            </td>
                            <td is="coral-table-cell" class="cq-DialogConverter-cell--centered"><a href="<%= xssAPI.getValidHref(href) %>" target="_blank" class="coral-Link">show</a> / <a href="<%= xssAPI.getValidHref(crxHref) %>" x-cq-linkchecker="skip" target="_blank" class="coral-Link">crxde</a></td>
                <% if (parent.hasNode("cq:dialog")) {
                    Node touchDialog = parent.getNode("cq:dialog");
                    href = externalizer.relativeLink(slingRequest, "/libs/cq/dialogconversion/content/render") + ".html" + touchDialog.getPath();
                    crxHref = externalizer.relativeLink(slingRequest, "/crx/de/index") + ".jsp#" + touchDialog.getPath().replaceAll(":", "%3A");
                %>
                            <td is="coral-table-cell" class="cq-DialogConverter-cell--centered">
                                <coral-icon icon="check"></coral-icon>
                                <a href="<%= xssAPI.getValidHref(href) %>" target="_blank" class="coral-Link">show</a>&nbsp;/&nbsp;<a href="<%= xssAPI.getValidHref(crxHref) %>" x-cq-linkchecker="skip" target="_blank" class="coral-Link">crxde</a>
                            </td>
                <% } else { %>
                            <td is="coral-table-cell" class="cq-DialogConverter-cell--centered">-</td>
                <% } %>
                        </tr>
            <% } %>
                    </tbody>
                </table>
                <br />
                <sling:include path="convertButton" resourceType="granite/ui/components/coral/foundation/button"/>
            </div>

            <table is="coral-table" class="js-cq-DialogConverter-conversionResults cq-DialogConverter-conversionResults">
                <thead is="coral-table-head">
                    <tr is="coral-table-row">
                        <th is="coral-table-headercell" class="cq-DialogConverter-Header--title"><%= i18n.get("Dialog (Classic UI)") %></th>
                        <th is="coral-table-headercell"><%= i18n.get("Conversion to Touch UI") %></th>
                        <th is="coral-table-headercell"><%= i18n.get("Message") %></th>
                    </tr>
                </thead>
                <tbody is="coral-table-body">
                </tbody>
            </table>
    <% } %>

</div>
