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

<%!
    /**
     * Parse a page-index query parameter safely.
     * Returns 0 for null, blank, non-integer, or negative values.
     */
    private static int safePage(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            return v < 0 ? 0 : v;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
%>

<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>User Cleanup Tool</title>
    <style>
        .navButtons {
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            max-width: 200px;
            margin-top: 10px;
        }

        .entryList {
            list-style: none;
        }

        .entryList li {
            display: flex;
            flex-direction: row;
            align-items: baseline;
            padding: 0 0 10px;
        }

        /*
         * Contrast ratios (WCAG 2.2 SC 1.4.6 AAA, threshold 7:1):
         *   .info:    #00205b on #dceeff  ≈ 14.9:1  (AAA pass)
         *   .warning: #7a0000 on #fff3cd  ≈ 10.9:1  (AAA pass)
         */
        .info {
            font-size: large;
            color: #00205b;
            background-color: #dceeff;
            padding: 10px;
            max-width: 700px;
        }
        .warning {
            font-size: large;
            color: #7a0000;
            background-color: #fff3cd;
            padding: 10px;
            max-width: 700px;
        }

        /* Visible focus ring for keyboard / switch users (WCAG 2.4.11 / 2.4.12) */
        input[type="checkbox"]:focus-visible,
        input[type="submit"]:focus-visible {
            outline: 3px solid #005fcc;
            outline-offset: 2px;
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

    // Guard: safePage() returns 0 for null / non-integer / negative input (robustness fix #7)
    int acePageIndex    = safePage(request.getParameter("nextAce"));
    int memberPageIndex = safePage(request.getParameter("nextMember"));

    pageContext.setAttribute("aces",    RemovalUtility.getUsersFromAces(RemovalUtility.SELECTION_SIZE * acePageIndex));
    pageContext.setAttribute("members", RemovalUtility.getMembers(RemovalUtility.SELECTION_SIZE * memberPageIndex));
%>

<body>
<main>
    <h1>User Cleanup Tool</h1>

    <div class="info">
        This tool helps you find and clean references, found in roles and groups, of users which are unknown to the system (e.g. it can happen when a user has been removed from a LDAP directory).
    </div><br/>

    <div class="info">
        <b>List of current External User Providers:</b><br/>
        <%
            /*
             * Robustness fix #8: prov.getKey() and prov.getMountPoint() are written via
             * fn:escapeXml (JSTL) for the variable parts; the static " - <b>inactive</b>"
             * suffix is kept as literal markup so the bold tag is preserved without
             * creating an XSS vector from provider config values.
             */
            boolean inActiveUser = false;
            for (JCRStoreProvider prov : RemovalUtility.getExternalUserProvider()) {
                pageContext.setAttribute("provKey",   prov.getKey());
                pageContext.setAttribute("provMount", prov.getMountPoint());
                if (prov.isAvailable()) {
        %><c:out value="${provKey}"/> for <c:out value="${provMount}"/> - active<br/><%
                } else {
        %><c:out value="${provKey}"/> for <c:out value="${provMount}"/> - <b>inactive</b><br/><%
                    inActiveUser = true;
                }
            }
        %>

        <br/><b>List of current External Group Providers:</b><br/>
        <%
            boolean inActiveGroup = false;
            for (JCRStoreProvider prov : RemovalUtility.getExternalGroupProvider()) {
                pageContext.setAttribute("provKey",   prov.getKey());
                pageContext.setAttribute("provMount", prov.getMountPoint());
                if (prov.isAvailable()) {
        %><c:out value="${provKey}"/> for <c:out value="${provMount}"/> - active<br/><%
                } else {
        %><c:out value="${provKey}"/> for <c:out value="${provMount}"/> - <b>inactive</b><br/><%
                    inActiveGroup = true;
                }
            }
        %>

        <br/><br/><b>Check if all of your External Providers are in the list (if a provider is stopped it won&#8217;t appear in the list)!</b>
    </div><br/>

    <% if (inActiveGroup || inActiveUser) { %>
    <div class="warning" role="alert">
        WARNING: SOME PROVIDERS ARE INACTIVE. BEFORE YOU CLEAN, CHECK IF THE REFERENCES SHOULD REALLY BE DELETED!
    </div>
    <% } %>

    <div>
        <h2>Aces (jnt:ace) with nonexistent principals</h2>
        <c:choose>
            <c:when test="${not empty aces}">
                <form id="acesForm" action="?" method="post">
                    <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
                    <ul class="entryList">
                        <li>
                            <%-- SC 1.3.1 / 4.1.2: label wraps the control so the "Select all" text is
                                 programmatically associated. No form name needed (client-side only). --%>
                            <label>
                                <input type="checkbox" onclick="selectAll(event)"/>
                                <strong>Select all</strong>
                            </label>
                        </li>
                        <c:forEach var="user" items="${aces}" varStatus="aceStatus">
                            <%-- SC 1.3.1 / 4.1.2: unique id ties the <label> to the checkbox.
                                 The label text includes both name and path so screen readers
                                 announce the full context without relying on adjacent text. --%>
                            <li>
                                <label for="ace-${aceStatus.index}">
                                    <input type="checkbox"
                                           id="ace-${aceStatus.index}"
                                           name="acesToRemove"
                                           value="${fn:escapeXml(user.path)}"/>
                                    <strong><c:out value="${user.name}"/></strong>&nbsp;at path&nbsp;<strong><c:out value="${user.path}"/></strong>
                                </label>
                            </li>
                        </c:forEach>
                    </ul>
                    <input type="submit" name="action" value="Remove selected aces" title="Remove selected aces"/>
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
                    <input type="number" name="nextAce" id="prevAce" value="${nextAce == 0 ? nextAce : nextAce - 1}" hidden="hidden"/>
                    <input type="number" name="nextMember" value="${nextMember}" hidden="hidden"/>
                    <input type="submit" value="Prev" title="Previous page of aces"/>
                </form>
            </c:if>

            <span>Page ${nextAce + 1}</span>

            <c:if test="${not empty aces}">
                <form id="acesNavNextForm" action="?" method="get">
                    <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
                    <input type="number" name="nextAce" id="nextAceInput" value="${nextAce + 1}" hidden="hidden"/>
                    <input type="number" name="nextMember" value="${nextMember}" hidden="hidden"/>
                    <input type="submit" value="Next" title="Next page of aces"/>
                </form>
            </c:if>
        </div>
    </div>

    <div>
        <h2>Members (jnt:member) with nonexistent references</h2>
        <c:choose>
            <c:when test="${not empty members}">
                <%-- Duplicate id "acesForm" fixed: renamed to membersForm --%>
                <form id="membersForm" action="?" method="post">
                    <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
                    <ul class="entryList">
                        <li>
                            <label>
                                <input type="checkbox" onclick="selectAll(event)"/>
                                <strong>Select all</strong>
                            </label>
                        </li>
                        <c:forEach var="user" items="${members}" varStatus="memberStatus">
                            <li>
                                <label for="member-${memberStatus.index}">
                                    <input type="checkbox"
                                           id="member-${memberStatus.index}"
                                           name="membersToRemove"
                                           value="${fn:escapeXml(user.path)}"/>
                                    <strong><c:out value="${user.name}"/></strong>&nbsp;at path&nbsp;<strong><c:out value="${user.path}"/></strong>
                                </label>
                            </li>
                        </c:forEach>
                    </ul>
                    <input type="submit" name="action" value="Remove selected members" title="Remove selected members"/>
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
                    <input type="number" name="nextMember" id="prevMember" value="${nextMember == 0 ? nextMember : nextMember - 1}" hidden="hidden"/>
                    <input type="number" name="nextAce" value="${nextAce}" hidden="hidden"/>
                    <input type="submit" value="Prev" title="Previous page of members"/>
                </form>
            </c:if>

            <span>Page ${nextMember + 1}</span>

            <c:if test="${not empty members}">
                <form id="membersNavNextForm" action="?" method="get">
                    <input type="hidden" name="toolAccessToken" value="${toolAccessToken}"/>
                    <input type="number" name="nextMember" id="nextMemberInput" value="${nextMember + 1}" hidden="hidden"/>
                    <input type="number" name="nextAce" value="${nextAce}" hidden="hidden"/>
                    <input type="submit" value="Next" title="Next page of members"/>
                </form>
            </c:if>
        </div>
    </div>
</main>
</body>
</html>
