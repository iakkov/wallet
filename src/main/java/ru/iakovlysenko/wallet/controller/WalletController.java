package ru.iakovlysenko.wallet.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.iakovlysenko.wallet.dto.WalletOperationRequest;
import ru.iakovlysenko.wallet.dto.WalletOperationResponse;

import java.util.UUID;

/**
 * Интерфейс под реализацию контроллера.
 */
@RequestMapping("/api/v1")
public interface WalletController {

    /**
     * Метод, осуществляющий операцию.
     */
    @PostMapping("/wallet")
    Mono<ResponseEntity<WalletOperationResponse>> performOperation(@Valid @RequestBody WalletOperationRequest request);

    /**
     * Метод для получения баланса.
     */
    @GetMapping("/wallets/{walletId}")
    Mono<ResponseEntity<Object>> getBalance(@PathVariable UUID walletId);

}
