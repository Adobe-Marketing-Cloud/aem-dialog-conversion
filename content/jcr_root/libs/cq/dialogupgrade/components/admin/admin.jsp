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
                                  javax.jcr.Node,
                                  javax.jcr.NodeIterator,
                                  javax.jcr.Session,
                                  javax.jcr.ValueFactory,
                                  javax.jcr.query.Query,
                                  javax.jcr.query.QueryManager,
                                  javax.jcr.query.QueryResult,
                                  javax.jcr.query.qom.Constraint,
                                  javax.jcr.query.qom.Literal,
                                  javax.jcr.query.qom.Ordering,
                                  javax.jcr.query.qom.PropertyValue,
                                  javax.jcr.query.qom.QueryObjectModelFactory,
                                  javax.jcr.query.qom.Selector,
                                  static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                                  com.adobe.cq.dialogupgrade.DialogUpgradeConstants" %><%
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
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            QueryObjectModelFactory qf = queryManager.getQOMFactory();
            ValueFactory vf = session.getValueFactory();

            /**
             * Using the query object model in order to prevent SQL injection in the ISDESCENDANTNODE constraint. The
             * query is equivalent to following JCR SQL:
             *
             * SELECT * FROM [cq:Dialog] AS d WHERE (ISDESCENDANTNODE('"+path+"') OR [jcr:path] = '"+path+"') AND NAME(d) = 'dialog' ORDER BY [jcr:path]
             */

            // node type selector
            Selector selector = qf.selector("cq:Dialog", "d");

            // ISDESCENDANTNODE constraint
            Constraint c1 = qf.descendantNode("d", path);

            // path equality constraint
            PropertyValue jcrPath = qf.propertyValue("d", "jcr:path");
            Literal pathLiteral = qf.literal(vf.createValue(path));
            Constraint c2 = qf.comparison(jcrPath, JCR_OPERATOR_EQUAL_TO, pathLiteral);

            // NAME constraint
            Literal dialogLiteral = qf.literal(vf.createValue("dialog"));
            Constraint c3 = qf.comparison(qf.nodeName("d"), JCR_OPERATOR_EQUAL_TO, dialogLiteral);

            // orderings
            Ordering[] orderings = new Ordering[]{ qf.ascending(jcrPath) };

            // execute query
            Constraint constraints = qf.and(qf.or(c1, c2), c3);
            Query query = qf.createQuery(selector, constraints, orderings, null);
            QueryResult result = query.execute();
            NodeIterator iterator = result.getNodes();

            // count number of results
            long nbResults = iterator.getSize();
            if (nbResults < 0) {
                nbResults = 0;
                while (iterator.hasNext()) {
                    iterator.nextNode();
                    nbResults++;
                }
                iterator = result.getNodes();
            }
    %>
            <script>

                $(document).ready(function () {
                    // prefill pathbrowser with value from the url
                    $(".js-coral-pathbrowser-input", $("#path").closest(".coral-Form-fieldwrapper")).val("<%= xssAPI.encodeForJSString(path) %>");
                });

            </script>

            <div id="info-text">
                Found <b><%= nbResults %></b> dialogs below <b><%= path %></b>
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
            while(iterator.hasNext()) {
                Node dialog = iterator.nextNode();
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
