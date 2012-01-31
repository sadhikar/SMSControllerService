/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smssenderpojo.helpers;

import java.io.Serializable;
import java.util.Set;


public class SMSMessage implements Serializable {

    private Set<String> mobileNumbers;
    private String message;

    public SMSMessage(Set<String> mobileNumbers, String message ) {
        this.mobileNumbers = mobileNumbers;
        this.message = message;
    }

    public Set<String> getMobileNumbers() {
        return mobileNumbers;
    }

    public String getMessage() {
        return message;
    }

    public int getSize() {
        return this.mobileNumbers.size();
    }
    
}
