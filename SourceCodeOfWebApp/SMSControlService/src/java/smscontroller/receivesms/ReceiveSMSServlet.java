/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.receivesms;

import smscontroller.receivesms.helpers.ReceiveSMSHelper;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The purpose of this Servlet is to receive the SMS from the gateway. The URL
 * corresponding to this servlet would be called by the gateway with the fixed
 * get parameters "mobileNo" and "message". This servlet kick starts the actions
 * based upon the command received from the sms.
 * 
 */
public class ReceiveSMSServlet extends HttpServlet {

    /*
     * A response string in this format is demanded by the gateway. This
     * response string has to be sent back to the gateway. One can use
     * String.format(...) to dynamically update the response sent back to the
     * user, based upon the action done
     */
    private static final String RESPONSE_STRING = "<html><head></head>" +
                                                  "<body><center><h3>%s</h3>" +
                                                  "</center></body></html>";


    

    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, 
                         HttpServletResponse response) throws ServletException,
                                                              IOException {
        PrintWriter out = response.getWriter();
        String mobileNumber = request.getParameter("mobileNo");
        String message = request.getParameter("message");
        String requesterAddress = request.getRemoteAddr();
        ReceiveSMSHelper helper = new ReceiveSMSHelper(this);
        String responseMessage = helper.processRequest(mobileNumber,
                                                       message,
                                                       requesterAddress);
        responseMessage = String.format(RESPONSE_STRING, responseMessage);
        out.println(responseMessage);
        try {
                out.close();
        } catch (Exception exc) {
        }

    } //end doGet()

} //end of class RecieveSMSServlet
