<%@ page language="java" contentType="text/html;charset=UTF-8"
%><?xml version="1.0" encoding="UTF-8" ?>
<%@ page import="org.jahia.modules.usercleanuptool.RemovalUtility" %>
<%@ page import="org.jahia.services.content.JCRStoreProvider" %>
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

<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <style>
        .navButtons {
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            width: 200px;
            margin-top: 10px;
        }

        .entryList {
            list-style: none;
        }

        .entryList li {
            display: flex;
            flex-direction: row;
            padding: 0 0 10px;
        }
        .info {
            font-size: large;
            color: darkblue;
            background-color: lightblue;
            padding: 10px;
            width: 700px;
        }
        .warning {
            font-size: large;
            color: red;
            background-color: yellow;
            padding: 10px;
            width: 700px;
        }      
    </style>
    <script type="text/javascript">
        function selectAll(e) {
            var checked = e.target.checked;
            var inputs = e.target.parentNode.parentNode.querySelectorAll("input");
            for (var i = 0; i < inputs.length; i++) {
                inputs[i].checked = checked;
            }
        }
    </script>
</head>

<c:set var="nextAce" value="${not empty param.nextAce ? param.nextAce : 0}"/>
<c:set var="nextMember" value="${not empty param.nextMember ? param.nextMember : 0}"/>

<%

    String[] acesToRemove = request.getParameterValues("acesToRemove");
    String[] membersToRemove = request.getParameterValues("membersToRemove");

    if (acesToRemove != null && acesToRemove.length > 0) {
        RemovalUtility.removeNode(acesToRemove);
    }

    if (membersToRemove != null && membersToRemove.length > 0) {
        RemovalUtility.removeNode(membersToRemove);
    }

    String nextAce = request.getParameter("nextAce");
    String nextMember = request.getParameter("nextMember");

    pageContext.setAttribute("aces", RemovalUtility.getUsersFromAces(RemovalUtility.SELECTION_SIZE * Integer.parseInt(nextAce == null ? "0" : nextAce)));
    pageContext.setAttribute("members", RemovalUtility.getMembers(RemovalUtility.SELECTION_SIZE * Integer.parseInt(nextMember == null ? "0" : nextMember)));
%>

<body>

<div class="info">
    This tool helps you find and clean references, found in roles and groups, of users which are unknown to the system (e.g. it can happen when a user has been removed from a LDAP directory).
</div><br/>
  
<div class="info">
  <b>List of current External User Providers:</b><br/>
  <%
     boolean inActiveUser = false;
     for (JCRStoreProvider prov : RemovalUtility.getExternalUserProvider()) {
         String output = prov.getKey();
         if (prov.isAvailable()) {
             output += " for " + prov.getMountPoint() + " - active";
         } else {
             output += " for " + prov.getMountPoint() + " - <b>inactive</b>";
             inActiveUser = true;
         }
         %><%=output%><br/><%
     
     }
     %>
  
    <br/><b>List of current External Group Providers: </b><br/>
     <%
     boolean inActiveGroup = false;
     for (JCRStoreProvider prov : RemovalUtility.getExternalGroupProvider()) {
         String output = prov.getKey();
         if (prov.isAvailable()) {
             output += " for " + prov.getMountPoint() + " - active";
         } else {
             output += " for " + prov.getMountPoint() + " - <b>inactive</b>";
             inActiveGroup = true;
         }
         %><%=output%></br><%
     
     }
     %>
     
    <br/><br/><b>Check if all of your External Providers are in the list (if a provider is stopped it won't appear in the list)!</b>
</div> <br/>
<% if (inActiveGroup || inActiveUser) { %>       
<div class="warning">
    BE CAREFULL, SOME PROVIDERS ARE INACTIVE, BEFORE YOU CLEAN CHECK IF THE REFERENCES SHOULD BE REALLY DELETED!
</div>
<%}%>
       

<div>
    <h2>Aces (jnt:ace) with nonexistent principals</h2>
    <c:choose>
        <c:when test="${not empty aces}">
            <form id="acesForm" action="?" method="post">
                <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
                <ul class="entryList">
                    <li><input type="checkbox" onclick="selectAll(event)" name="Select all"/> <strong>Select all</strong></li>
                    <c:forEach var="user" items="${aces}">
                        <li><input type="checkbox" name="acesToRemove" value="${user.path}"><strong>${user.name}</strong>&nbsp;at path&nbsp;<strong>${user.path}</strong></li>
                    </c:forEach>
                </ul>
                <input type="submit" name="action" value="Remove selected aces"  title="Remove selected aces" />
            </form>
        </c:when>
        <c:otherwise>
            No aces found
        </c:otherwise>
    </c:choose>
    <div class="navButtons">
        <c:if test="${nextAce != 0}">
            <form id="acesNavPrevForm" action="?" method="get">
                <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
                <input type="number" name="nextAce" id="prevAce" value="${nextAce == 0 ? nextAce : nextAce - 1}" hidden/>
                <input type="number" name="nextMember" value="${nextMember}" hidden/>
                <input type="submit" value="Prev"  title="Prev" />
            </form>
        </c:if>

        <span>Page ${nextAce + 1}</span>

        <c:if test="${not empty aces}">
            <form id="acesNavNextForm" action="?" method="get">
                <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
                <input type="number" name="nextAce" id="nextAce" value="${nextAce + 1}" hidden/>
                <input type="number" name="nextMember" value="${nextMember}" hidden/>
                <input type="submit" value="Next"  title="Next" />
            </form>
        </c:if>
    </div>
</div>

<div>
    <h2>Members (jnt:member) with nonexistent references</h2>
    <c:choose>
        <c:when test="${not empty members}">
            <form id="acesForm" action="?" method="post">
                <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
                <ul class="entryList">
                    <li><input type="checkbox" onclick="selectAll(event)" value="SelectAll"/> <strong>Select all</strong></li>
                    <c:forEach var="user" items="${members}">
                        <li><input type="checkbox" name="membersToRemove" value="${user.path}"><strong>${user.name}</strong>&nbsp;at path&nbsp;<strong>${user.path}</strong></li>
                    </c:forEach>
                </ul>
                <input type="submit" name="action" value="Remove selected members"  title="Remove selected members" />
            </form>
        </c:when>
        <c:otherwise>
            No members found
        </c:otherwise>
    </c:choose>
    <div class="navButtons">
        <c:if test="${nextMember != 0}">
            <form id="membersNavPrevForm" action="?" method="get">
                <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
                <input type="number" name="nextMember" id="prevMember" value="${nextMember == 0 ? nextMember : nextMember - 1}" hidden/>
                <input type="number" name="nextAce" value="${nextAce}" hidden/>
                <input type="submit" value="Prev" title="Prev" />
            </form>
        </c:if>

        <span>Page ${nextMember + 1}</span>

        <c:if test="${not empty members}">
            <form id="membersNavNextForm" action="?" method="get">
                <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
                <input type="number" name="nextMember" id="nextMember" value="${nextMember + 1}" hidden/>
                <input type="number" name="nextAce" value="${nextAce}" hidden/>
                <input type="submit" value="Next" title="Next" />
            </form>
        </c:if>
    </div>
</div>
</body>
</html>
