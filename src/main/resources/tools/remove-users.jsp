<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ page import="org.jahia.modules.userremovaltool.RemovalUtility" %>
<%@ page import="java.util.List" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt_rt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<html>
<head>

</head>

<%
    String[] acesToRemove = request.getParameterValues("acesToRemove");
    RemovalUtility.removeNode(acesToRemove);
    pageContext.setAttribute("aces", RemovalUtility.getUsersFromAces());
    pageContext.setAttribute("members", RemovalUtility.getMembers());
%>

<body>
<c:if test="${not empty aces}">
    <h2>Aces that have non existing principals</h2>
    <form id="acesForm" action="?" method="post">
        <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
        <ul>
            <c:forEach var="user" items="${aces}">
                <li><input type="checkbox" name="acesToRemove" value="${user.path}">${user.name}${user.path}${user.type}</li>
            </c:forEach>
        </ul>
        <input type="submit" name="action" value="Remove selected aces"  title="Remove selected aces" />
    </form>

    <form id="acesNavNextForm" action="?" method="get">
        <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
        <input type="number" name="nextAce" id="nextAce" value="${1}" hidden/>
        <input type="submit" name="action" value="Next"  title="Next" />
    </form>
</c:if>

<c:if test="${not empty members}">
    <h2>Members with bad references</h2>
    <form id="acesForm" action="?" method="post">
        <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
        <ul>
            <c:forEach var="user" items="${members}">
                <li><input type="checkbox" name="membersToRemove" value="${user.path}">${user.name}${user.path}${user.type}</li>
            </c:forEach>
        </ul>
        <input type="submit" name="action" value="Remove selected members"  title="Remove selected members" />
    </form>

    <form id="membersNavNextForm" action="?" method="get">
        <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
        <input type="number" name="nextMember" id="nextMember" value="${1}" hidden/>
        <input type="submit" name="action" value="Next"  title="Next" />
    </form>
</c:if>
</body>
</html>
