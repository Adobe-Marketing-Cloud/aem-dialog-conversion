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
%><%@page session="false" import="javax.jcr.*,
                                  javax.jcr.query.*,
                                  java.util.*,
                                  com.day.cq.commons.Externalizer" %>
<%@include file="/libs/foundation/global.jsp"%>

<% if (request.getParameter("path") != null) { %>
<script>

    $(document).ready(function () {
        // prefill pathbrowser with value from the url
        $(".js-coral-pathbrowser-input", $("#path").closest(".coral-Form-fieldwrapper")).val("<%= request.getParameter("path") %>");
    });

</script>
<% } %>

<div id="content">

    <cq:include path="/libs/cq/ui/dialogupgrade/content/console/components/showButton" resourceType="granite/ui/components/foundation/button"/>

    <div class="left">
        <cq:include path="/libs/cq/ui/dialogupgrade/content/console/components/path" resourceType="granite/ui/components/foundation/form/pathbrowser"/>
    </div>

    <br /><br /><br />

    <%

        if (request.getParameter("path") != null && !request.getParameter("path").isEmpty()) {
            Externalizer externalizer = resourceResolver.adaptTo(Externalizer.class);
            String path = request.getParameter("path");
            String sql = "SELECT * FROM [cq:Dialog] AS d WHERE isdescendantnode('"+path+"') AND NAME(d) = 'dialog'";
            List<String> paths = new LinkedList<String>();
    %>
            <table class="coral-Table coral-Table--hover">
                <thead>
                    <tr class="coral-Table-row">
                        <th class="coral-Table-headerCell">Dialog (Classic UI)</th>
                        <th class="coral-Table-headerCell centered">Links</th>
                        <th class="coral-Table-headerCell centered">Dialog (Touch UI)</th>
                    </tr>
                </thead>
                <tbody>
        <%
            final QueryManager qm = slingRequest.getResourceResolver().adaptTo(Session.class).getWorkspace().getQueryManager();
            final Query query = qm.createQuery(sql, Query.JCR_SQL2);
            final NodeIterator results = query.execute().getNodes();
            while(results.hasNext()) {
                Node dialog = results.nextNode();
                paths.add(dialog.getPath());
                Node parent = dialog.getParent();
                %>
                <tr class="coral-Table-row">
                    <td class="coral-Table-cell" class="path"><%= dialog.getPath()%></td>
                    <td class="coral-Table-cell centered"><a href="<%= externalizer.authorLink(resourceResolver, dialog.getPath()) %>.html" target="_blank" class="coral-Link">show</a> / <a href="<%= externalizer.authorLink(resourceResolver, "/") %>crx/de/index.jsp#<%= dialog.getPath() %>" x-cq-linkchecker="skip" target="_blank" class="coral-Link">crxde</a></td>
                    <% if (parent.hasNode("cq:dialog")) {
                        Node touchDialog = parent.getNode("cq:dialog");
                    %>
                        <td class="coral-Table-cell centered"><i class="coral-Icon coral-Icon--check"></i> &nbsp;&nbsp; <a href="<%= externalizer.authorLink(resourceResolver, touchDialog.getPath()) %>.html" target="_blank" class="coral-Link">show</a> / <a href="<%= externalizer.authorLink(resourceResolver, "/") %>crx/de/index.jsp#<%= touchDialog.getPath() %>" x-cq-linkchecker="skip" target="_blank" class="coral-Link">crxde</a></td>
                    <% } else { %>
                        <td class="coral-Table-cell centered">-</td>
                    <% } %>
                </tr>
            <% } %>
                </tbody>
            </table>

            <br />

            <cq:include path="/libs/cq/ui/dialogupgrade/content/console/components/upgradeButton" resourceType="granite/ui/components/foundation/button"/>
    <% } %>

</div>
