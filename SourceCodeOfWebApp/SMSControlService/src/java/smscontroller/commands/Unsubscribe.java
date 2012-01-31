/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.commands;

import com.tcs.igrid.universal.helper.DBConnectionManager;
import com.tcs.igrid.universal.helper.DBHandler;
import java.sql.Connection;


public class Unsubscribe extends Command {
    private String response = "Execution has not yet begun";


    public boolean execute(String mobileNumber, String command) {
        Connection connection = new DBConnectionManager().getConnection();
        boolean executed=false;
        DBHandler handler = new DBHandler(connection, "unsubscribe.properties");
        try {
            executed = handler.execute("remove_subscription", mobileNumber);
        } catch (Exception exc) {
        }
        if (executed) {
            response = "Unsubscribed";
        } else {
            response = "User does not exist";
        }
        return true;
    }

    public String getResponse() {
        return response;
    }

}
