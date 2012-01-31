/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.commands;

/**
 * The purpose of this interface is to provide the contract that all classes
 * implementing this interface should have an execute method takin two
 * parameters of type String, one the mobile number for the purposes of logging
 * and the other the command itself that would be executed by the implementation
 * class.
 *
 * 
 */

public interface Executable {
    /**
     * Attempts to execute the command.
     * @param mobileNumber  Sender's mobile number for the purposes of logging
     * @param command  Command to be executed. It's the job of the
     * implementation classes to worry about the details of the command
     * @return  <bold>true</bold> if the command execution was successful
     *          <bold>false</bold> if the command execution failed.
     */
    public boolean execute(String mobileNumber, String command);
    @Override
    public String toString();
}
