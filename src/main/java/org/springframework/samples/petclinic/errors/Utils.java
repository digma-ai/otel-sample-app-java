package org.springframework.samples.petclinic.errors;

import org.apache.coyote.BadRequestException;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for error handling and simulation.
 * Contains methods for throwing various types of exceptions,
 * including simulated exceptions for testing purposes.
 */
@Slf4j
public class Utils {

    /**
     * Marker class to identify simulated exceptions.
     * This class is attached as a suppressed exception to simulated errors.
     */
    public static class SimulationMetadata extends RuntimeException {
        public SimulationMetadata() {
            super("This is a simulated exception for testing purposes");
        }
    }

    /**
     * Throws a simulated IllegalStateException with simulation metadata.
     * This method is specifically used for error simulation scenarios.
     */
    public static void throwSimulatedException() {
        IllegalStateException ex = new IllegalStateException("Simulated monitoring error");
        ex.addSuppressed(new SimulationMetadata());
        log.debug("Throwing simulated exception");
        throw ex;
    }

    public static void ThrowIllegalStateException() throws IllegalStateException {
        throw new IllegalStateException("Some unexpected state error");
    }

    public static void ThrowBadRequestException() throws BadRequestException {
        throw new BadRequestException("validation failure");
    }

    public static void ThrowUnsupportedOperationException() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("operation is not supported");
    }

    public static <T extends Exception> void throwException(Class<T> exceptionClass, String message) throws T {
        try {
            // Use reflection to create a new instance of the exception with the message
            T exceptionInstance = exceptionClass.getConstructor(String.class).newInstance(message);
            throw exceptionInstance;
        } catch (ReflectiveOperationException e) {
            // Handle case where the exception type doesn't have the expected constructor
            throw new IllegalArgumentException("Invalid exception type provided", e);
        }
    }
}