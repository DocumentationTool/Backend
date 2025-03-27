package com.wonkglorg.doc.core.exception.client;

/**
 * Represent an exception that do not indicate an error in the code, but rather an error in the client's request and should be treated in the controllers response separately instead of logging it as an error
 * Example:
 * <pre>
 * {@code
 * try{
 *      # call to some service that may throw a ClientException
 * } catch(ClientException e){
 *      return RestResponse.<>error(e.getMessage()).toResponse();
 * } catch(Exception e){
 *      log.error("Error while execution.", e);
 *      return RestResponse.<>error(e.getMessage()).toResponse();
 * }
 * }
 *
 * </pre>
 */
public class ClientException extends Exception {
    public ClientException() {
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientException(Throwable cause) {
        super(cause);
    }

    public ClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
