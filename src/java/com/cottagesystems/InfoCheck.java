/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cottagesystems;

import static com.cottagesystems.Check.hapiURL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
        StringBuilder b= new StringBuilder();
        BufferedReader read= new BufferedReader( new InputStreamReader(info.openStream()) );
        String s;
        while ( ( s=read.readLine() )!=null ) {
            b.append(s).append("\n");
        }
        JSONObject jo= new JSONObject(b.toString());
        jo.getString("HAPI");
        //jo.getString("status");
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
            String units= jo1.getString("units");
            if ( type.equals("isotime") ) {
                if ( !units.equals("UTC") ) {
                    throw new IllegalArgumentException("isotime should have units UTC");
                }
            }
        }
        return new CheckStatus(0);        
    }
    
    @Override
    public CheckStatus doCheck() throws Exception {
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
            String id= jo1.getString("id");
            doCheck(id);
        }
        return new CheckStatus(0);
    }
    
}
