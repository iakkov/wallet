package ru.iakovlysenko.wallet.exception;

public class ConcurrentOperationException extends RuntimeException {
    public ConcurrentOperationException(String message) {
        super(message);
    }
}
