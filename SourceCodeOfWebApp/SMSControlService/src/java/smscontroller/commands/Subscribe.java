/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.commands;

import com.tcs.igrid.universal.helper.DBConnectionManager;
import com.tcs.igrid.universal.helper.DBHandler;
import java.sql.Connection;
import java.sql.ResultSet;


public class Subscribe extends Command {

    private String response = "Execution has not yet begun";


    public boolean execute(String mobileNumber, String command) {
        if (command == null) return false;
        String userNameToSubscribe = command;
        String subscriptionID = null;
        Connection connection = new DBConnectionManager()
                .getConnection();
        DBHandler handler = new DBHandler(connection, "subscription.properties");
        userNameToSubscribe = handler.replaceSQLMetaCharacters(userNameToSubscribe);
        ResultSet userExists = null;

        try {
            userExists = handler.fetch("check_subscription", mobileNumber);
            userExists.next();
            subscriptionID = userExists.getInt(1) + "";
        } catch (Exception exc) {
        }
        userExists = null;
        if (subscriptionID != null) {
            response = "This mobile number has already been registered with " +
                    "subscription ID " + subscriptionID;
            return true;
        }

        handler.execute("add_subscription", mobileNumber, userNameToSubscribe);

        try {
            userExists = handler.fetch("check_subscription", mobileNumber);
            userExists.next();
            subscriptionID = userExists.getInt(1) + "";
        } catch (Exception exc) {
        }
        userExists = null;

        response = "Thank you for subscribing. Your subscription ID  is: " +
                subscriptionID;
        return true;
    }

    public String getResponse() {
        return response;
    }

}
