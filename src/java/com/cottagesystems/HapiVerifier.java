
package com.cottagesystems;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Check known HAPI servers against a suite of checks.
 * @author jbf
 */
public class HapiVerifier {
    
    private static final Logger logger= Logger.getLogger("HapiVerifier");
    
    /**
     * perform the check, adding the result CheckStatus to the collection of results from other tests.
     * @param results
     * @param check 
     */
    public static void doCheck( LinkedHashMap<String,CheckStatus> results, Check check ) {
        String checkName= check.getName();
        logger.log(Level.INFO, "-- doCheck {0} --", check.toString());
        CheckStatus checkStatus;
        try {
            checkStatus= check.doCheck();
        } catch ( Exception ex ) {
            checkStatus= new CheckStatus(1,ex.toString());
        }
        results.put( checkName, checkStatus );
    }
    
    /**
     * run all the checks on the server
     * @param server the HAPI server URL, ending in "/hapi"
     * @return a map from check name to CheckStatus
     */
    public static Map<String,CheckStatus> doChecks( URL server ) {
        LinkedHashMap<String,CheckStatus> results= new LinkedHashMap<>();
        List<Check> checks= new ArrayList<>();
        
        //checks.add( new CapabilitiesCheck( server ) );
        //checks.add( new CatalogCheck( server ) );
        //checks.add( new InfoCheck( server ) );
        checks.add( new DataCheck( server ) );
        
        for ( Check check : checks ) {
            doCheck( results, check );
        }
        return results;
    }
    
    private static String colorFor( int status ) {
        return status==0 ? "#38c550" : "#c55038";
    }
    
    public static void doAllServers( Writer outw ) throws MalformedURLException {
        
        PrintWriter out= new PrintWriter(outw);
        
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
            
    public static void doAllServers( PrintStream out ) throws MalformedURLException {
        doAllServers( new PrintWriter( out ) );
    }
    
    public static void main( String[] args ) throws MalformedURLException, FileNotFoundException {
        try (PrintWriter out = new PrintWriter(new File("/tmp/out.hapicheck.html"))) {
            doAllServers(out);
        }
    }
}
