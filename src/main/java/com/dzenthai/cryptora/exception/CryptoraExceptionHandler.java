package com.dzenthai.cryptora.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;


@Slf4j
@ControllerAdvice
public class CryptoraExceptionHandler {

    @ExceptionHandler({Exception.class})
    public ResponseEntity<?> handleException(Exception e) {
        return buildExceptionData(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({RuntimeException.class})
    public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
        return buildExceptionData(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return buildExceptionData(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({NoResourceFoundException.class})
    public ResponseEntity<?> handleNoResourceFoundException(NoResourceFoundException e) {
        return buildExceptionData(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({NoSuchElementException.class})
    public ResponseEntity<?> handleNoSuchElementException(NoSuchElementException e) {
        return buildExceptionData(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({NumberFormatException.class})
    public ResponseEntity<?> handleNumberFormatException(NumberFormatException e) {
        return buildExceptionData(e, HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<?> buildExceptionData(final Exception exception, final HttpStatusCode status) {
        var message = exception.getMessage();
        var code = status.value();
        var timestamp = Instant.now();
        var data = Map.of(
                "message", message,
                "code", status,
                "timestamp", timestamp
        );
        log.error("CryptoraExceptionHandler | message: {}, code: {}, timestamp: {}, exception: {}",
                message, code, timestamp, exception.getStackTrace());
        return new ResponseEntity<>(data, status);
    }

}
