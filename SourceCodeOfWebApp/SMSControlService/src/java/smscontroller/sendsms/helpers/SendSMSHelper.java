/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.sendsms.helpers;

import smscontroller.exceptions.ValidationFailedException;
import com.tcs.igrid.smssenderpojo.helpers.SMSMessage;
import com.tcs.igrid.universal.helper.DBConnectionManager;
import com.tcs.igrid.universal.helper.DBHandler;
import com.tcs.igrid.universal.helper.IPVerifier;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;


public class SendSMSHelper {

    private static int MAX_CONCURRENT_MESSAGESERVER_HITS;
    private static int DELAY_BETWEEN_SUCCESSIVE_REQUESTGROUP_MS;

    private static String SENDING_FAILED_INVALID_IP;
    private static String SENDING_FAILED_FROZEN ;
    private static String SENDING_FAILED_INVALID_PARAMS;
    private static String SENDING_FAILED_BLANK_MESSAGE;
    private static String SENDING_FAILED_UNKNOWN;

    private static String SENDING_FAILED_UNSUBSCRIBED;
    private static String SENDING_SUCCESSFUL;

    private static String subscriptionCheckQueryName;
    private static String fetchRequestFromPoolQueryName;
    private static String ipVerificationQueryName;
    private static String frozenFieldCheckQueryName;
    private static String addNewRequestQueryName;
    private static String addAllRecipientsQueryName;
    private static String updateSingleCompletionQueryName;
    private static String updateRequestCompletionQueryName;

    private static String smsGatewayDetailsPropertiesFile;
    private static String fetchOldestMobileNoQueryName;
    private static String loggerQueryName;
    private static Connection dbConnection;
    private static DBHandler handler;
    private static int counter;

    private static boolean processing=false;

    private static Properties queries = new Properties();

    private PreparedStatement addAllRowsOfNewRequest = null;
    private static PreparedStatement subscriptionCheck = null;
    private static PreparedStatement updateEachCompletion = null;
    private static PreparedStatement fetchOldestUnprocessedMobileNo = null;
    private static PreparedStatement fetchOldestRequestInPool = null;
    private static PreparedStatement updateRequestCompletion = null;
    private static ServletConfig servletConfig;

    public static void init(ServletConfig servletConfig) {

        SendSMSHelper.servletConfig = servletConfig;

        SENDING_FAILED_INVALID_IP =
                servletConfig.getInitParameter("sending_failed_invalid_ip");
        SENDING_FAILED_FROZEN =
                servletConfig.getInitParameter("sending_failed_frozen_field");
        SENDING_FAILED_UNSUBSCRIBED =
                servletConfig.getInitParameter("sending_failed_unsubscribed");
        SENDING_FAILED_INVALID_PARAMS =
                servletConfig.getInitParameter("sending_failed_invalid_params");
        SENDING_FAILED_BLANK_MESSAGE =
                servletConfig.getInitParameter("sending_failed_blank_message");
        SENDING_FAILED_UNKNOWN =
                servletConfig.getInitParameter("sending_failed_unknown");
        SENDING_SUCCESSFUL =
                servletConfig.getInitParameter("sending_successful");



        subscriptionCheckQueryName =
                servletConfig.getInitParameter("sendsms_subscription_check_query_name");
        smsGatewayDetailsPropertiesFile =
                servletConfig.getInitParameter("sms_gateway_details_file_name");
        loggerQueryName = servletConfig.getInitParameter("logger_query_name");
        fetchRequestFromPoolQueryName =
                servletConfig.getInitParameter("fetch_oldest_request_query_name");
        fetchOldestMobileNoQueryName =
                servletConfig.getInitParameter("fetch_oldest_mobile_query_name");

        ipVerificationQueryName =
                servletConfig.getInitParameter("ip_verification_query_name");
        frozenFieldCheckQueryName =
                servletConfig.getInitParameter("sendsms_frozen_check_query_name");

        addNewRequestQueryName =
                servletConfig.getInitParameter("add_new_request_query_name");
        addAllRecipientsQueryName =
                servletConfig.getInitParameter("add_all_recipients_query_name");
        updateSingleCompletionQueryName =
                servletConfig.getInitParameter("update_single_completion_query_name");
        updateRequestCompletionQueryName =
                servletConfig.getInitParameter("update_request_completion_query_name");

        MAX_CONCURRENT_MESSAGESERVER_HITS = Integer.parseInt(
                servletConfig.getInitParameter("max_concurrent_smsserver_hits"));

        DELAY_BETWEEN_SUCCESSIVE_REQUESTGROUP_MS = Integer.parseInt(
                servletConfig.getInitParameter("delay_between_successive_reqgroup"));

        counter = MAX_CONCURRENT_MESSAGESERVER_HITS;
        try {
            getConnectionAndPrepareStatements();
        } catch (Exception exc) {
            exc.printStackTrace();
            throw new ExceptionInInitializerError(exc.getMessage());
        }

        process();

        
    }

    private static void getConnectionAndPrepareStatements() throws Exception {
            dbConnection = new DBConnectionManager().getConnection();
            handler = new DBHandler(dbConnection);

            String queriesPropertiesFileName =
                    servletConfig.getInitParameter("queries_properties_file_name");
            InputStream queriesFile = SendSMSHelper.class.getClassLoader()
                    .getResourceAsStream(queriesPropertiesFileName);

            queries.load(queriesFile);

            subscriptionCheck = dbConnection.prepareStatement(
                    queries.getProperty(subscriptionCheckQueryName));

            updateEachCompletion = dbConnection.prepareStatement(
                    queries.getProperty(updateSingleCompletionQueryName));

            //System.out.println(queries.getProperty(fetchOldestUnprocessedMobileNoQuery));
            fetchOldestUnprocessedMobileNo = dbConnection.prepareStatement(
                    queries.getProperty(fetchOldestMobileNoQueryName));

            fetchOldestRequestInPool = dbConnection.prepareStatement(
                    queries.getProperty(fetchRequestFromPoolQueryName));

            updateRequestCompletion = dbConnection.prepareStatement(
                    queries.getProperty(updateRequestCompletionQueryName));
    }

    public static void destroy() {
        try {
            dbConnection.close();
        } catch (Exception exc) {
            
        }
    }

    
    public static synchronized boolean isProcessing() {
        return processing;
    }

    private static int getCountFromRequestID(String requestID) {
        String count = 
               requestID.substring(requestID.indexOf('_') +1);
        return Integer.parseInt(count);
    }

    public static void process() {

        int count = 0;
        synchronized (SendSMSHelper.class) {
            processing = true;
        }
        
        String requestID = null;
        String message = null;
        ResultSet newRequest = null;
        boolean triedRepoeningConnection = false;

        while(true) {

            try {
                newRequest = fetchOldestRequestInPool.executeQuery();
                if (!newRequest.next()) {
                    synchronized (SendSMSHelper.class) {
                        processing = false;
                    }
                    break;
                }
                triedRepoeningConnection = false;

                requestID = newRequest.getString("request_id");
                message = newRequest.getString("message");
                newRequest = null;

                ResultSet allEntriesPerRequest = null;
                fetchOldestUnprocessedMobileNo.setString(1, requestID);
                allEntriesPerRequest =
                        fetchOldestUnprocessedMobileNo.executeQuery();

                while(allEntriesPerRequest.next()) {
                    count++;
                    try {
                        String mobileNumber = allEntriesPerRequest.getString("to_number");
                        String status = validateAndSendSMS(mobileNumber, message,requestID);
                        updateEachCompletion.setString(1, status);
                        updateEachCompletion.setString(2, mobileNumber);
                        updateEachCompletion.setString(3, requestID);
                        updateEachCompletion.executeUpdate();
                    } catch (Exception exc) {

                    }
                }

                updateRequestCompletion.setInt(1, 1);
                updateRequestCompletion.setInt(2, 0);
                updateRequestCompletion.setString(3, requestID);
                updateRequestCompletion.executeUpdate();

                allEntriesPerRequest = null;
                message = null;
                requestID = null;

            } catch (Exception exc) {
                exc.printStackTrace();
                if (!triedRepoeningConnection) {
                    try {
                        getConnectionAndPrepareStatements();
                        triedRepoeningConnection = true;
                    } catch (Exception exce) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return;
    }

    public static void processByPassSubscription() {

        int count = 0;
        synchronized (SendSMSHelper.class) {
            processing = true;
        }

        String requestID = null;
        String message = null;
        ResultSet newRequest = null;
        boolean triedRepoeningConnection = false;

        while(true) {

            try {
                newRequest = fetchOldestRequestInPool.executeQuery();
                if (!newRequest.next()) {
                    synchronized (SendSMSHelper.class) {
                        processing = false;
                    }
                    break;
                }
                triedRepoeningConnection = false;

                requestID = newRequest.getString("request_id");
                message = newRequest.getString("message");
                newRequest = null;

                ResultSet allEntriesPerRequest = null;
                fetchOldestUnprocessedMobileNo.setString(1, requestID);
                allEntriesPerRequest =
                        fetchOldestUnprocessedMobileNo.executeQuery();

                while(allEntriesPerRequest.next()) {
                    count++;
                    try {
                        String mobileNumber = allEntriesPerRequest.getString("to_number");
                        String status = validateAndSendSMSByPassSubscription(mobileNumber, message,requestID);
                        System.out.println(status);
                        updateEachCompletion.setString(1, status);
                        updateEachCompletion.setString(2, mobileNumber);
                        updateEachCompletion.setString(3, requestID);
                        updateEachCompletion.executeUpdate();
                    } catch (Exception exc) {

                    }
                }

                updateRequestCompletion.setInt(1, 1);
                updateRequestCompletion.setInt(2, 0);
                updateRequestCompletion.setString(3, requestID);
                updateRequestCompletion.executeUpdate();

                allEntriesPerRequest = null;
                message = null;
                requestID = null;

            } catch (Exception exc) {
                exc.printStackTrace();
                if (!triedRepoeningConnection) {
                    try {
                        getConnectionAndPrepareStatements();
                        triedRepoeningConnection = true;
                    } catch (Exception exce) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return;
    }

    public void addRequest(String uniqueRequestID,
                           String requesterAddress,
                           SMSMessage messageComponent ) {

        int newRequestStatus = 0;
        int processed = 0;
        Connection connection  = new DBConnectionManager().getConnection();
        DBHandler dbHandler = new DBHandler(connection);
        String message = messageComponent.getMessage();

        try {
            addAllRowsOfNewRequest = connection.prepareStatement(
                queries.getProperty(addAllRecipientsQueryName));

            if (message == null || message.trim().equals("")) {
                throw new ValidationFailedException(SENDING_FAILED_BLANK_MESSAGE);
            }

            message=dbHandler.replaceSQLMetaCharacters(message);

            IPVerifier ipVerifier = new IPVerifier(connection,
                                           ipVerificationQueryName,
                                           requesterAddress);

            if (  !ipVerifier.isIPAllowed() ) {
                throw new ValidationFailedException(SENDING_FAILED_INVALID_IP);
            }

            ResultSet frozenCheck = dbHandler.fetch(frozenFieldCheckQueryName);
            boolean frozen = false;

            if ( frozenCheck == null ) {
                throw new ValidationFailedException(SENDING_FAILED_FROZEN);
            }

            try {
                frozenCheck.next();
                frozen = (frozenCheck.getInt(1) == 0)
                         ? false
                         : true;
            } catch (Exception exc) {
                throw new ValidationFailedException(SENDING_FAILED_FROZEN);
            }
            frozenCheck = null;

            if (frozen) {
                throw new ValidationFailedException(SENDING_FAILED_FROZEN);
            }
            
            
        } catch (ValidationFailedException exc) {
            newRequestStatus = Integer.parseInt(exc.getMessage());
            processed=1;
        } catch (Exception exc) {
            newRequestStatus = 99;
            processed=1;
        } finally {
            dbHandler.execute(addNewRequestQueryName,
                              uniqueRequestID,
                              requesterAddress,
                              message,
                              processed,
                              newRequestStatus);
        }
        if (processed == 0 ) {
            Runtime.getRuntime().gc();
            Set<String> mobileNumbers = messageComponent.getMobileNumbers();
            for (String mobileNumber : mobileNumbers ) {
                try {
                    addAllRowsOfNewRequest.setString(1, mobileNumber);
                    addAllRowsOfNewRequest.setString(2, uniqueRequestID);
                    addAllRowsOfNewRequest.executeUpdate();
                } catch (Exception exc) {

                }
            }
            addAllRowsOfNewRequest = null;
            mobileNumbers = null;

            try {
                connection.close();
            } catch (Exception exc) {

            }
        }

        if (!isProcessing()) {
            process();
        }

    }

    public void addRequestDndByPassSubcription(String uniqueRequestID,
                           String requesterAddress,
                           SMSMessage messageComponent ) {

        int newRequestStatus = 0;
        int processed = 0;
        Connection connection  = new DBConnectionManager().getConnection();
        DBHandler dbHandler = new DBHandler(connection);
        String message = messageComponent.getMessage();

        try {
            addAllRowsOfNewRequest = connection.prepareStatement(
                queries.getProperty(addAllRecipientsQueryName));

            if (message == null || message.trim().equals("")) {
                throw new ValidationFailedException(SENDING_FAILED_BLANK_MESSAGE);
            }

            message=dbHandler.replaceSQLMetaCharacters(message);

            IPVerifier ipVerifier = new IPVerifier(connection,
                                           ipVerificationQueryName,
                                           requesterAddress);

            if (  !ipVerifier.isIPAllowed() ) {
                throw new ValidationFailedException(SENDING_FAILED_INVALID_IP);
            }

            ResultSet frozenCheck = dbHandler.fetch(frozenFieldCheckQueryName);
            boolean frozen = false;

            if ( frozenCheck == null ) {
                throw new ValidationFailedException(SENDING_FAILED_FROZEN);
            }

            try {
                frozenCheck.next();
                frozen = (frozenCheck.getInt(1) == 0)
                         ? false
                         : true;
            } catch (Exception exc) {
                throw new ValidationFailedException(SENDING_FAILED_FROZEN);
            }
            frozenCheck = null;

            if (frozen) {
                throw new ValidationFailedException(SENDING_FAILED_FROZEN);
            }


        } catch (ValidationFailedException exc) {
            newRequestStatus = Integer.parseInt(exc.getMessage());
            processed=1;
        } catch (Exception exc) {
            newRequestStatus = 99;
            processed=1;
        } finally {
            dbHandler.execute(addNewRequestQueryName,
                              uniqueRequestID,
                              requesterAddress,
                              message,
                              processed,
                              newRequestStatus);
        }
        if (processed == 0 ) {
            Runtime.getRuntime().gc();
            Set<String> mobileNumbers = messageComponent.getMobileNumbers();
            for (String mobileNumber : mobileNumbers ) {
                try {
                    addAllRowsOfNewRequest.setString(1, mobileNumber);
                    addAllRowsOfNewRequest.setString(2, uniqueRequestID);
                    addAllRowsOfNewRequest.executeUpdate();
                } catch (Exception exc) {

                }
            }
            addAllRowsOfNewRequest = null;
            mobileNumbers = null;

            try {
                connection.close();
            } catch (Exception exc) {

            }
        }

        if (!isProcessing()) {
            processByPassSubscription();
        }

    }
    
    private static String validateAndSendSMS(String mobileNumber,
                                             String message,String requestId) {
        String status = SENDING_FAILED_INVALID_PARAMS;
        try {
            if (mobileNumber == null
                        || mobileNumber.trim().equals("")
                        || Pattern.compile("[^0-9]")
                                  .matcher(mobileNumber).find()) {
                throw new ValidationFailedException(SENDING_FAILED_INVALID_PARAMS);
            }
            ResultSet subscriptionCheckResult = null;
            try {
                subscriptionCheck.setString(1, mobileNumber);
                subscriptionCheckResult = 
                            subscriptionCheck.executeQuery();
            } catch (Exception exc) {
                throw new ValidationFailedException(SENDING_FAILED_UNSUBSCRIBED);
            }
            int subscribed = 0;
            if ( subscriptionCheckResult == null ) {
                throw new ValidationFailedException(SENDING_FAILED_UNSUBSCRIBED);
            }
            try {
                subscriptionCheckResult.next();
                subscribed = subscriptionCheckResult.getInt(1);
            } catch (Exception exc) {
                throw new ValidationFailedException(SENDING_FAILED_UNSUBSCRIBED);
            }
            subscriptionCheckResult = null;
            if (subscribed <= 0) {
                throw new ValidationFailedException(SENDING_FAILED_UNSUBSCRIBED);
            }
            SMSSender smsSender = new SMSSender(smsGatewayDetailsPropertiesFile);

            status = (smsSender.sendMessage(mobileNumber, message,requestId))
                      ? SENDING_SUCCESSFUL
                      : SENDING_FAILED_UNKNOWN;


        } catch (ValidationFailedException vfe) {
            status = vfe.getMessage();
        } catch (Exception exc) {

        }
        return status;
    }
private static String validateAndSendSMSByPassSubscription(String mobileNumber,
                                             String message,String requestId) {
        String status = SENDING_FAILED_INVALID_PARAMS;
        try {
            if (mobileNumber == null
                        || mobileNumber.trim().equals("")
                        || Pattern.compile("[^0-9]")
                                  .matcher(mobileNumber).find()) {
                throw new ValidationFailedException(SENDING_FAILED_INVALID_PARAMS);
            }
//            ResultSet subscriptionCheckResult = null;
//            try {
//                subscriptionCheck.setString(1, mobileNumber);
//                subscriptionCheckResult =
//                            subscriptionCheck.executeQuery();
//            } catch (Exception exc) {
//                throw new ValidationFailedException(SENDING_FAILED_UNSUBSCRIBED);
//            }
//            int subscribed = 0;
//            if ( subscriptionCheckResult == null ) {
//                throw new ValidationFailedException(SENDING_FAILED_UNSUBSCRIBED);
//            }
//            try {
//                subscriptionCheckResult.next();
//                subscribed = subscriptionCheckResult.getInt(1);
//            } catch (Exception exc) {
//                throw new ValidationFailedException(SENDING_FAILED_UNSUBSCRIBED);
//            }
//            subscriptionCheckResult = null;
//            if (subscribed <= 0) {
//                throw new ValidationFailedException(SENDING_FAILED_UNSUBSCRIBED);
//            }
            SMSSender smsSender = new SMSSender(smsGatewayDetailsPropertiesFile);
            System.out.println("sending failed unknown = "+SENDING_FAILED_UNKNOWN);

            status = (smsSender.sendMessage(mobileNumber, message,requestId))
                      ? SENDING_SUCCESSFUL
                      : SENDING_FAILED_UNKNOWN;


        } catch (ValidationFailedException vfe) {
            status = vfe.getMessage();
        } catch (Exception exc) {

        }
        return status;
    }
    
}
