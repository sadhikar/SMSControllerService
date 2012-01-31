/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.sendsms;

import smscontroller.sendsms.helpers.SendSMSHelper;
import com.tcs.igrid.smssenderpojo.helpers.SMSMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class SendSmsBypassSubscription extends HttpServlet {

    @Override
    public void init() {
        SendSMSHelper.init(this);

    }

    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException,
                                                               IOException {


        SMSMessage messageComponent = null;
        //final ServletConfig config = this;
        String uniqueID = UUID.randomUUID().toString();

        SendSMSHelper helper = null;
        String uniqueRequestID = null;
        String requesterAddress = request.getRemoteAddr();

        ObjectInputStream input =
                new ObjectInputStream(request.getInputStream());

        try {
            messageComponent = (SMSMessage)input.readObject();
        } catch (Exception exc) {
            throw new RuntimeException(exc.getMessage());
        }

        uniqueRequestID = uniqueID + "_" +
                String.format("%1$08d",messageComponent.getSize());

        PrintWriter out = response.getWriter();
        out.println(uniqueRequestID);
        try {
            out.close();
        } catch (Exception exc) {

        }

        helper= new SendSMSHelper();
        helper.addRequestDndByPassSubcription(uniqueRequestID, requesterAddress, messageComponent);


    } //end doPost()

    @Override
    public void destroy() {
        SendSMSHelper.destroy();
    }

} //end Servlet
