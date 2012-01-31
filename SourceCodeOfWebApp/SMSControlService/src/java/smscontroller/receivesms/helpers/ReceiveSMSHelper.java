/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.receivesms.helpers;

import smscontroller.exceptions.ValidationFailedException;
import smscontroller.commands.Command;
import smscontroller.receivesms.exceptions.InvalidCommandException;
import com.tcs.igrid.universal.helper.DBConnectionManager;
import com.tcs.igrid.universal.helper.DBHandler;
import com.tcs.igrid.universal.helper.IPVerifier;
import java.sql.Connection;
import java.sql.ResultSet;
import javax.servlet.ServletConfig;


public class ReceiveSMSHelper {

    private final String FAILED;
    private final String FROZEN;
    private final String INVALID_COMMAND;
    private final String PERMISSION_DENIED;
    private final String COMMAND_EXECUTION_FAILED;
    
    private String frozenCheckQueryName;
    private String loggerQueryName;
    private String ipVerificationQueryName;
    private String secureCheckQueryName;
    private String permissionCheckQueryName;
    
    private Connection dbConnection;
    private DBHandler dbHandler;

    public ReceiveSMSHelper(ServletConfig config) {
        this.FAILED =
                config.getInitParameter("default_response");
        this.FROZEN =
                config.getInitParameter("frozen_response");
        this.INVALID_COMMAND =
                config.getInitParameter("invalid_command_response");
        this.PERMISSION_DENIED =
                config.getInitParameter("permission_denied_response");
        this.COMMAND_EXECUTION_FAILED =
                config.getInitParameter("command_execution_failed_response");
        this.frozenCheckQueryName =
                config.getInitParameter("frozen_check_query_name");
        this.loggerQueryName =
                config.getInitParameter("logger_query_name");
        this.ipVerificationQueryName =
                config.getInitParameter("ip_verification_query_name");
        this.secureCheckQueryName =
                config.getInitParameter("secure_check_query_name");
        this.permissionCheckQueryName =
                config.getInitParameter("permission_check_query_name");

        dbConnection = new DBConnectionManager().getConnection();
        dbHandler = new DBHandler(dbConnection);
    }

    public String processRequest(String mobileNumber, 
                                 String message,
                                 String requesterAddress) {

        String responseMessage = FAILED;
        String[] messageComponent = null;
        String userCommand=null;
        String userCommandParameters = null;
        String commandClassName = null;
        ResultSet frozenCheck = null;
        boolean frozen=false;

        ResultSet secureCheck = null;
        boolean secureFlag = false;

        ResultSet permissionCheck = null;
        boolean userAllowed=false;
        
        Command command = null;
        
        try {

            IPVerifier ipVerifier = new IPVerifier(dbConnection,
                                                   ipVerificationQueryName,
                                                   requesterAddress);

            if (!ipVerifier.isIPAllowed()) {
                throw new ValidationFailedException(FAILED);
            }

            if (message == null || message.trim().equals(""))
                throw new ValidationFailedException(INVALID_COMMAND);

            message=dbHandler.replaceSQLMetaCharacters(message);
            messageComponent = message.split("[ ]+", 2);
            userCommand = messageComponent[0].trim().toLowerCase();
            

            if (messageComponent.length > 1 )
                userCommandParameters = messageComponent[1].trim();

            commandClassName =
                    new ClassNameResolver().resolveClassName(userCommand);

            if (commandClassName == null) {
                throw new ValidationFailedException(INVALID_COMMAND);
            }

            frozenCheck = dbHandler.fetch(frozenCheckQueryName,
                                                    userCommand);

            if (frozenCheck == null) {
                throw new ValidationFailedException(FROZEN);
            }

            try {
                    frozenCheck.next();
                    frozen = (frozenCheck.getInt(1)==0)
                             ? false
                             : true;
            } catch (Exception exc) {
                throw new ValidationFailedException(FROZEN);
            }
            if (frozen)
                throw new ValidationFailedException(FROZEN);


            secureCheck = dbHandler.fetch(secureCheckQueryName,
                                          userCommand);

            if (secureCheck == null) {
                throw new ValidationFailedException(PERMISSION_DENIED);
            }

            try {
                    secureCheck.next();
                    secureFlag = (secureCheck.getInt(1)==0)
                                 ? false
                                 : true;
            } catch (Exception exc) {
                throw new ValidationFailedException(PERMISSION_DENIED);
            }

            if (secureFlag) {
                permissionCheck = dbHandler.fetch(permissionCheckQueryName,
                        userCommand, mobileNumber);

                if (permissionCheck == null) {
                    throw new ValidationFailedException(PERMISSION_DENIED);
                }

                try {
                        permissionCheck.next();
                        userAllowed = permissionCheck.getInt(1) > 0 ? true
                                                                    : false;
                } catch (Exception exc) {
                    throw new ValidationFailedException(PERMISSION_DENIED);
                }

                if (!userAllowed)
                    throw new ValidationFailedException(PERMISSION_DENIED);

            }
                


            try {
                command = Command.getInstance(commandClassName);
                boolean executed = command.execute(mobileNumber,
                                                   userCommandParameters);
                responseMessage = (executed ) ? command.getResponse()
                                              : COMMAND_EXECUTION_FAILED;
            } catch (InvalidCommandException ince) {
                throw new ValidationFailedException(INVALID_COMMAND);
            } //end of try block
            
            

        } catch (ValidationFailedException vfe) {
            responseMessage = vfe.getMessage();
        } catch (Exception exc) {
            responseMessage = FAILED;
        } finally {
            dbHandler.execute(loggerQueryName,
                              mobileNumber,
                              message,
                              responseMessage);
            try {
                if (dbConnection != null) dbConnection.close();
            } catch (Exception exc) {}
        }
        return responseMessage;
    }


}
