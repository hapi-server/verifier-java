
package com.cottagesystems;

import static com.cottagesystems.Check.getJSONObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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
import org.json.JSONException;
import org.json.JSONObject;


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
        CheckStatus cached= haveCached(check);
        if ( cached!=null ) {
            results.put( checkName, cached );
            return;
        }
        
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
        try {
            cache( check, checkStatus );
        } catch ( IOException ex ) {
            logger.warning("unable to cache result");
        }
    }
    
    /**
     * return null or the cached check status.
     * @param check
     * @return 
     */
    public static CheckStatus haveCached( Check check ) {
        File serverRoot= HapiVerifier.serverFolder(check.getHapi());
        File cacheFile= new File( serverRoot, check.getName() + ".json" );
        if ( cacheFile.exists() ) {
            try {
                JSONObject jo= getJSONObject( new URL( "file:"+ cacheFile ) );
                int status= jo.getInt("status");
                String message= jo.getString("message");
                CheckStatus result= new CheckStatus(status,message);
                result.setLog( jo.getString("log") );
                return result;
            } catch (MalformedURLException ex) {
                Logger.getLogger(Check.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSONException | IOException ex) {
                Logger.getLogger(Check.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }
    
    /**
     * add the result to the cache of results.
     * @param check
     * @param status
     * @throws IOException 
     */
    public static void cache( Check check, CheckStatus status ) throws IOException {
        File serverRoot= HapiVerifier.serverFolder(check.getHapi());
        if ( !serverRoot.exists() && serverRoot.mkdirs() ) {
            throw new IOException("unable to mkdir "+serverRoot);
        }
        File cacheFile= new File( serverRoot, check.getName() + ".json" );
        try ( FileWriter fw= new FileWriter(cacheFile) ) {
            JSONObject jo= new JSONObject();
            jo.put( "status", status.getStatus() );
            jo.put( "message", status.getMessage() );
            jo.put( "log", status.getLog() );
            jo.write( fw );
        } catch (JSONException ex) {
            Logger.getLogger(Check.class.getName()).log(Level.SEVERE, null, ex);
        }
        logger.log(Level.INFO, "wrote cache file {0}", cacheFile);
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
        checks.add( new PartialDataCheck( server ) );
        
        for ( Check check : checks ) {
            if ( results.containsKey(check.getName() ) ) {
                throw new IllegalArgumentException("check name is used twice: "+check.getClass().getName() );
            }
            doCheck( results, check );
        }
        return results;
    }
    
    /**
     * return the directory containing cached test results.
     * @param server
     * @return 
     */
    public static String serverFolderName( URL server ) {
        String serverName= server.getProtocol() + "_" + server.getAuthority() + server.getPath().replaceAll("/", "_");
        return serverName;
    }
    
    public static File serverFolder( URL server ) {
        String serverName= serverFolderName( server );
        File serverRoot= new File( root, serverName );
        return serverRoot;
    }
    
    private static File root;
    
    public static void doAllServers( File root ) throws MalformedURLException, FileNotFoundException {
         
        HapiVerifier.root= root;
        
        long t0= System.currentTimeMillis();
                
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
                
                String serverName= serverFolderName(server);
                        
                File serverRoot= serverFolder(server);
                if ( !serverRoot.exists() ) {
                    if ( !serverRoot.mkdirs() ) {
                        throw new IllegalArgumentException("unable to mkdir "+serverRoot);
                    }
                }

                out.printf("<tr><td><a href='%s'>%s</a></td>\n",serverName+".html",server);
                
                PrintWriter out3= new PrintWriter( new File( root, serverName + ".html" ) );
                out3.println( "<h2>Server <a href="+server+">"+server +"</a></h2>");
                
                out3.println( "<table>");
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
                    out3.printf( "<tr><td><a href=\"%s/%s.html\"><img src='%s.gif'>%s</a></td><td>%s</td></tr>\n", serverName, e.getKey(), ball, e.getKey(), c.getMessage() );
                    out.printf("<td><a href=\"%s/%s.html\"><img src='%s.gif'></a></td>", serverName, e.getKey(), ball );
                    
                }
                out.printf("</tr>\n" );
                out3.println("</table>");
                out3.close();
            }
            out.println("</table>");
            
            out.println("<a href=\"index.jsp\">manage</a>");
            
            out.println("<small>Complete test suite calculated in "+ String.format( "%.2f", (System.currentTimeMillis()-t0)/60000. ) + " minutes." );
            out.println("</body>");
        }
    }
    
    private static String makeHtml( String raw ) {
        StringBuilder builder= new StringBuilder();
        String[] ss= raw.split("\n");
        Pattern url= Pattern.compile("(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?");
        
        for ( String s: ss ) {
            Matcher m= url.matcher(s);
            int more= 0;
            while ( m.find() ) {
                builder.append( s.substring(0,m.start()) );
                more= m.end();
                String surl= s.substring(m.start(),more);
                builder.append("<a href='").append(surl).append("'>").append(surl.replaceAll("\\&", "&amp;") ).append("</a>");
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
