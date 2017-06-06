
package com.cottagesystems;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Checks that a catalog request returns a valid catalog response.
 * @author jbf
 */
public class CatalogCheck extends Check {
    
    public CatalogCheck( URL hapi ) {
        super(hapi,"catalog");
    }
    
    @Override
    public CheckStatus doCheck( ) throws MalformedURLException, IOException, JSONException {
        URL cap= hapiURL( hapi, "catalog", null );
        StringBuilder b= new StringBuilder();
        BufferedReader read= new BufferedReader( new InputStreamReader(cap.openStream()) );
        String s;
        while ( ( s=read.readLine() )!=null ) {
            b.append(s).append("\n");
        }
        JSONObject jo= new JSONObject(b.toString());
        jo.getString("HAPI");
        jo.getString("status");
        JSONArray ja= jo.getJSONArray("catalog");
        for ( int i=0; i<ja.length(); i++ ) {
            JSONObject jo1= ja.getJSONObject(i);
            jo1.getString("id");
        }
        return new CheckStatus(0);
    }
}
