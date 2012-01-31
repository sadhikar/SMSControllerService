/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.receivesms.helpers;

import com.tcs.igrid.universal.helper.DBConnectionManager;
import com.tcs.igrid.universal.helper.DBHandler;
import java.sql.Connection;
import java.sql.ResultSet;


public class UserInfo {

    private String mobileNumber;
    private String userName;

    public UserInfo(String mobileNumber) {
        this.mobileNumber= mobileNumber;
    }

    public String getUserName() {
        try {
            Connection connection = new DBConnectionManager().getConnection();
            DBHandler handler = new DBHandler(connection,"userinfo.properties");
            ResultSet result = handler.fetch("fetch_commanding_user_info",
                                             mobileNumber);
            result.next();
            userName = result.getString(1);
        } catch (Exception exc) {
        }
        return userName;
    }


}
