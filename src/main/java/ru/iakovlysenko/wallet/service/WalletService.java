package ru.iakovlysenko.wallet.service;

import reactor.core.publisher.Mono;
import ru.iakovlysenko.wallet.dto.WalletBalanceResponse;
import ru.iakovlysenko.wallet.dto.WalletOperationResponse;
import ru.iakovlysenko.wallet.model.OperationType;

import java.util.UUID;

/**
 * Сервис под реализацию.
 */
public interface WalletService {
    Mono<WalletOperationResponse> performOperation(UUID walletId, OperationType operationType, Long amount);
    Mono<WalletBalanceResponse> getBalance(UUID walletId);
}
