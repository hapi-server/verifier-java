
package org.hapiserver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import static org.hapiserver.Check.getJSONObject;
import static org.hapiserver.Check.hapiURL;
import static org.hapiserver.Check.logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jbf
 */
public class DataWithHeaderCheck extends Check {
     
    public DataWithHeaderCheck( URL hapi ) {
        super(hapi,"DataWithHeader");
    }
    
    private CheckStatus doCheck( String id, String parameters, String min, String max, int nf ) throws Exception {
        Map<String,String> params= new LinkedHashMap<>();
        params.put( "id", id );
        params.put( "time.min", min );
        params.put( "time.max", max );
        params.put( "parameters", parameters );
        params.put( "include", "header" );
        URL data= hapiURL( hapi, "data", params );
        logger.log(Level.INFO, "opening {0}", data);
        
        
        int actualFieldCount= -1;
        int len=0;
        StringBuilder b= new StringBuilder();
        StringBuilder jsonB= new StringBuilder();
        
        try ( BufferedReader read= new BufferedReader( new InputStreamReader(data.openStream()) ) ) {
            String s;
            while ( ( s=read.readLine() )!=null ) {
                if ( s.startsWith("#") ) {
                    jsonB.append(s.substring(1)).append("\n");
                } else {
                    if ( actualFieldCount==-1 ) {
                        actualFieldCount= s.split(",").length;
                    }
                    b.append(s).append("\n");
                    len+= 1;
                }
            }
        }
        
        StringBuilder errors= new StringBuilder();
        
        logger.log(Level.INFO, "Records received: {0}", len);
        if ( len>0 ) {
            if ( actualFieldCount!=nf ) {
                errors.append("expected ").append(nf).append(" fields but got ").append(actualFieldCount).append("\n");
            } 
        } else {
            errors.append("empty response").append("\n");
        }
        
        try {
            JSONObject jo= new JSONObject(jsonB.toString());
            int actualNf= numberOfFields( jo, parameters );
            if ( actualNf!=nf ) {
                errors.append("expected number of fields implied by header is wrong.  Expected ").append(nf).append(" got ").append(actualNf).append("...\n");
            }
        } catch ( JSONException ex ) {
            errors.append(ex.toString()).append("\n");
        }
        
        if ( errors.length()==0 ) {
            return new CheckStatus(0);
        } else {
            logger.info(errors.toString());
            return new CheckStatus(2,errors.toString());
        }
        
    }

    private static int numberOfFields( JSONObject jo, String params ) throws JSONException {
        JSONArray arr= jo.getJSONArray("parameters");
        
        String[] ss= params.split(",");
        
        int nf= 0;
        for ( int i=0; i<arr.length(); i++ ) {
            JSONObject arr1= arr.getJSONObject(i);
            String n= arr1.getString("name");
            if ( params.contains(n) ) {
                if ( arr1.has("size") ) {
                    JSONArray size= arr1.getJSONArray("size");
                    int p=1;
                    for ( int j=0; j<size.length(); j++ ) p*= size.getInt(j);
                    nf+= p;
                } else {
                    nf+= 1;
                }
            }
        }

        return nf;

    }
    
    private CheckStatus doCheck(String id) throws Exception {
        URL info= hapiURL( hapi, "info", Collections.singletonMap( "id", id ) );
        JSONObject jo= getJSONObject(info);        
        String[] ss= HapiUtil.getSampleRange(jo);        
        JSONArray arr= jo.getJSONArray("parameters");
        
        if ( arr.length()<2 ) {
            return new CheckStatus(0);
        } else {
            String parameters= arr.getJSONObject(0).getString("name") + "," + arr.getJSONObject(1).getString("name");
            int nf= numberOfFields(jo,parameters);
            return doCheck( id, parameters, ss[0], ss[1], nf );
        }

    }
    
    
    @Override
    public CheckStatus doCheck() throws Exception {
        URL catalog= hapiURL( hapi, "catalog", null );
        JSONObject jo= getJSONObject(catalog);
        jo.getString("HAPI");
        jo.getString("status");
        JSONArray ja= jo.getJSONArray("catalog");
        int status= 0;
        int failCount= 0;
        for ( int i=0; i<ja.length(); i++ ) {
            JSONObject jo1= ja.getJSONObject(i);
            String id= jo1.getString("id");
            CheckStatus st1= doCheck(id);
            if ( st1.getStatus()!=0 ) {
                logger.log(Level.INFO, "<img src=''../red.gif''> test returns fail status: {0}<br>", id);
                status=1;
                failCount++;
            }
        }
        CheckStatus result= new CheckStatus(status);
        result.setMessage("number of failures: "+failCount);
        return new CheckStatus(status);
    }
    
}
