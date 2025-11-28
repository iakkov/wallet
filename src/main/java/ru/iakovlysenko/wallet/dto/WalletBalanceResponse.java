package ru.iakovlysenko.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ДТО представляющая баланс кошелька
 * @param walletId
 * @param balance
 */
public record WalletBalanceResponse(
        UUID walletId,
        BigDecimal balance
) {
}
