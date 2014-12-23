<%--

  Renders the Touch UI dialog specified by the suffix path.

--%><%
%><%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" import="com.adobe.cq.dialogupgrade.DialogUpgradeConstants"%><%
%><%

    String dialogPath = slingRequest.getRequestPathInfo().getSuffix();
    String emptyResourcePath = DialogUpgradeConstants.BASE_PATH + "/content/empty/jcr:content";

%><sling:forward path="<%= dialogPath %>" replaceSuffix="<%= emptyResourcePath %>" resourceType="cq/gui/components/authoring/dialog" />