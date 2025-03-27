package com.wonkglorg.doc.api.controller;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

/**
 * A response object returned by the REST endpoints
 */
public record RestResponse<T>(String message, String error, T content) {


    /**
     * Creates a new response object
     * @param message the message
     * @param content the content
     * @return the response object
     * @param <T> the type of the content
     */
    public static <T> RestResponse<T> success(String message, T content) {
        return new RestResponse<>(message, null, content);
    }

    /**
     * Creates a new successful response object
     * @param content the content
     * @return the response object
     * @param <T> the type of the content
     */
    public static <T> RestResponse<T> success(T content) {
        return new RestResponse<>(null, null, content);
    }

    public static <T> RestResponse<T> error(String error) {
        return new RestResponse<>(null, error, null);
    }

    public ResponseEntity<RestResponse<T>> toResponse() {
        if (error != null) {
            return ResponseEntity.badRequest().body(this);
        }
        return ResponseEntity.ok(this);
    }

    public ResponseEntity<RestResponse<T>> toResponse(HttpStatusCode errorStatus) {
        if (error != null) {
            return ResponseEntity.status(errorStatus).body(this);
        }
        return ResponseEntity.ok(this);
    }

}
