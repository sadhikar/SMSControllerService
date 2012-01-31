/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.sendsms;

import com.tcs.igrid.universal.helper.DBConnectionManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class SubscriptionCheck extends HttpServlet {

    public String subscriptionCheckQuery = null;

    @Override
    public void init() {
        String subscriptionCheckQueryName =
                    getInitParameter("sendsms_subscription_check_query_name");
        Properties queries = new Properties();
        InputStream input = this.getClass()
                                .getClassLoader()
                                .getResourceAsStream("queries.properties");
        try {
            queries.load(input);
            subscriptionCheckQuery =
                    queries.getProperty(subscriptionCheckQueryName);
        } catch (Exception exc) {
            throw new ExceptionInInitializerError("Unable to load properties file");
        }
    }
    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        Connection connection = new DBConnectionManager().getConnection();
        Map<String, Boolean> subscriptionStatus = new HashMap<String, Boolean>();
        ObjectInputStream input = null;
        Set<String> numbers = null;

        try {
            PreparedStatement statement =
                connection.prepareStatement(subscriptionCheckQuery);
            input = new ObjectInputStream(request.getInputStream());
            numbers = (Set<String>)input.readObject();
            for (String number: numbers) {

                statement.setString(1, number);
                ResultSet subscriptionCheck = statement.executeQuery();
                boolean subscribed = false;
                if ( subscriptionCheck != null ) {
                    try {
                        subscriptionCheck.next();
                        subscribed = (subscriptionCheck.getInt(1) == 0)
                                     ? false
                                     : true;
                    } catch (Exception exc) {
                    }
                    subscriptionCheck = null;
                }
                subscriptionStatus.put(number, subscribed);
            }

            ObjectOutputStream output =
                    new ObjectOutputStream(response.getOutputStream());

            output.writeObject(subscriptionStatus);
            output.close();

        } catch (Exception exc) {

        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception exc) {

            }
        }

    }

 

}
