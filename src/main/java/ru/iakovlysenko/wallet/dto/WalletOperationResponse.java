package ru.iakovlysenko.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ДТО представляющая ответ на выполненную операцию.
 * @param walletId
 * @param balance
 * @param message
 */
public record WalletOperationResponse(
        UUID walletId,
        BigDecimal balance,
        String message
) {
}
