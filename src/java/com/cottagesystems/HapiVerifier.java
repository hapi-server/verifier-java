/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cottagesystems;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Check known HAPI servers against a suite of checks.
 * @author jbf
 */
public class HapiVerifier {
    
    public static void doCheck( LinkedHashMap<String,CheckStatus> results, Check check ) {
        try {
            results.put( check.getName(), check.doCheck()  );
        } catch (Exception ex) {
            results.put( check.getName(), new CheckStatus(1,ex.toString()) );
        }
    }
    
    /**
     * run all the checks on the server
     * @param server
     * @return 
     */
    public static Map<String,CheckStatus> doChecks( URL server ) {
        LinkedHashMap<String,CheckStatus> results= new LinkedHashMap<>();
        List<Check> checks= new ArrayList<>();
        
        checks.add( new CapabilitiesCheck( server ) );
        checks.add( new CatalogCheck( server ) );
        
        for ( Check check : checks ) {
            doCheck( results, check );
        }
        return results;
    }
    
    private static String colorFor( int status ) {
        return status==0 ? "#38c550" : "#c55038";
    }
    
    public static void doAllServers( PrintStream out ) throws MalformedURLException {
        
        String css= ".CellWithComment{\n" +
"  position:relative;\n" +
"}\n" +
"\n" +
".CellComment{\n" +
"  display:none;\n" +
"  position:absolute; \n" +
"  z-index:100;\n" +
"  border:1px;\n" +
"  background-color:white;\n" +
"  border-style:solid;\n" +
"  border-width:1px;\n" +
"  border-color:red;\n" +
"  padding:3px;\n" +
"  color:red; \n" +
"  top:20px; \n" +
"  left:20px;\n" +
"}\n" +
"\n" +
".CellWithComment:hover span.CellComment{\n" +
"  display:block;\n" +
"}";
        List<URL> servers= new ArrayList<>();
        servers.add( new URL("http://jfaden.net/HapiServerDemo/hapi") );
        servers.add( new URL("http://datashop.elasticbeanstalk.com/hapi") );
        servers.add( new URL("http://mag.gmu.edu/TestData/hapi") );
        
        out.printf("<html>");
        out.printf("<body><table border='1' style='%s'>", css );
        out.printf("<tr><td>Server</td>");
        Map<String,CheckStatus> check= doChecks(servers.get(0));
        for ( Entry<String,CheckStatus> e: check.entrySet() ) {
            out.printf("<td class=\"CellWithComment\">%s</td>", e.getKey() );
        }
        out.printf("</tr>");
        
        for ( URL server: servers ) {
            
            check= doChecks(server);                
            out.printf("<tr><td>%s</td>\n",server);
            
            for ( Entry<String,CheckStatus> e: check.entrySet() ) {
                CheckStatus c= e.getValue();
                String label= c.getMessage();
                label="";
                out.printf("<td class=\"CellWithComment\" color=\"%s\">%s<span class=\"CellComment\">%s</span></td>", colorFor( c.getStatus() ), c.getStatus(), label );
                
            }
            out.printf("</tr>\n" );
            
        }
        out.println("</table>");
        out.println("</body>");
    }
    
    public static void main( String[] args ) throws MalformedURLException, FileNotFoundException {
        PrintStream out= new PrintStream(new File("/tmp/out.hapicheck.html"));
        doAllServers(out);
        out.close();
    }
}
