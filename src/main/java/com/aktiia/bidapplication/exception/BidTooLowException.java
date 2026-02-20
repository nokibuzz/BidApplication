package com.aktiia.bidapplication.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class BidTooLowException extends RuntimeException {

    public BidTooLowException(String message) {
        super(message);
    }
}

