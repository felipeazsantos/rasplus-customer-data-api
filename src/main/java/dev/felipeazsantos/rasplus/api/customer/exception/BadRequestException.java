package dev.felipeazsantos.rasplus.api.customer.exception;

public class BadRequestException extends RuntimeException{

    public BadRequestException(String message) {
        super(message);
    }
}
