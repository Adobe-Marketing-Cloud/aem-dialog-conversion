<%--

  render component.

  

--%><%
%><%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" %><%
%><%

    String suffix = slingRequest.getRequestPathInfo().getSuffix();

%><sling:forward path="<%= suffix %>" replaceSuffix="/libs/cq/ui/dialogupgrade/content/empty/jcr:content" resourceType="cq/gui/components/authoring/dialog" />