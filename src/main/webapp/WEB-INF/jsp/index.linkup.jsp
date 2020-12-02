<%-- 
    Document   : index.linkup
    Created on : Aug 26, 2019, 10:27:01 AM
    Author     : เสือไฮ่
--%><%

%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%
%><%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %><%
%><%@page contentType="text/html" pageEncoding="UTF-8"%><%
%><c:if test="${empty path}">/</c:if><%
%><c:forEach varStatus="i" items="${path}"><%
    %><c:if test="${i.last}">${i.current}/</c:if><%
    %><c:if test="${!i.last}"><%
        %><a href="<c:forEach begin="2" end="${fn:length(path) - i.index}">../</c:forEach>"><%
          %>${i.current}/<%
        %></a><%
      %></c:if><%
%></c:forEach>