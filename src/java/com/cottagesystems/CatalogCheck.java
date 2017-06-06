/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cottagesystems;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Checks that a capabilities request returns a valid capabilities response.
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
        new JSONObject(b.toString());
        return new CheckStatus(0);
    }
}
