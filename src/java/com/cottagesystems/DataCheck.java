
package com.cottagesystems;

import static com.cottagesystems.Check.hapiURL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * verify data request for example times.
 * @author jbf
 */
public class DataCheck extends Check {
    
    public DataCheck( URL hapi ) {
        super(hapi,"data");
    }
    
    private CheckStatus doCheck( String id, String min, String max ) throws Exception {
        Map<String,String> params= new LinkedHashMap<>();
        params.put( "id", id );
        params.put( "time.min", min );
        params.put( "time.max", max );
        URL data= hapiURL( hapi, "data", params );
        logger.log(Level.INFO, "opening {0}", data);
        
        StringBuilder b= new StringBuilder();
        try ( BufferedReader read= new BufferedReader( new InputStreamReader(data.openStream()) ) ) {
            String s;
            while ( ( s=read.readLine() )!=null ) {
                b.append(s).append("\n");
            }
        }
        
        return new CheckStatus(0);
    }
    
    private CheckStatus doCheck(String id) throws Exception {
        URL info= hapiURL( hapi, "info", Collections.singletonMap( "id", id ) );
        logger.log(Level.INFO, "opening {0}", info);
        StringBuilder b= new StringBuilder();
        try ( BufferedReader read= new BufferedReader( new InputStreamReader(info.openStream()) ) ) {
            String s;
            while ( ( s=read.readLine() )!=null ) {
                b.append(s).append("\n");
            }
        }
        JSONObject jo= new JSONObject(b.toString());
        jo.getString("HAPI");
        //jo.getString("status"); //TEMPORARY
        //String startDate= jo.getString("startDate");
        //String stopDate= jo.getString("stopDate");
        
        String sampleStartDate;
        String sampleStopDate;
        if ( jo.has("sampleStopDate") ) {
            sampleStopDate= jo.getString("sampleStopDate");
        } else {
            logger.log(Level.INFO, "test could not be performed for {0} from {1}", new Object[] { id, hapi } );
            return new CheckStatus(0);
        }
        
        if ( jo.has("sampleStartDate") ) {
            sampleStartDate= jo.getString("sampleStartDate");
        } else {
            logger.log(Level.INFO, "test could not be performed {0} from {1}", new Object[] { id, hapi } );
            return new CheckStatus(0);
        }
        
        return doCheck( id, sampleStartDate, sampleStopDate );
        
    }
    
    @Override
    public CheckStatus doCheck() throws Exception {
        URL catalog= hapiURL( hapi, "catalog", null );
        logger.log(Level.INFO, "opening {0}", catalog);
        StringBuilder b= new StringBuilder();
        try ( BufferedReader read= new BufferedReader( new InputStreamReader(catalog.openStream()) ) ) {
            String s;
            while ( ( s=read.readLine() )!=null ) {
                b.append(s).append("\n");
            }
        }
        JSONObject jo= new JSONObject(b.toString());
        jo.getString("HAPI");
        jo.getString("status");
        JSONArray ja= jo.getJSONArray("catalog");
        for ( int i=0; i<ja.length(); i++ ) {
            JSONObject jo1= ja.getJSONObject(i);
            String id= jo1.getString("id");
            doCheck(id);
        }
        return new CheckStatus(0);
    }
    
}
