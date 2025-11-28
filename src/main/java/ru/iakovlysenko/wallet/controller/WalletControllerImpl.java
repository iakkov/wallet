package ru.iakovlysenko.wallet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.iakovlysenko.wallet.dto.ErrorResponse;
import ru.iakovlysenko.wallet.dto.WalletOperationRequest;
import ru.iakovlysenko.wallet.dto.WalletOperationResponse;
import ru.iakovlysenko.wallet.exception.InsufficientFundsException;
import ru.iakovlysenko.wallet.exception.WalletNotFoundException;
import ru.iakovlysenko.wallet.service.WalletService;

import java.util.UUID;

/**
 * Реализация {@link WalletController}
 */
@RestController
@RequiredArgsConstructor
public class WalletControllerImpl implements WalletController {

    private final WalletService walletService;

    @Override
    public Mono<ResponseEntity<WalletOperationResponse>> performOperation(@Valid @RequestBody WalletOperationRequest request) {
        return walletService.performOperation(
                        request.walletId(),
                        request.operationType(),
                        request.amount())
                .map(ResponseEntity::ok)
                .onErrorResume(WalletNotFoundException.class, ex ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(new WalletOperationResponse(
                                        request.walletId(),
                                        null,
                                        ex.getMessage()))))
                .onErrorResume(InsufficientFundsException.class, ex ->
                        Mono.just(ResponseEntity.status(HttpStatus.valueOf(422))
                                .body(new WalletOperationResponse(
                                        request.walletId(),
                                        null,
                                        ex.getMessage()))))
                .onErrorResume(ex -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new WalletOperationResponse(
                                request.walletId(),
                                null,
                                "Внутренняя ошибка сервера"))));
    }

    @Override
    public Mono<ResponseEntity<Object>> getBalance(@PathVariable UUID walletId) {
        return walletService.getBalance(walletId)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .onErrorResume(WalletNotFoundException.class, ex ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(new ErrorResponse(
                                        "NOT_FOUND",
                                        ex.getMessage(),
                                        HttpStatus.NOT_FOUND.value()))))
                .onErrorResume(ex ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new ErrorResponse(
                                        "INTERNAL_ERROR",
                                        "Внутренняя ошибка сервера",
                                        HttpStatus.INTERNAL_SERVER_ERROR.value()))));
    }

}
