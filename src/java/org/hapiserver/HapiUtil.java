/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hapiserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * utilities for all classes.
 * @author jbf
 */
public class HapiUtil {

    private static final Logger logger= Logger.getLogger("HapiVerifier");
        
    /**
     * approximate the cadence specified as a duration in seconds.
     * @param s
     * @return
     * @throws ParseException 
     */
    private static double cadenceSeconds( String s ) throws ParseException {
        int[] ss= TimeUtil.parseISO8601Duration(s);
        return ss[0]*86400*365 + ss[1]*86400*30 + ss[2]*86400 + ss[3]*3600 + ss[4]*60 + ss[5] + ss[6]/1e9;
    }

    
    /**
     * 
     * @param info
     * @return
     * @throws IllegalArgumentException
     * @throws JSONException 
     */
    public static String[] getSampleRange(JSONObject info) throws IllegalArgumentException, JSONException {
        String[] sampleRange=null;
        
        String startDate= info.getString("startDate");
        String stopDate= info.getString("stopDate");
        
        int[] istartDate= TimeUtil.parseISO8601(startDate);
        int[] istopDate= TimeUtil.parseISO8601(stopDate);
        
        if ( info.has("sampleStartDate") && info.has("sampleStopDate") ) {
            sampleRange = new String[] { info.getString("sampleStartDate"), info.getString("sampleStopDate") } ;
        }
        
        if ( sampleRange==null ) {
            if ( info.has("cadence") ) {
                try{
                    System.arraycopy(istopDate, 0, istartDate, 0, 6);
                    double cs= cadenceSeconds(info.getString("cadence"));
                    if ( cs<1. ) {
                        istartDate[4]-=1;
                    } else if ( cs<60. ) {
                        istartDate[3]-=1;
                    } else if ( cs<3600. ) {
                        istartDate[2]-=1;
                    } else {
                        istartDate[2]-=1;
                    }
                } catch ( ParseException ex ) {
                    logger.log(Level.WARNING, "parse error in cadence: {0}", info.getString("cadence"));
                }
            } else {
                System.arraycopy(istopDate, 0, istartDate, 0, 6);
                istartDate[2]-=1;
            }
            istartDate= TimeUtil.normalizeTimeComponents(istartDate);
            return new String[] { TimeUtil.formatISO8601Datum(istartDate), TimeUtil.formatISO8601Datum(istopDate) };
        } else {
            return sampleRange;
        }
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
     * return the duration in a easily-human-consumable form.
     * @param dt the duration in milliseconds.
     * @return a duration like "2.6 hours"
     */
    public static String getDurationForHumans( long dt ) {
        if ( dt<2*1000 ) {
            return dt+" milliseconds";
        } else if ( dt<2*60000 ) {
            return String.format( Locale.US, "%.1f",dt/1000.)+" seconds";
        } else if ( dt<2*3600000 ) {
            return String.format( Locale.US, "%.1f",dt/60000.)+" minutes";
        } else if ( dt<2*86400000 ) {
            return String.format( Locale.US, "%.1f",dt/3600000.)+" hours";
        } else {
            return String.format( Locale.US, "%.1f",dt/86400000.)+" days";
        }
    }
    
    public static String makeHtml( String raw ) {
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

}
