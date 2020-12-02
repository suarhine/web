<%-- 
    Document   : index
    Created on : Aug 18, 2019, 1:55:06 AM
    Author     : เสือไฮ่
--%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="p" uri="/page" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Directory Listing For [${path}]</title>
    <STYLE>
      <!--
      h1 {
          font-family:Tahoma,Arial,sans-serif;
          color:white;
          background-color:#525D76;
          font-size:22px;
      }
      h2 {
          font-family:Tahoma,Arial,sans-serif;
          color:white;
          background-color:#525D76;
          font-size:16px;
      }
      h3 {
          font-family:Tahoma,Arial,sans-serif;
          color:white;
          background-color:#525D76;
          font-size:14px;
      }
      body {
          font-family:Tahoma,Arial,sans-serif;
          color:black;
          background-color:white;
      }
      b {
          font-family:Tahoma,Arial,sans-serif;color:white;
          background-color:#525D76;
      }
      p {
          font-family:Tahoma,Arial,sans-serif;
          background:white;
          color:black;
          font-size:12px;
      }
      a {
          color:black;
      }
      a.name {
          color:black;
      }
      .line {
          height:1px;
          background-color:#525D76;
          border:none;
      }
      table tr:nth-child(odd) {
          background-color: #eeeeee;
      }
      h1 > a {
          color: white;
          text-decoration: none;
      }
      -->
    </STYLE>
  </head>
  <body>
    <h1>Directory Listing For [<p:include page="index.linkup.jsp" path="${path.split('/')}" />]</h1>
    <HR size="1" noshade="noshade">
    <form method="post">
      <table width="100%" cellspacing="0" cellpadding="5" align="center">
        <tr>
          <td align="left"><font size="+1"><strong>Filename</strong></font></td>
          <td align="center"><font size="+1"><strong>Size</strong></font></td>
          <td align="right"><font size="+1"><strong>Last Modified</strong></font></td>
        </tr>
        <c:forEach var="i" items="${entries}">
            <c:if test="${i.directory}">
                <c:if test="${path != '/' || !(i.name == 'META-INF' || i.name == 'WEB-INF')}">
                    <tr>
                      <td align="left">&nbsp;&nbsp;
                        <a href="${pageContext.servletContext.contextPath}${path}${i.name}/">
                          <tt>
                            ${i.name}/
                          </tt>
                        </a>
                      </td>
                      <td align="right"><tt>&nbsp;</tt></td>
                      <td align="right"><tt>${i.lastModifiedHttp}</tt></td>
                    </tr>
                </c:if>
            </c:if>
        </c:forEach>
        <tr>
          <td align="left" colspan="3">
            <input name="dir" type="text" style="width: 100%" /><button style="display:none;"></button>
          </td>
        </tr>
        <c:forEach var="i" items="${entries}">
            <c:if test="${!i.directory}">
                <tr>
                  <td align="left">
                    <input name="name" value="${i.name}" type="checkbox" />
                    <a href="${pageContext.servletContext.contextPath}${path}${i.name}"><tt>${i.name}</tt></a></td>
                  <td align="right">
                    <tt>
                      <c:if test="${i.contentLength < 1024}">${i.contentLength} b</c:if>
                      <c:if test="${1024 <= i.contentLength && i.contentLength < 1024 * 1024}">
                          <fmt:formatNumber pattern="0.#" value="${i.contentLength / 1024}" /> kb
                      </c:if>
                      <c:if test="${1024 * 1024 <= i.contentLength && i.contentLength < 1024 * 1024 * 1024}">
                          <fmt:formatNumber pattern="0.#" value="${i.contentLength / (1024 * 1024)}" /> mb
                      </c:if>
                      <c:if test="${1024 * 1024 * 1024 <= i.contentLength && i.contentLength < 1024 * 1024 * 1024 * 1024}">
                          <fmt:formatNumber pattern="0.#" value="${i.contentLength / (1024 * 1024 * 1024)}" /> gb
                      </c:if>
                    </tt>
                  </td>
                  <td align="right"><tt>${i.lastModifiedHttp}</tt></td>
                </tr>
            </c:if>
        </c:forEach>
        <tr>
          <td colspan="2">
            <button name="delete" >Delete Selected Files</button>
            <c:if test="${path != '/'}">
                <button name="delete-dir" >Delete This Directory</button>
            </c:if>
          </td>
          <td align="right">
            <input name="file" type="file" multiple onchange="this.form.setAttribute('enctype', 'multipart/form-data');this.form.submit();"/>
          </td>
        </tr>
      </table>
    </form>
    <HR size="1" noshade="noshade"><h3><%=org.apache.catalina.util.ServerInfo.getServerInfo()%></h3></body>
</html>
