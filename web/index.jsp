<%-- 
    Document   : index
    Created on : Jun 7, 2017, 7:53:30 AM
    Author     : jbf
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>HAPI Verification</title>
    </head>
    <body>
        <h1>HAPI Verification</h1>
        This verifies HAPI servers by running a suite of checks.<br>
        <a href="index.html">View last run</a><br>
        <a href="DoUpdate">Run tests</a><br>
        
        <br>HAPI_VERIFIER_HOME=<%= getServletContext().getInitParameter("HAPI_VERIFIER_HOME") %><br>
        <%
            if ( getServletContext().getInitParameter("HAPI_VERIFIER_HOME")==null ) {
                out.print("(not specified, using /tmp/hapiVerifier/)");
            }
            %>
          
        <h4>Version history</h4>
        <small>2017-06-15c: somewhat useful.  Bugfixes.</small>
    </body>
</html>
