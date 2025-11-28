package ru.iakovlysenko.wallet.dto;

/**
 * ДТО для ошибок.
 * @param error
 * @param message
 * @param status
 */
public record ErrorResponse(
        String error,
        String message,
        int status
) {
}
