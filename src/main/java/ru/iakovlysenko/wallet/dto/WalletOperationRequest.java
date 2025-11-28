package ru.iakovlysenko.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.iakovlysenko.wallet.model.OperationType;

import java.util.UUID;

/**
 * ДТО представляющая запрос на выполнение операции
 * @param walletId
 * @param operationType
 * @param amount
 */
public record WalletOperationRequest(
        @NotNull
        UUID walletId,

        @NotNull
        OperationType operationType,

        @NotNull
        @Positive
        Long amount
) {
}
