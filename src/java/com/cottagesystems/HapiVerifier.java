
package com.cottagesystems;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Check known HAPI servers against a suite of checks.
 * @author jbf
 */
public class HapiVerifier {
    
    private static final Logger logger= Logger.getLogger("HapiVerifier");
    
    /**
     * perform the check, adding the result CheckStatus to the collection of results from other tests.
     * @param results CheckStatus result for each check
     * @param check the check to perform.
     */
    public static void doCheck( LinkedHashMap<String,CheckStatus> results, Check check ) {
        String checkName= check.getName();
        logger.log(Level.INFO, "-- doCheck {0} --", check.toString());
        final StringBuilder b= new StringBuilder();
        Handler h= new Handler() {
            @Override
            public void publish(LogRecord record) {
                SimpleFormatter formatter= new SimpleFormatter();
                String s= formatter.formatMessage(record);
                b.append(s);
                b.append("\n");
            }

            @Override
            public void flush() {   
            }

            @Override
            public void close() throws SecurityException {
            }
        };
        logger.addHandler(h);
        CheckStatus checkStatus;
        try {
            checkStatus= check.doCheck();
        } catch ( Exception ex ) {
            checkStatus= new CheckStatus(1,ex.toString());
        }
        checkStatus.setLog(b.toString());
        logger.removeHandler(h);
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
        
        checks.add( new CapabilitiesCheck( server ) );
        checks.add( new CatalogCheck( server ) );
        checks.add( new InfoCheck( server ) );
        checks.add( new DataCheck( server ) );
        
        for ( Check check : checks ) {
            if ( results.containsKey(check.getName() ) ) {
                throw new IllegalArgumentException("check name is used twice: "+check.getClass().getName() );
            }
            doCheck( results, check );
        }
        return results;
    }
    
    private static String colorFor( int status ) {
        return status==0 ? "#38c550" : "#c55038";
    }
    
    public static void doAllServers( File root ) throws MalformedURLException, FileNotFoundException {
         
        if ( !root.exists() ) {
            if ( !root.mkdirs() ) {
                throw new IllegalArgumentException("unable to mkdir "+root);
            }
        }
        
        try (PrintWriter out = new PrintWriter( new File( root, "index.html" ) )) {
            List<URL> servers= new ArrayList<>();
            servers.add( new URL("http://jfaden.net/HapiServerDemo/hapi") );
            servers.add( new URL("http://datashop.elasticbeanstalk.com/hapi") );
            servers.add( new URL("http://mag.gmu.edu/TestData/hapi") );
            
            out.printf("<html>");
            out.printf("<body><table border='1' >" );
            out.printf("<tr><td>Server</td>");
            Map<String,CheckStatus> check= doChecks(servers.get(0));
            for ( Entry<String,CheckStatus> e: check.entrySet() ) {
                out.printf("<td>%s</td>", e.getKey() );
            }
            out.printf("</tr>");
            
            for ( URL server: servers ) {
                
                check= doChecks(server);
                out.printf("<tr><td><a href='%s'>%s</a></td>\n",server,server);
                
                String serverName= server.getPath().replaceAll("/", "_");
                serverName= serverName.replaceAll(":","");
                File serverRoot= new File( root, serverName );
                if ( !serverRoot.exists() ) {
                    if ( !serverRoot.mkdirs() ) {
                        throw new IllegalArgumentException("unable to mkdir "+serverRoot);
                    }
                }
                
                for ( Entry<String,CheckStatus> e: check.entrySet() ) {
                    CheckStatus c= e.getValue();
                    
                    try (PrintWriter out2 = new PrintWriter( new File( serverRoot, e.getKey()+".html" ) )) {
                        out2.println( "<h2>" );
                        out2.println( "Test \""+e.getKey()+"\" on server "+ server );
                        out2.println( "</h2>" );
                        out2.println( "Status Code=" + c.getStatus() + "<br>");
                        out2.println( c.getMessage() );
                        out2.println( "<br>" );
                        out2.println( "<h2>Log output</h2>");
                        out2.println( makeHtml( c.getLog() ) );
                    }
                    String ball= c.getStatus()==0 ? "blue" : "red";
                    out.printf("<td color=\"%s\"><a href=\"%s/%s.html\"><img src='%s.gif'></a></td>", colorFor( c.getStatus() ), serverName, e.getKey(), ball );
                    
                }
                out.printf("</tr>\n" );
                
            }
            out.println("</table>");
            
            out.println("<a href=\"index.jsp\">manage</a>");
            out.println("</body>");
        }
    }
    
    private static String makeHtml( String raw ) {
        StringBuilder builder= new StringBuilder();
        String[] ss= raw.split("\n");
        Pattern url= Pattern.compile("(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?");
        
        for ( String s: ss ) {
            if ( s.contains("http://jfaden.net/HapiServerDemo/hapi/catalog") ) {
                System.err.println("here");
            }
            Matcher m= url.matcher(s);
            int more= 0;
            while ( m.find() ) {
                builder.append( s.substring(0,m.start()) );
                more= m.end();
                String surl= s.substring(m.start(),more);
                builder.append("<a href='").append(surl).append("'>").append(surl).append("</a>");
            }
            builder.append( s.substring(more) ); // I know there's a matcher field for this, just can't find it...
            builder.append( "<br>\n");
        }
        
        return builder.toString();
    }
            
    public static void doAllServers( PrintStream out ) throws MalformedURLException, FileNotFoundException, IOException {
        doAllServers( new File("/tmp/hapiVerifier/") );
        out.write( "<a href='index.html'>here</a>".getBytes() );
        out.close();
    }
    
    public static void main( String[] args ) throws MalformedURLException, FileNotFoundException {
        doAllServers( new File("/tmp/hapiVerifier/" ));
    }
}
