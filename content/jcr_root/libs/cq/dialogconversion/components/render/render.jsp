<%--

  Renders the Touch UI dialog specified by the suffix path.

--%><%
%><%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false"%><%
%><%

    String dialogPath = slingRequest.getRequestPathInfo().getSuffix();
    String emptyResourcePath = "/libs/cq/dialogconversion/content/empty/jcr:content";

%><sling:forward path="<%= dialogPath %>" replaceSuffix="<%= emptyResourcePath %>" resourceType="cq/gui/components/authoring/dialog" />