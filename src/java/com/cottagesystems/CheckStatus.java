/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cottagesystems;

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
    
}