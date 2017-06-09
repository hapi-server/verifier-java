
package org.hapiserver;

import java.io.IOException;
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
        URL cat= hapiURL( hapi, "catalog", null );
        JSONObject jo= getJSONObject(cat);
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
