/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smscontroller.exceptions;

/**
 *
 * @author Shrikant
 */
public class ValidationFailedException extends Exception {
    public ValidationFailedException(String message) {
        super(message);
    }
}
