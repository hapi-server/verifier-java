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
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * each Check is a implementation of this abstract class, calling the constructor
 * with the Check's name and implementing doCheck method.  The logger "logger"
 * can be used to send additional logging information for the check.
 * @author jbf
 */
public abstract class Check {
    
    URL hapi;
    String name;
    
    public static final Logger logger= Logger.getLogger("HapiVerifier");
    
    public Check( URL hapi, String name ) {
        this.hapi= hapi;
        this.name= name;
    }
    
    public static URL hapiURL( URL hapi, String name, Map<String,String> args ) {
        try {
            StringBuilder url= new StringBuilder(hapi.toString());
            url.append("/");
            url.append(name);
            if ( args!=null ) {
                boolean first= true;
                for ( Entry<String,String> arg: args.entrySet() ) {
                    if ( first ) {
                        url.append("?");
                        first= false;
                    } else {
                        url.append("&");
                    }
                    url.append(arg.getKey()).append("=").append(arg.getValue());
                }
            }
            return new URL( url.toString() );
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * return a JSONObject response from the server, for the URL.
     * @param url
     * @return the JSONObject
     * @throws org.json.JSONException 
     * @throws java.io.IOException 
     */
    public static JSONObject getJSONObject( URL url ) throws JSONException, IOException {
        logger.log(Level.INFO, "opening {0}", url);
        StringBuilder b= new StringBuilder();
        BufferedReader read= new BufferedReader( new InputStreamReader(url.openStream()) );
        String s;
        while ( ( s=read.readLine() )!=null ) {
            b.append(s).append("\n");
        }
        return new JSONObject(b.toString());
    }
    
    public String getName() {
        return this.name;
    }
    
    @Override
    public String toString() {
        return name + " " + hapi;
    }
    
    public abstract CheckStatus doCheck( ) throws Exception ;
}
