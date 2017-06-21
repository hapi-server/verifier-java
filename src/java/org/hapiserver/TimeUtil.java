
package org.hapiserver;

import java.text.ParseException;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Many of these time utilities were copied from Das2, 
 * see https://saturn.physics.uiowa.edu/svn/das2/dasCore/community/autoplot2011/trunk/dasCoreDatum/src/org/das2/datum/DatumRangeUtil.java
 * @author jbf
 */
public class TimeUtil {
    
    private static final String SIMPLE_FLOAT= "\\d?\\.?\\d+";
    public static final String ISO8601_DURATION= "P(\\d+Y)?(\\d+M)?(\\d+D)?(T(\\d+H)?(\\d+M)?("+SIMPLE_FLOAT+"S)?)?";
    public static final Pattern ISO8601_DURATION_PATTERN= Pattern.compile(ISO8601_DURATION);

    /**
     * returns a 7 element array with [year,mon,day,hour,min,sec,nanos].
     * copied from Das2.
     * @param stringIn
     * @return 7-element array with [year,mon,day,hour,min,sec,nanos]
     * @throws ParseException if the string does not appear to be valid.
     */
    public static int[] parseISO8601Duration( String stringIn ) throws ParseException {
        Matcher m= ISO8601_DURATION_PATTERN.matcher(stringIn);
        if ( m.matches() ) {
            double dsec=getDouble( m.group(7),0 );
            int sec= (int)dsec;
            int nanosec= (int)( ( dsec - sec ) * 1e9 );
            return new int[] { getInt( m.group(1), 0 ), getInt( m.group(2), 0 ), getInt( m.group(3), 0 ), getInt( m.group(5), 0 ), getInt( m.group(6), 0 ), sec, nanosec };
        } else {
            if ( stringIn.contains("P") && stringIn.contains("S") && !stringIn.contains("T") ) {
                throw new ParseException("ISO8601 duration expected but not found.  Was the T missing before S?",0);
            } else {
                throw new ParseException("ISO8601 duration expected but not found.",0);
            }
        }
    }
    
    private final static int[][] DAYS_IN_MONTH = {
        {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 0},
        {0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 0}
    };
        
    public static int daysInMonth(int month, int year) {
        return DAYS_IN_MONTH[isLeapYear(year)?1:0][month];
    }
    
    /**
     * return the leap year for years 1901-2099.
     * @param year
     * @return
     */
    public static boolean isLeapYear( int year ) {
        return (year % 4)==0 && ( year%400==0 || year%100!=0 );
    }
    
    /**
     * Normalize all the components, so no component is 
     * greater than its expected range or less than zero.
     * 
     * Note that leap seconds are not accounted for.  TODO: account for them.
     * @param components int[7]: [ Y, m, d, H, M, S, nano ]
     * @return the same array
     */
    public static int[] normalizeTimeComponents( int[] components ) {
        while ( components[6]>=1000000000 ) {
            components[5]+=1;
            components[6]-= 1000000000;
        }
        while ( components[5]>=60 ) { // TODO: leap seconds
            components[4]+= 1;
            components[5]-= 60;
        }
        while ( components[5]<0 ) { // TODO: leap seconds
            components[4]-= 1;
            components[5]+= 60;
        }
        while ( components[4]>=60 ) {
            components[3]+= 1;
            components[4]-= 60;
        }
        while ( components[4]<0 ) {
            components[3]-= 1;
            components[4]+= 60;
        }
        while ( components[3]>=23 ) {
            components[2]+= 1;
            components[3]-= 24;
        }
        while ( components[3]<0 ) {
            components[2]-= 1;
            components[3]+= 24;
        }
        // Irregular month lengths make it impossible to do this nicely.  Either 
        // months should be incremented or days should be incremented, but not
        // both.  Note Day-of-Year will be normalized to Year,Month,Day here
        // as well.  e.g. 2000/13/01 because we incremented the month.
        if ( components[2]>28 ) {  
            int daysInMonth= daysInMonth( components[1], components[0] );
            while ( components[2] > daysInMonth ) {
                components[2]-= daysInMonth;
                components[1]+= 1;
                if ( components[1]>12 ) break;
                daysInMonth= daysInMonth( components[1], components[0] );
            }
        }
        if ( components[2]==0 ) { // handle borrow when it is no more than one day.
            components[1]=- 1;
            if ( components[1]==0 ) {
                components[1]= 12;
                components[0]-= 1;
            }
            int daysInMonth= daysInMonth( components[1], components[0] );
            components[2]= daysInMonth;
        }
        while ( components[1]>12 ) {
            components[0]+= 1;
            components[1]-= 12;
        }
        if ( components[1]<0 ) { // handle borrow when it is no more than one year.
            components[0]+= 1;
            components[1]+= 12;
        }
        return components;
        
    }
    
    /**
     * new attempt to write a clean ISO8601 parser.  This should also parse 02:00
     * in the context of 2010-002T00:00/02:00.  This does not support 2-digit years, which
     * were removed in ISO 8601:2004.
     * 
     * @param str the ISO8601 string
     * @param result the datum, decomposed into [year,month,day,hour,minute,second,nano]
     * @param lsd -1 or the current position 
     * @return the lsd least significant digit
     */
    public static int parseISO8601Datum( String str, int[] result, int lsd ) {
        StringTokenizer st= new StringTokenizer( str, "-T:.Z+", true );
        Object dir= null;
        final Object DIR_FORWARD = "f";
        final Object DIR_REVERSE = "r";
        int want= 0;
        boolean haveDelim= false;
        boolean afterT= false;
        while ( st.hasMoreTokens() ) {
            char delim= ' ';
            if ( haveDelim ) {
                delim= st.nextToken().charAt(0);
                if ( delim=='T' ) afterT= true;
                if ( afterT && ( delim=='-' || delim=='+' ) ) { // Time offset
                    StringBuilder toff= new StringBuilder( String.valueOf(delim) );
                    while ( st.hasMoreElements() ) {
                        toff.append(st.nextToken());
                    }
                    int deltaHours= Integer.parseInt(toff.substring(0,3));
                    switch ( toff.length() ) {
                        case 6: 
                            result[3]-= deltaHours;
                            result[4]-= Math.signum(deltaHours) * Integer.parseInt(toff.substring(4) );
                            break;
                        case 5: 
                            result[3]-= deltaHours;
                            result[4]-= Math.signum(deltaHours) * Integer.parseInt(toff.substring(3) );
                            break;
                        case 3:
                            result[3]-= deltaHours;
                            break;
                        default:
                            throw new IllegalArgumentException("malformed time zone designator: "+str);
                    }
                    normalizeTimeComponents(result);
                    break;
                }
                if ( st.hasMoreElements()==false ) { // "Z"
                    break;
                }
            } else {
                haveDelim= true;
            }
            String tok= st.nextToken();
            if ( dir==null ) {
                switch (tok.length()) {
                    case 4:
                        // typical route
                        int iyear= Integer.parseInt( tok );
                        result[0]= iyear;
                        want= 1;
                        dir=DIR_FORWARD;
                        break;
                    case 6:
                        want= lsd;
                        if ( want!=6 ) throw new IllegalArgumentException("lsd must be 6");
                        result[want]= Integer.parseInt( tok.substring(0,2) );
                        want--;
                        result[want]= Integer.parseInt( tok.substring(2,4) );
                        want--;
                        result[want]= Integer.parseInt( tok.substring(4,6) );
                        want--;
                        dir=DIR_REVERSE;
                        break;
                    case 7:
                        result[0]= Integer.parseInt( tok.substring(0,4) );
                        result[1]= 1;
                        result[2]= Integer.parseInt( tok.substring(4,7) );
                        want= 3;
                        dir=DIR_FORWARD;
                        break;
                    case 8:
                        result[0]= Integer.parseInt( tok.substring(0,4) );
                        result[1]= Integer.parseInt( tok.substring(4,6) );
                        result[2]= Integer.parseInt( tok.substring(6,8) );
                        want= 3;
                        dir=DIR_FORWARD;
                        break;
                    default:
                        dir= DIR_REVERSE;
                        want= lsd;  // we are going to have to reverse these when we're done.
                        int i= Integer.parseInt( tok );
                        result[want]= i;
                        want--;
                        break;
                }
            } else if ( dir==DIR_FORWARD) {
                if ( want==1 && tok.length()==3 ) { // $j
                    result[1]= 1;
                    result[2]= Integer.parseInt( tok ); 
                    want= 3;
                } else if ( want==3 && tok.length()==6 ) {
                    result[want]= Integer.parseInt( tok.substring(0,2) );
                    want++;
                    result[want]= Integer.parseInt( tok.substring(2,4) );
                    want++;
                    result[want]= Integer.parseInt( tok.substring(4,6) );
                    want++;
                } else if ( want==3 && tok.length()==4 ) {
                    result[want]= Integer.parseInt( tok.substring(0,2) );
                    want++;
                    result[want]= Integer.parseInt( tok.substring(2,4) );
                    want++;
                } else {
                    int i= Integer.parseInt( tok );
                    if ( delim=='.' && want==6 ) {
                        int n= 9-tok.length();
                        result[want]= i * ((int)Math.pow(10,n));
                    } else {
                        result[want]= i;
                    }
                    want++;
                }
            } else if ( dir==DIR_REVERSE ) { // what about 1200 in reverse?
                int i= Integer.parseInt( tok ); 
                if ( delim=='.' ) {
                    int n= 9-tok.length();
                    result[want]= i * ((int)Math.pow(10,n));
                } else {
                    result[want]= i;
                }
                want--;
            }
        }
        
        if ( dir==DIR_REVERSE ) {
            int iu= want+1;
            int id= lsd;
            while( iu<id ) {
                int t= result[iu];
                result[iu]= result[id];
                result[id]= t;
                iu= iu+1;
                id= id-1;
            }
        } else {
            lsd= want-1;
        }
        
        return lsd;
    }

    
    /**
     * returns the time found in an ISO8601 string.  This supports
     * periods (durations) as in: 2007-03-01T13:00:00Z/P1Y2M10DT2H30M
     * Other examples:<ul>
     *   <li>2007-03-01T13:00:00Z/2008-05-11T15:30:00Z
     *   <li>2007-03-01T13:00:00Z/P1Y2M10DT2H30M
     *   <li>P1Y2M10DT2H30M/2008-05-11T15:30:00Z
     *   <li>2007-03-01T00:00Z/P1D
     *   <li>2012-100T02:00/03:45
     * </ul>
     * http://en.wikipedia.org/wiki/ISO_8601#Time_intervals
     * @param stringIn the time range
     * @return int[12], [ Y m d H M S nanos Y m d H M S nanos ]
     * @throws java.text.ParseException
     */
    public static int[] parseISO8601Range( String stringIn ) throws ParseException {

        String[] parts= stringIn.split("/",-2);
        if ( parts.length!=2 ) return null;

        boolean d1= parts[0].charAt(0)=='P'; // true if it is a duration
        boolean d2= parts[1].charAt(0)=='P';

        int[] digits0;
        int[] digits1;
        int lsd= -1;

        if ( d1 ) {
            digits0= parseISO8601Duration( parts[0] );
        } else {
            digits0= new int[7];
            lsd= parseISO8601Datum( parts[0], digits0, lsd );
            for ( int j=lsd+1; j<3; j++ ) digits0[j]=1; // month 1 is first month, not 0. day 1 
        }

        if ( d2 ) {
            digits1= parseISO8601Duration(parts[1]);
        } else {
            if ( d1 ) {
                digits1= new int[7];
            } else {
                digits1= Arrays.copyOf( digits0, digits0.length );
            }
            lsd= parseISO8601Datum( parts[1], digits1, lsd );
            for ( int j=lsd+1; j<3; j++ ) digits1[j]=1; // month 1 is first month, not 0. day 1 
        }

        if ( digits0==null || digits1==null ) return null;
        
        if ( d1 ) {
            for ( int i=0; i<7; i++ ) digits0[i] = digits1[i] - digits0[i];
        }

        if ( d2 ) {
            for ( int i=0; i<7; i++ ) digits1[i] = digits0[i] + digits1[i];
        }

        int[] result= new int[12];
        System.arraycopy( digits0, 0, result, 0, 6 );
        System.arraycopy( digits1, 0, result, 6, 6 );
        
        return result;

    }
    
    /**
     * Parser for ISO8601 formatted times.
     * returns null or int[7]: [ Y, m, d, H, M, S, nano ]
     * The code cannot parse any iso8601 string, but this code should.  Right now it parses:
     * "2012-03-27T12:22:36.786Z"
     * "2012-03-27T12:22:36"
     * (and some others) TODO: enumerate and test.
     * @param str iso8601 string.
     * @return null or int[7]: [ Y, m, d, H, M, S, nano ]
     */
    public static int[] parseISO8601 ( String str ) {
        int[] result= new int[7];
        int r= parseISO8601Datum( str, result, 0 );
        return result;
    }

    private static int getInt( String val, int deft ) {
        if ( val==null ) {
            if ( deft!=-99 ) return deft; else throw new IllegalArgumentException("bad digit");
        }
        int n= val.length()-1;
        if ( Character.isLetter( val.charAt(n) ) ) {
            return Integer.parseInt(val.substring(0,n));
        } else {
            return Integer.parseInt(val);
        }
    }
    
    private static double getDouble( String val, double deft ) {
        if ( val==null ) {
            if ( deft!=-99 ) return deft; else throw new IllegalArgumentException("bad digit");
        }
        int n= val.length()-1;
        if ( Character.isLetter( val.charAt(n) ) ) {
            return Double.parseDouble(val.substring(0,n));
        } else {
            return Double.parseDouble(val);
        }
    }
    
    /**
     * for convenience, this formats the decomposed time.
     * @param result seven-element time [ Y,m,d,H,M,S,nanos ] 
     * @return formatted time
     */
    public static String formatISO8601Datum( int[] result ) {
        if ( result[6]!=0 ) {
            return String.format( "%04d-%02d-%02dT%02d:%02d:%02d.%09dZ", result[0], result[1], result[2], result[3], result[4], result[5], result[6] );
        } else if ( result[5]!=0 ) {
            return String.format( "%04d-%02d-%02dT%02d:%02d:%02dZ", result[0], result[1], result[2], result[3], result[4], result[5] );
        } else {
            return String.format( "%04d-%02d-%02dT%02d:%02dZ", result[0], result[1], result[2], result[3], result[4] );
        }
    }
    
    /**
     * for convenience, this formats the decomposed time range.
     * @param result seven-element time [ Y,m,d,H,M,S,nanos, Y,m,d,H,M,S,nanos ] 
     * @return formatted time range
     */
    public static String formatISO8601Range( int[] result ) {
        String t0= formatISO8601Datum( java.util.Arrays.copyOfRange( result, 0,7 ) );
        String t1= formatISO8601Datum( java.util.Arrays.copyOfRange( result, 7,7 ) );
        if ( t0.substring(0,10).equals(t1.substring(0,10) ) ) {
            return t0 + "/" + t1.substring(11);
        } else {
            return t0 + "/" + t1;
        }
    }

}
