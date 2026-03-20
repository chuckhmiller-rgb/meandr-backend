/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.meandr.meandrDataServices.exception;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 *
 * @author chuck
 */
@RestControllerAdvice
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RouteNotFoundException.class)
    public ResponseEntity<Object> handleRouteNotFound(RouteNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        body.put("status", HttpStatus.NOT_FOUND.value());
        //log.error("An unexpected error occurred", ex.getCause().getMessage() + Arrays.toString(ex.getStackTrace()));
        log.error("An unexpected error occurred", ex);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // Catch-all for database errors or parsing errors during saveRoute
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "An unexpected error occurred");
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        log.error("An unexpected error occurred", ex);
        //log.error("An unexpected error occurred", ex.getCause().getMessage() + Arrays.toString(ex.getStackTrace()));

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNotFound(NoResourceFoundException ex) {
        // Just log a tiny warning instead of the whole stack trace
        log.error("An unexpected error occurred", ex);
        //log.error("An unexpected error occurred", ex.getCause().getMessage() + Arrays.toString(ex.getStackTrace()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resource not found");
    }

}
