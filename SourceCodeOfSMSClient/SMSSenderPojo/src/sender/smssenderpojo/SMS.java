/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sender.smssenderpojo;

import com.tcs.igrid.smssenderpojo.helpers.SMSMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;


public class SMS  {

   private URL sendURLObject;
   private URL sendUrlObjectWithBypass;
   private URL subscriptionURLObject;
   private String subscriptionVerificationURL;
   private Set<String> mobileNumbersSet;

   /**
    * 
    * @throws java.io.IOException
    * @throws java.net.MalformedURLException
    */
   public SMS() throws IOException, MalformedURLException {

        InputStream urlFile = this.getClass().getClassLoader()
                .getResourceAsStream("smssenderurl.properties");
        Properties urlProperties = new Properties();
        urlProperties.load(urlFile);
        String sendURL = urlProperties.getProperty("send");
        String sendURLWithByPass = urlProperties.getProperty("sendWithBypass");
        String subscriptionCheckURL =
                urlProperties.getProperty("subscription_check");
        subscriptionVerificationURL =
                urlProperties.getProperty("verify_subscription_code");
        sendURLObject = new URL(sendURL);
        sendUrlObjectWithBypass = new URL(sendURLWithByPass);
        subscriptionURLObject = new URL(subscriptionCheckURL);
        

   }

   /**
    * This method take a pair of the mobileNumber and the subscriptionId and checks if they match.
    * @param mobileNumber
    * @param subscriptionID
    * @return true if parameters match; else false
    */
   public boolean isSubscriptionIDValid(String mobileNumber,
                                          int subscriptionID) {
       if ((mobileNumber == null)
                        || mobileNumber.trim().equals("")
                        || Pattern.compile("[^0-9]").matcher(mobileNumber).find()) {
           return false;
       }
       if ((mobileNumber.length() == 10))
                mobileNumber="91" + mobileNumber;
       URL subscriptionVerificationURLObject = null;
       try {
           String subscriptionVerificationURLParams =
                    String.format(subscriptionVerificationURL,
                                  mobileNumber,
                                  subscriptionID);

            subscriptionVerificationURLObject =
                    new URL(subscriptionVerificationURLParams);
            URLConnection connection =
                    subscriptionVerificationURLObject.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            BufferedReader input = new BufferedReader(
                                        new InputStreamReader(
                                            connection.getInputStream()));
            String value = input.readLine();
            return Boolean.parseBoolean(value);
       } catch (Exception exc) {
            return false;
       }
       


   }
   private Set<String> formatMobileNumbers(String[] mobileNumbers) {
        Set<String> uniqueMobileNos = new HashSet<String>();
        if (mobileNumbers == null) return uniqueMobileNos;
        for(String mobileNumber : mobileNumbers) {
            if ((mobileNumber == null) 
                        || mobileNumber.trim().equals("")
                        || Pattern.compile("[^0-9]").matcher(mobileNumber).find())
                continue;
            if ((mobileNumber.length() == 10))
                mobileNumber="91" + mobileNumber;
            uniqueMobileNos.add(mobileNumber);
        }
        return uniqueMobileNos;
    }
   /**
    * Sends a message to the given mobile number ONLY if the user of the mobile number has subscriped to recieve messages from the gateway.
    * @param mobileNumber
    * @param message
    * @return A message stating the status of the message.
    * @throws java.lang.Exception
    */
   public String sendMessage(String mobileNumber,
                                          String message) throws Exception {
       String[] mobileNumbers = { mobileNumber };
       return sendMessage(mobileNumbers, message);
   }
   /**
    * Given a mobile number it returns the mobilenumber mapped to a booloean value, checking if a mobile number is subscribed or not.
    * @param mobileNumber
    * @return Returns mobilenumber mapped to a boolean value; True if subscribed 
    * @throws java.lang.Exception
    */
   public Map<String, Boolean> getSubscriptionStatus(String mobileNumber) throws Exception {
       String[] mobileNumbers = {mobileNumber};
       return getSubscriptionStatus(mobileNumbers);
   }
   /**
    * Given an array of mobile number it returns the mobilenumber mapped to a booloean value, checking if a mobile number is subscribed or not.
    * @param mobileNumbers
    * @return Returns mobilenumber mapped to a boolean value; True if subscribed 
    * @throws java.lang.Exception
    */
   public Map<String,Boolean> getSubscriptionStatus(String[] mobileNumbers) throws Exception {
       Set<String> numbers = new HashSet<String>();
        for(String mobileNumber : mobileNumbers) {
            if ((mobileNumber == null)
                        || mobileNumber.trim().equals("")
                        || Pattern.compile("[^0-9]").matcher(mobileNumber).find())
                continue;
            if ((mobileNumber.length() == 10))
                mobileNumber="91" + mobileNumber;
            numbers.add(mobileNumber);
        }
        URLConnection connection = subscriptionURLObject.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setDefaultUseCaches(false);

        ObjectOutputStream output =
                new ObjectOutputStream(connection.getOutputStream());
        output.writeObject(numbers);
        output.flush();
        output.close();

        ObjectInputStream input =
                new ObjectInputStream(connection.getInputStream());
        Map<String, Boolean> subscriptionStatus =
                (Map<String,Boolean>)input.readObject();
       return subscriptionStatus;
   }
   /**
    * Sends a message to a given array of mobile numbers ONLY if the user of the mobile number has subscriped to recieve messages from the gateway.
    * @param mobileNumbers
    * @param message
    * @return Status of delivery
    * @throws java.lang.Exception
    */
   public String sendMessage(String[] mobileNumbers,
                                          String message) throws Exception {

        this.mobileNumbersSet = formatMobileNumbers(mobileNumbers);
        SMSMessage messageComponent = new SMSMessage(mobileNumbersSet,
                                                     message);
        
        URLConnection connection = sendURLObject.openConnection(Proxy.NO_PROXY);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setDefaultUseCaches(false);

        ObjectOutputStream output =
                new ObjectOutputStream(connection.getOutputStream());
        output.writeObject(messageComponent);
        output.flush();
        output.close();
        
        BufferedReader input =
                new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String deliveryStatus = input.readLine();
        input.close();
        return deliveryStatus;
    }
   /**
    * Sends a message to the given mobile number; Please read below for the risk involved
    * The National Do Not Call Registry (NDNC Registry) is setup to curb the Unsolicited Commercial Communication (UCC), which is any telecommunications service message, which is transmitted for the purpose of informing about or soliciting or promoting any commercial transaction in relation to goods, investments or services which a subscriber opts not to receive. The NDNC Registry will be a database having the list of all telephone numbers of the subscribers who do not want to receive unsolicited messages. Telephone subscriber (land line or mobile) who does not wish to receive UCC, can register their telephone number with their telecom service provider for inclusion in the NDNC. Telecom Service Provider shall upload the telephone number to the NDNC within 45 days of receipt. The Telemarketer will have to verify their calling telephone numbers list with the NDNC registry before making a call. An amount of Rs 500/- per call/message for the first call has been prescribed to discourage telemarketers who make calls to numbers registered in Do Not Call list. The second call will have a fine of Rs 1000/- and third time the line will be disconnected. The defaulter telemarketer will face disconnection of telecom service.
    * @param mobileNumber
    * @param message
    * @return Returns a return reciept
    * @throws java.lang.Exception
    */
   public String sendMessageSubscriptionBypass(String mobileNumber,
                                          String message) throws Exception {
       String[] mobileNumbers = { mobileNumber };
       return sendMessageSubscriptionBypass(mobileNumbers, message);
   }
   /**
    * Sends a message to a given array of mobile numbers; Please read below for the risk involved
    * Please read below for the risk involved
    * The National Do Not Call Registry (NDNC Registry) is setup to curb the Unsolicited Commercial Communication (UCC), which is any telecommunications service message, which is transmitted for the purpose of informing about or soliciting or promoting any commercial transaction in relation to goods, investments or services which a subscriber opts not to receive. The NDNC Registry will be a database having the list of all telephone numbers of the subscribers who do not want to receive unsolicited messages. Telephone subscriber (land line or mobile) who does not wish to receive UCC, can register their telephone number with their telecom service provider for inclusion in the NDNC. Telecom Service Provider shall upload the telephone number to the NDNC within 45 days of receipt. The Telemarketer will have to verify their calling telephone numbers list with the NDNC registry before making a call. An amount of Rs 500/- per call/message for the first call has been prescribed to discourage telemarketers who make calls to numbers registered in Do Not Call list. The second call will have a fine of Rs 1000/- and third time the line will be disconnected. The defaulter telemarketer will face disconnection of telecom service.
    * @param mobileNumbers
    * @param message
    * @return Returns a return reciept
    * @throws java.lang.Exception
    */
   public String sendMessageSubscriptionBypass(String[] mobileNumbers,
                                          String message) throws Exception {

        this.mobileNumbersSet = formatMobileNumbers(mobileNumbers);
        SMSMessage messageComponent = new SMSMessage(mobileNumbersSet,
                                                     message);

        URLConnection connection = sendUrlObjectWithBypass.openConnection(Proxy.NO_PROXY);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setDefaultUseCaches(false);

        ObjectOutputStream output =
                new ObjectOutputStream(connection.getOutputStream());
        output.writeObject(messageComponent);
        output.flush();
        output.close();

        BufferedReader input =
                new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String deliveryStatus = input.readLine();
        input.close();
        return deliveryStatus;
    }

}
