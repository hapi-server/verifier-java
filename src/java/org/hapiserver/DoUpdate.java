
package org.hapiserver;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author jbf
 */
@WebServlet(name = "DoUpdate", urlPatterns = {"/DoUpdate"})
public class DoUpdate extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     * 
     * parameters include: action=go, server=, test=.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        
        String action= request.getParameter("action");
        if ( action==null ) {
            action="";
        }
        String ROOT= getServletContext().getInitParameter("HAPI_VERIFIER_HOME");
        if ( ROOT==null ) {
            ROOT= "/tmp/hapiserver/";
        }
        File root= new File(ROOT);
        
        try (PrintStream out = new PrintStream( response.getOutputStream() ) ) {
            if ( action.equals("go") ) {
                String server= request.getParameter("server"); // might be null
                String test= request.getParameter("test"); // might be null.
                
                out.print("<body>");
                File newIndexFile= new File( root, "index.html.tmp" );
                if ( newIndexFile.exists() ) {
                    out.print("test appears to be running, because "+newIndexFile+" exists.<br>" );
                } else {
                    out.print("test system is idle.<br>" );
                }
                out.print("Return to <a href='index.jsp'>home</a>" );
                out.print("<h4>Log output from run:</h4>");
                if ( HapiVerifier.isRunning( root) ) {
                    out.println("Busy running.  Old results are available <a href='index.html'>here</a>.");
                } else {
                    HapiVerifier.doAllServers( out, root, server, test );
                }
                
                if ( test!=null && server!=null ) {
                    String testOutput= HapiVerifier.serverFolderName(new URL(server)) + "/" + test + ".html";
                    out.println( String.format( "<br>Return to <a href='%s'>test output</a><br>", testOutput ) );
                }
                
                out.println( "</body>" );
                
            } else {
                out.println( "<body>" );
                File newIndexFile= new File( root, "index.html.tmp" );
                if ( newIndexFile.exists() ) {
                    out.print("test appears to be running, because "+newIndexFile+" exists.<br>" );
                } else {
                    out.print("test system is idle.<br>" );
                }
                
                out.print("Return to <a href='index.jsp'>home</a>" );
                out.println( "<h4>Click to run test on all servers:</h4>");
                out.println( "<small>this will take a while, and server will not respond nicely.</small><br>");
                out.println( "Run <a href='"+request.getRequestURI()+"?action=go'>All Tests</a>" );
                
                out.println( "<h4>Click to run all tests on an individual server:</h4>");
                List<URL> servers= HapiVerifier.getServers(root);
                for ( URL server: servers ) {
                    out.println( "<a href='"+request.getRequestURI()+"?server="+server+"&action=go'>"+server+"</a><br>" );
                }

                out.println( "<h4>Click to run a tests on all servers:</h4>");
                List<String> tests= HapiVerifier.getCheckNames(root);
                for ( String test: tests ) {
                    out.println( "<a href='"+request.getRequestURI()+"?test="+test+"&action=go'>"+test+"</a><br>" );
                }
                
                out.println( "</body>" );
            }
        }
        
        

    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
