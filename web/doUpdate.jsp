<%-- 
    Document   : doUpdate
    Created on : Jun 7, 2017, 7:24:03 AM
    Author     : jbf
--%>

<%@page import="java.io.File"%>
<%@page import="com.cottagesystems.HapiVerifier"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>
        <h1>Updating tests</h1>
        Wait a minute or so, and then click <a href="index.html">here</a>.
        <%
            Runnable run= new Runnable() {
                public void run() {
                    String p= getServletContext().getRealPath("");
                    try {
                        HapiVerifier.doAllServers( new File( p ) );
                    } catch ( Exception ex ) {
                        ex.printStackTrace();
                    }
                }
            };
            new Thread(run).start();
            %>
    </body>
</html>
