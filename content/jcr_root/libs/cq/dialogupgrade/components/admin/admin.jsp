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
%><%@page session="false" import="com.adobe.cq.dialogupgrade.DialogUpgradeConstants,
                                  com.day.cq.commons.Externalizer,
                                  org.apache.jackrabbit.util.ISO9075,
                                  javax.jcr.Node,
                                  javax.jcr.NodeIterator,
                                  javax.jcr.Session,
                                  javax.jcr.query.Query,
                                  javax.jcr.query.QueryManager,
                                  java.util.LinkedList,
                                  java.util.List" %><%
%><%@include file="/libs/foundation/global.jsp"%>

<div id="content">

    <cq:include path="showButton" resourceType="granite/ui/components/foundation/button"/>

    <div class="left">
        <cq:include path="path" resourceType="granite/ui/components/foundation/form/pathbrowser"/>
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
            if (session.nodeExists(path)) {
                Node node = session.getNode(path);
                if ("dialog".equals(node.getName()) && "cq:Dialog".equals(node.getPrimaryNodeType().getName())) {
                    nodes.add(node);
                }
            }

            // the path does not point to a dialog node: we query for dialog nodes
            if (nodes.isEmpty()) {
                // encode path and strip leading slash
                String encodedPath = ISO9075.encodePath(path).substring(1);
                String xpath = "/jcr:root/" + encodedPath + "//element(dialog, cq:Dialog) order by @jcr:path";
                QueryManager queryManager = session.getWorkspace().getQueryManager();
                Query query = queryManager.createQuery(xpath, Query.XPATH);

                NodeIterator iterator = query.execute().getNodes();
                while (iterator.hasNext()) {
                    nodes.add(iterator.nextNode());
                }
            }
    %>
            <script>

                $(document).ready(function () {
                    // prefill pathbrowser with value from the url
                    $(".js-coral-pathbrowser-input", $("#path").closest(".coral-Form-fieldwrapper")).val("<%= xssAPI.encodeForJSString(path) %>");
                });

            </script>

            <div id="info-text">
                Found <b><%= nodes.size() %></b> dialogs below <b><%= path %></b>
            </div>

            <br />

            <div id="dialogs">
                <table class="coral-Table coral-Table--hover">
                    <thead>
                        <tr class="coral-Table-row">
                            <th class="coral-Table-headerCell dialog-cell">Dialog (Classic UI)</th>
                            <th class="coral-Table-headerCell centered">Links</th>
                            <th class="coral-Table-headerCell centered">Dialog (Touch UI)</th>
                        </tr>
                    </thead>
                    <tbody>
        <%
            String renderPath = DialogUpgradeConstants.BASE_PATH + "/content/render";
            for (Node dialog : nodes) {
                Node parent = dialog.getParent();
                String href = externalizer.authorLink(resourceResolver, dialog.getPath()) + ".html";
                String crxHref = externalizer.authorLink(resourceResolver, "/") + "crx/de/index.jsp#" + dialog.getPath();
                String disabled = parent.hasNode("cq:dialog") ? "disabled=\"disabled\"" : "";
                %>
                        <tr class="coral-Table-row">
                            <td class="coral-Table-cell dialog-cell">
                                <label class="coral-Checkbox">
                                    <input class="coral-Checkbox-input path" type="checkbox" value="<%= dialog.getPath()%>" <%= disabled %>>
                                    <span class="coral-Checkbox-checkmark"></span>
                                    <span class="coral-Checkbox-description"><%= dialog.getPath()%></span>
                                </label>
                            </td>
                            <td class="coral-Table-cell centered"><a href="<%= xssAPI.getValidHref(href) %>" target="_blank" class="coral-Link">show</a> / <a href="<%= xssAPI.getValidHref(crxHref) %>" x-cq-linkchecker="skip" target="_blank" class="coral-Link">crxde</a></td>
                <% if (parent.hasNode("cq:dialog")) {
                    Node touchDialog = parent.getNode("cq:dialog");
                    href = externalizer.authorLink(resourceResolver, renderPath) + ".html" + touchDialog.getPath();
                    crxHref = externalizer.authorLink(resourceResolver, "/") + "crx/de/index.jsp#" + touchDialog.getPath().replaceAll(":", "%3A");
                %>
                            <td class="coral-Table-cell centered"><i class="coral-Icon coral-Icon--check"></i><a href="<%= xssAPI.getValidHref(href) %>" target="_blank" class="coral-Link">show</a> / <a href="<%= xssAPI.getValidHref(crxHref) %>" x-cq-linkchecker="skip" target="_blank" class="coral-Link">crxde</a></td>
                <% } else { %>
                            <td class="coral-Table-cell centered">-</td>
                <% } %>
                        </tr>
            <% } %>
                    </tbody>
                </table>
                <br />
                <cq:include path="upgradeButton" resourceType="granite/ui/components/foundation/button"/>
            </div>

            <table class="coral-Table coral-Table--hover" id="upgrade-results">
                <thead>
                    <tr class="coral-Table-row">
                        <th class="coral-Table-headerCell">Dialog (Classic UI)</th>
                        <th class="coral-Table-headerCell centered">Upgrade to Touch UI</th>
                        <th class="coral-Table-headerCell">Message</th>
                    </tr>
                </thead>
                <tbody>
                </tbody>
            </table>
    <% } %>

</div>
