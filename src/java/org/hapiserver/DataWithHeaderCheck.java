
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
import org.json.JSONObject;

/**
 *
 * @author jbf
 */
public class DataWithHeaderCheck extends Check {
     
    public DataWithHeaderCheck( URL hapi ) {
        super(hapi,"HeaderWithData");
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
        try ( BufferedReader read= new BufferedReader( new InputStreamReader(data.openStream()) ) ) {
            String s;
            while ( ( s=read.readLine() )!=null ) {
                if ( actualFieldCount==-1 ) {
                    actualFieldCount= s.split(",").length;
                }
                b.append(s).append("\n");
                len+= 1;
            }
        }
        logger.log(Level.INFO, "Records received: {0}", len);
        if ( len>0 ) {
            if ( actualFieldCount!=nf ) {
                return new CheckStatus(2,"expected "+nf+" fields but got "+actualFieldCount );
            } else {
                return new CheckStatus(0);
            }
        } else {
            return new CheckStatus(1,"empty response");
        }
        
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
            int nf= 1;
            if ( arr.getJSONObject(1).has("size") ) {
                JSONArray size= arr.getJSONObject(1).getJSONArray("size");
                int p=1;
                for ( int j=0; j<size.length(); j++ ) p*= size.getInt(j);
                nf+= p;
            } else {
                nf+= 1;
            }
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
