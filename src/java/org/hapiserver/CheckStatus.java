
package org.hapiserver;

/**
 *
 * @author jbf
 */
public class CheckStatus {
    
    private int status = 0;
    private String message = "";
    
    public CheckStatus( int status ) {
        this.status= status;
    }

    public CheckStatus( int status, String message ) {
        this.status= status;
        this.message= message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
    
    private String log = "";

    public String getLog() {
        return log;
    }

    /** 
     * set the log associated with the test--note that this is set externally
     * and the Check should not call this.
     * @param log 
     */
    public void setLog(String log) {
        this.log = log;
    }

    private long timeStamp = 0;

    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * age in the number of milliseconds since 1970-01-01T00:00.  
     * (See Date.getTime()).
     * @param timeStamp 
     */
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    
}
