/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.commands;

import smscontroller.receivesms.exceptions.InvalidCommandException;

/**
 * The purpose of this class is to serve as a factory. All implementing
 * subclasses are required to implement the getResponse() method.
 * 
 */
public abstract class Command implements Executable {

    /**
     * This factory method attempts to create an instance of one of it's subtype
     * specified in the argument.
     * @param className  Any of the concrete implements of the abstract Command
     * type.
     * @return An instance of type Command
     * @throws com.tcs.igrid.receivesms.helpers.exceptions.InvalidCommandException
     * if the argument isn't a subtype of Command
     * or if JVM was not able to find the specified class,
     */
    public static Command getInstance(String className) 
                                     throws InvalidCommandException {
        try {
            /*
             * Create an instance of the class specified by the argument.
             */
            return (Command)Class.forName(className).newInstance();
        } catch (Exception exc) {
            /* hide the other exception details and throw an exception stating
             * the proper reason of the failure
             */
            throw new InvalidCommandException("Unable to get an instance of " +
                    "the specified command. \n"+exc.getMessage());
        } 
    }

    public abstract String getResponse();
}
