/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.helpers;

import com.tcs.igrid.universal.helper.DBConnectionManager;
import com.tcs.igrid.universal.helper.DBHandler;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



public class VerifySubscriptionID extends HttpServlet {
   
    private String fetchSubscriptionIDQuery;

    @Override
    public void init() {
        fetchSubscriptionIDQuery =
                getInitParameter("subscription_verification_query_name");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        Connection connection = new DBConnectionManager().getConnection();
        boolean matched = false;
        try {
            String mobileNumber = request.getParameter("mobile_number").trim();
            int subscriptionIdEntered =
                    Integer.parseInt(request.getParameter("subscription_id").trim());
            DBHandler handler = new DBHandler(connection);
            ResultSet queryResult = handler.fetch(fetchSubscriptionIDQuery, mobileNumber);
            queryResult.next();
            int subscriptonIdInRecord = queryResult.getInt(1);
            matched = (subscriptonIdInRecord == subscriptionIdEntered);
        } catch (Exception exc) {

        }
        PrintWriter out = response.getWriter();
        out.println(matched);
        out.close();
        try {
            connection.close();
        } catch (Exception exc) {

        }
    }


}
