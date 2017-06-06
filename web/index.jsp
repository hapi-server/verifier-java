<%-- 
    Document   : index
    Created on : Jun 5, 2017, 12:50:52 PM
    Author     : jbf
--%>

<%@page import="com.cottagesystems.HapiVerifier"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>HAPI Verifier</title>
    </head>
    <body>  
        <h1>Running everything in-line, which will take a while...</h1>
        
        <% HapiVerifier.doAllServers(out); %>
        
    </body>
</html>
