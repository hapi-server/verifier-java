
package org.hapiserver;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Many of these time utilities were copied from Das2, 
 * see https://saturn.physics.uiowa.edu/svn/das2/dasCore/community/autoplot2011/trunk/dasCoreDatum/src/org/das2/datum/DatumRangeUtil.java
 * @author jbf
 */
public class Util {
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
        
    
    private static Pattern TIME1, TIME2, TIME3, TIME4 ,TIME5, TIME6;
    static {
        String d= "[-:]"; // delim
        String i4= "(\\d\\d\\d\\d)";
        String i3= "(\\d+)";
        String i2= "(\\d\\d)";
        String tz= "((\\+|\\-)(\\d\\d)(:?(\\d\\d))?)"; // Note UTC allows U+2212 as well as dash.

        String iso8601time= i4 + d + i2 + d + i2 + "T" + i2 + d + i2 + "((" + d + i2 + "(\\." + i3 + ")?)?)Z?" ;  // "2012-03-27T12:22:36.786Z"
        String iso8601time2= i4 + i2 + i2 + "T" + i2 + i2 + "(" + i2 + ")?Z?" ;
        String iso8601time3= i4 + d + i3 + "T" + i2 + d + i2 + "(" + i2 + ")?Z?" ;
        String iso8601time4= i4 + d + i2 + d + i2 + "Z?" ;
        String iso8601time5= i4 + d + i3 + "Z?" ;
        String iso8601time6= i4 + d + i2 + d + i2 + "T" + i2 + d + i2 + "((" + d + i2 + "(\\." + i3 + ")?)?)"+tz+"?" ;  // "2014-09-02T10:55:10-05:00"
        TIME1= Pattern.compile(iso8601time);
        TIME2= Pattern.compile(iso8601time2);
        TIME3= Pattern.compile(iso8601time3);
        TIME4= Pattern.compile(iso8601time4);
        TIME5= Pattern.compile(iso8601time5);
        TIME6= Pattern.compile(iso8601time6);
    }
    
    /**
     * Parser for ISO8601 formatted times.
     * returns null or int[7]: [ Y, m, d, H, M, S, nano ]
     * The code cannot parse any iso8601 string, but this code should.  Right now it parses:
     * "2012-03-27T12:22:36.786Z"
     * "2012-03-27T12:22:36"
     * (and some others) TODO: enumerate and test.
     * TODO: this should use parseISO8601Datum.
     * @param str iso8601 string.
     * @return null or int[7]: [ Y, m, d, H, M, S, nano ]
     */
    public static int[] parseISO8601 ( String str ) {

        Matcher m;

        m= TIME1.matcher(str);
        if ( m.matches() ) {
            String sf= m.group(10);
            if ( sf!=null && sf.length()>9 ) throw new IllegalArgumentException("too many digits in nanoseconds part");
            int nanos= sf==null ? 0 : ( Integer.parseInt(sf) * (int)Math.pow( 10, ( 9 - sf.length() ) ) );
            return new int[] { Integer.parseInt( m.group(1) ), Integer.parseInt( m.group(2) ), Integer.parseInt( m.group(3) ), getInt( m.group(4), 0 ), getInt( m.group(5), 0 ), getInt( m.group(8), 0), nanos };
        } else {
            m= TIME2.matcher(str);
            if ( m.matches() ) {
                return new int[] { Integer.parseInt( m.group(1) ), Integer.parseInt( m.group(2) ), Integer.parseInt( m.group(3) ), getInt( m.group(4), 0 ), getInt( m.group(5), 0 ), getInt( m.group(7), 0), 0 };
            } else {
                m= TIME3.matcher(str);
                if ( m.matches() ) {
                    return new int[] { Integer.parseInt( m.group(1) ), 1, Integer.parseInt( m.group(2) ), getInt( m.group(3), 0 ), getInt( m.group(4), 0 ), getInt( m.group(5), 0), 0 };
                } else {
                    m= TIME4.matcher(str);
                    if ( m.matches() ) {
                        return new int[] { Integer.parseInt( m.group(1) ), Integer.parseInt( m.group(2) ), getInt( m.group(3), 0 ), 0, 0, 0, 0 };
                    } else {
                        m= TIME5.matcher(str);
                        if ( m.matches() ) {
                            return new int[] { Integer.parseInt( m.group(1) ), 1, Integer.parseInt( m.group(2) ), 0, 0, 0, 0 };
                        } else {
                            m= TIME6.matcher(str);
                            if ( m.matches() ) {
                                String sf= m.group(10);
                                if ( sf!=null && sf.length()>9 ) throw new IllegalArgumentException("too many digits in nanoseconds part");
                                int nanos= sf==null ? 0 : ( Integer.parseInt(sf) * (int)Math.pow( 10, ( 9 - sf.length() ) ) );
                                String plusMinus= m.group(12);
                                String tzHours= m.group(13);
                                String tzMinutes= m.group(15);
                                int[] result;
                                if ( plusMinus.charAt(0)=='+') {
                                    result= new int[] { Integer.parseInt( m.group(1) ), Integer.parseInt( m.group(2) ), Integer.parseInt( m.group(3) ), 
                                        getInt( m.group(4), 0 ) - getInt( tzHours,0 ), getInt( m.group(5), 0 )- getInt( tzMinutes, 0 ),
                                        getInt( m.group(8), 0), nanos };
                                } else {
                                    result= new int[] { Integer.parseInt( m.group(1) ), Integer.parseInt( m.group(2) ), Integer.parseInt( m.group(3) ), 
                                        getInt( m.group(4), 0 ) + getInt( tzHours,0 ), getInt( m.group(5), 0 ) + getInt( tzMinutes, 0 ),
                                        getInt( m.group(8), 0) , nanos };
                                }
                                return normalizeTimeComponents(result);
                            }
                        }
                    }
                }
            }
        }
        return null;
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

}
