/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.receivesms.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class ClassNameResolver {

    private Properties nameResolutionProperties;

    public ClassNameResolver() {
        InputStream nameResolutionPropertiesFile = null;
        try {
            nameResolutionPropertiesFile = this.getClass().getClassLoader()
                    .getResourceAsStream("nameresolution.properties");
            nameResolutionProperties = new Properties();
            nameResolutionProperties.load(nameResolutionPropertiesFile);
            nameResolutionPropertiesFile.close();

        } catch (IOException exc) {
            throw new ExceptionInInitializerError("Problem reading the Name " +
                                                  "resolution properties file");
        }
    }

    public String resolveClassName(String command) {
        return nameResolutionProperties.getProperty(command.trim()
                                                   .toLowerCase());
    }

}
