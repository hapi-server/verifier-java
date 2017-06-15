
package org.hapiserver;

import static org.hapiserver.Check.hapiURL;
import java.net.URL;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author jbf
 */
public class InfoCheck extends Check {
    
    public InfoCheck( URL hapi ) {
        super(hapi,"info");
    }
    
    private CheckStatus doCheck(String id) throws Exception {
        URL info= hapiURL( hapi, "info", Collections.singletonMap( "id", id ) );
        JSONObject jo= getJSONObject(info);
        jo.getString("HAPI");
        //jo.getString("status"); //TEMPORARY
        jo.getString("startDate");
        jo.getString("stopDate");
        
        JSONArray ja= jo.getJSONArray("parameters");
        for ( int i=0; i<ja.length(); i++ ) {
            JSONObject jo1= ja.getJSONObject(i);
            jo1.getString("name");
            String type= jo1.getString("type");
            if ( type.equals("isotime") || type.equals("string" ) ) {
                jo1.getInt("length");
            }
            if ( type.equals("isotime") && !jo1.has("units") ) {
                throw new IllegalArgumentException("isotime should have units UTC" );
            }
            if ( !jo1.has("units") ) {
                throw new IllegalArgumentException("units missing");
            }
            
        }
        return new CheckStatus(0);        
    }
    
    @Override
    public CheckStatus doCheck() throws Exception {
        URL cat= hapiURL( hapi, "catalog", null );
        JSONObject jo= getJSONObject(cat);
        jo.getString("HAPI");
        jo.getString("status");
        JSONArray ja= jo.getJSONArray("catalog");
        for ( int i=0; i<ja.length(); i++ ) {
            JSONObject jo1= ja.getJSONObject(i);
            String id= jo1.getString("id");
            try {
                doCheck(id);
            } catch ( Exception ex ) {
                throw new IllegalStateException( "Exception with \""+id+"\": "+ex.getMessage(), ex );
            }
        }
        return new CheckStatus(0);
    }
    
}
