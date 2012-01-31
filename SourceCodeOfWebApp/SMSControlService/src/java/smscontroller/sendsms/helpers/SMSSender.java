
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.sendsms.helpers;

import com.tcs.igrid.universal.helper.DBConnectionManager;
import com.tcs.igrid.universal.helper.DBHandler;
import java.net.URL;
import java.io.*;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Properties;


public class SMSSender {

    private String gatewayURL;
    private String stringBeforeMessageId;
    private String stringAfterMessageId;
    private String logGatewayMessageIdQuery = "log_gateway_message_id";

    public SMSSender (String gatewayPropertiesFileName) {

        InputStream gatewayPropertiesFile = null;
        Properties gatewayProperties = new Properties();
        
        try {
            gatewayPropertiesFile = this.getClass().getClassLoader()
                    .getResourceAsStream(gatewayPropertiesFileName);

            gatewayProperties.load(gatewayPropertiesFile);
            this.gatewayURL = gatewayProperties.getProperty("gatewayurl");
            this.stringBeforeMessageId = gatewayProperties.getProperty("stringBeforeMessageId");
            this.stringAfterMessageId = gatewayProperties.getProperty("stringAfterMessageId");

            if ( gatewayURL == null ) {
                throw new ExceptionInInitializerError("Required property " +
                                                      "gatewayurl missing");
            }
        } catch (Exception exc) {
            throw new ExceptionInInitializerError("Unable to read properties" +
                    "file " + gatewayPropertiesFileName);
        }
    }

    public boolean sendMessage(String mobileNumber, String message,String requestId) {

        URL messageURL = null;
        URLConnection connection = null;
        BufferedReader input = null;
        String response = "";

        try {
            String processURL = String.format(gatewayURL,
                                             mobileNumber,
                                             URLEncoder.encode(message,"UTF8"));

            messageURL = new URL(processURL);
            connection = messageURL.openConnection();
            input = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));

            String responseLine = null;
            while((responseLine = input.readLine())!=null){
                response = response + responseLine;
            }
            input.close();
            try{
            logGatewayMessageId(response,mobileNumber,requestId);
            }
            catch(Exception ex){
                // Do nothing   //
            }
            return true;
        } catch (Exception exc) {
            return false;
        }
    }

    private void logGatewayMessageId(String response,String mobileNumber,String requestId) {
        System.out.println("response= "+response);
        String messageId = null;        
        int startPoint = response.indexOf(stringBeforeMessageId) + stringBeforeMessageId.length();
        String start = response.substring(startPoint);
        int lastpoint = start.indexOf(stringAfterMessageId);        
        messageId = start.substring(0, lastpoint - 1);
        System.out.println("***messageId: "+messageId);
        if(messageId==null){
            messageId = "NOT AVAILABLE";
        }
        try{
        DBConnectionManager connection = new DBConnectionManager();
        DBHandler dbhandler = new DBHandler(connection.getConnection());
        dbhandler.execute(logGatewayMessageIdQuery,messageId,mobileNumber,requestId);
        }
        catch(Exception exc){
            System.out.println(exc);
        }
    }

}

