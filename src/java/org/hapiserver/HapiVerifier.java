
package org.hapiserver;

import static org.hapiserver.Check.getJSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
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
        
        logger.log(Level.INFO, "### doCheck {0} ###", check.toString());
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
     * return a list of checks to perform, such as "capabilities" and "data"
     * read from the file ROOT/checks.txt.
     * @param root the root of the testing area.
     * @return the list of checks.
     * @throws IOException 
     */
    public static List<String> getCheckNames( File root ) throws IOException {
        List<String> checks= new ArrayList<>();
        File checksFile= new File( root, "checks.txt" );
        if ( checksFile.exists() ) {
            try ( BufferedReader read= new BufferedReader(new FileReader(checksFile)) ) {
                String line;
                while ( ( line= read.readLine())!=null ) {
                    int i= line.indexOf('#');
                    if ( i>-1 ) line= line.substring(0,i);
                    if ( line.trim().length()!=0 ) {
                        checks.add( line ) ;
                    }
                }
            }
        } else {
            checks.add( "capabilities" );
            checks.add( "catalog" );
            checks.add( "info" );
            checks.add( "data" );
            checks.add( "partialdata" );
            try ( BufferedWriter write= new BufferedWriter( new FileWriter(checksFile) ) ) {
                for ( String check1: checks ) {
                    write.write(check1);
                    write.write('\n');
                }
            }
        }
        return checks;
    }
    
    /**
     * return a list of servers to check, using ROOT/servers.txt.  This
     * file will be created with a hard-coded list of servers, 
     * @param root
     * @return list of servers, pointing to the ".../hapi" landing page.
     * @throws IOException 
     */
    public static List<URL> getServers(File root) throws IOException {
        List<URL> servers= new ArrayList<>();
        File serversFile = new File(root, "servers.txt");
        if ( serversFile.exists() ) {
            try ( BufferedReader read= new BufferedReader(new FileReader(serversFile)) ) {
                String line;
                while ( ( line= read.readLine())!=null ) {
                    if ( line.trim().length()!=0 ) {
                        servers.add( new URL(line) ) ;
                    }
                }
            }
        } else {
            servers.add( new URL("http://jfaden.net/HapiServerDemo/hapi") );
            servers.add( new URL("http://datashop.elasticbeanstalk.com/hapi") );
            servers.add( new URL("http://mag.gmu.edu/TestData/hapi") );
            try ( BufferedWriter write= new BufferedWriter( new FileWriter(serversFile) ) ) {
                for ( URL server1: servers ) {
                    write.write(server1.toExternalForm());
                    write.write('\n');
                }
            }
        }
        return servers;
    }
    
    
    /**
     * run all the checks on the server
     * @param root testing area
     * @param server the HAPI server URL, ending in "/hapi"
     * @return a map from check name to CheckStatus
     * @throws java.io.IOException
     */
    public static Map<String,CheckStatus> doChecks( File root, URL server ) throws IOException {
        LinkedHashMap<String,CheckStatus> results= new LinkedHashMap<>();
        List<Check> checks= new ArrayList<>();
        
        List<String> checkNames= getCheckNames(root);
        for ( String checkName: checkNames ) {
            checks.add( Check.lookup(checkName,server) );
        }
        
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
    
    /**
     * run through all servers and all tests, but using cached results where they
     * exist.  Typically a number of the cache files (*.json) will be deleted
     * and the test is rerun.
     * @param root
     * @throws MalformedURLException
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static void doAllServers( File root ) throws MalformedURLException, FileNotFoundException, IOException {
         
        HapiVerifier.root= root;
        
        long t0= System.currentTimeMillis();
                
        if ( !root.exists() ) {
            if ( !root.mkdirs() ) {
                throw new IllegalArgumentException("unable to mkdir "+root);
            }
        }

        File icon;
        icon= new File( root, "red.gif" );
        if ( !icon.exists()) transfer( HapiVerifier.class.getResourceAsStream("/resource/red.gif"), new FileOutputStream(icon) );
        icon= new File( root, "blue.gif" );
        if ( !icon.exists()) transfer( HapiVerifier.class.getResourceAsStream("/resource/blue.gif"), new FileOutputStream(icon) );
        icon= new File( root, "grey.gif" );
        if ( !icon.exists()) transfer( HapiVerifier.class.getResourceAsStream("/resource/grey.gif"), new FileOutputStream(icon) );
        
        List<URL> servers= getServers(root);
        
        try (PrintWriter out = new PrintWriter( new File( root, "index.html" ) )) {
            
            out.printf("<html>");
            out.printf("<body><table border='1' >" );
            out.printf("<tr><td>Server</td>");
            
            List<String> checkNames= getCheckNames(root);
            
            for ( String checkName: checkNames ) {
                out.printf("<td>%s</td>", checkName );
            }
            out.printf("</tr>");
            
            for ( URL server: servers ) {
                
                Map<String,CheckStatus> check= doChecks(root,server);
                
                String serverName= serverFolderName(server);
                        
                File serverRoot= serverFolder(server);
                if ( !serverRoot.exists() ) {
                    if ( !serverRoot.mkdirs() ) {
                        throw new IllegalArgumentException("unable to mkdir "+serverRoot);
                    }
                }

                out.printf("<tr><td><a href='%s'>%s</a></td>\n",serverName+".html",server);
                
                try (PrintWriter out3 = new PrintWriter( new File( root, serverName + ".html" ) )) {
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
                }
            }
            out.println("</table>");
            
            out.println("<a href=\"index.jsp\">manage</a><br>\n");
            
            out.println("data is stored in "+HapiVerifier.root+"<br>\n");
            
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
            
    /**
     * transfers the data from one channel to another.  src and dest are
     * closed after the operation is complete.
     * @param src
     * @param dest
     * @throws java.io.IOException
     */
    public static void transfer( InputStream src, OutputStream dest ) throws IOException {
        final byte[] buffer = new byte[ 16 * 1024 ];

        int i= src.read(buffer);
        while ( i != -1) {
            dest.write(buffer,0,i);
            i= src.read(buffer);
        }
        dest.close();
        src.close();
    }
    
    /**
     * reset the test caches, limiting to one server when this is non-null, or
     * one test when it is non-null.
     * @param root
     * @param server null or the server
     * @param test null or the test
     */
    public static void resetCachedResults( File root, URL server, String test ) {
        File[] ff= root.listFiles();
        String sserver=null;
        if ( server!=null ) {
            sserver= serverFolderName(server);
        }
        for ( File f: ff ) {
            if ( f.isDirectory() ) {
                if ( sserver==null || f.getName().equals(sserver) ) {
                    File [] ff1= f.listFiles();
                    for ( File f1: ff1 ) {
                        if ( f1.getName().endsWith(".json") ) {
                            if ( test==null || f1.getName().equals(test+".json") ) {
                                if ( !f1.delete() ) {
                                    throw new IllegalArgumentException("unable to delete file: "+f1);
                                }
                            }
                        }
                    }
                }
                
            }
        }
    }
    
    /**
     * run the tests, refreshing sserver=null and test=null.
     * @param out status messages.
     * @param root the output folder.
     * @param sserver
     * @param test
     * @throws MalformedURLException
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static void doAllServers( final PrintStream out, File root, String sserver, String test ) throws MalformedURLException, FileNotFoundException, IOException {
        URL server= sserver==null?null:new URL(sserver);
        resetCachedResults( root,server,test);
        logger.addHandler( new Handler() {
            @Override
            public void publish(LogRecord record) {
                SimpleFormatter formatter= new SimpleFormatter();
                String s= formatter.formatMessage(record);
                out.append(s.replaceAll("\n","<br>"));
                out.append("<br>");
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });
        doAllServers( root );
        out.close();
    }
    
    /**
     * Run the test.  You can refresh for one server with:
     * --server=http://jfaden.net/HapiServerDemo/hapi 
     * or one test with:
     * --test==info
     * @param args
     * @throws MalformedURLException
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static void main( String[] args ) throws MalformedURLException, FileNotFoundException, IOException {
        File r= new File("/tmp/hapiVerifier/" );
        String sserver=null;
        String test=null;
        //args= new String[] { "--server=http://jfaden.net/HapiServerDemo/hapi", "--test=info" };
        for ( String s:args ) {
            if ( s.startsWith("--server=") ) sserver=s.substring(9);
            if ( s.startsWith("--test=") ) test=s.substring(7);
            if ( s.startsWith("--root=") ) r= new File(s.substring(7));
        }
        URL server= sserver==null?null:new URL(sserver);
        resetCachedResults(r,server,test);
        
        doAllServers(r);
    }
}
