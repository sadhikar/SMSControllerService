/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.commands;

import smscontroller.receivesms.helpers.UserInfo;
import com.tcs.igrid.sendmailfrompojo.SendMailPojo;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * 
 */
public class EMail extends Command {

    private String recipientAddress;
    private String message;
    private String response = "Execution has not yet begun";
    private final String MAIL_SERVER;


    public EMail() throws IOException {
        InputStream eMailPropertiesFile = this.getClass().getClassLoader()
                .getResourceAsStream("emailserver.properties");
        Properties eMailProperties = new Properties();
        eMailProperties.load(eMailPropertiesFile);

        MAIL_SERVER = eMailProperties.getProperty("mail_server_url");
    }

    public boolean execute(String mobileNumber, String command) {
        if (command == null ) {
            response = "Insufficient Parameters";
            return false;
        }
        
        String[] commandParameters = command.trim().split("[ ]+",2);

        if (commandParameters.length != 2) {
            response = "Insufficient Parameters";
            return false;
        }
        recipientAddress = commandParameters[0];
        message = commandParameters[1];
        String userName =new UserInfo(mobileNumber).getUserName();
        userName = (userName == null) ? mobileNumber
                                      : userName;
        SendMailPojo sendMail = new SendMailPojo(MAIL_SERVER,
                                                 recipientAddress,
                                                 null,
                                                 null,
                                                 "Message from " + userName,
                                                 message,null);

        try {
            sendMail.sendMail();
            response = "Mail has been sent";
        } catch (Exception exc) {
        }
        return true;
    }

    public String getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return "email";
    }

}
