package ru.iakovlysenko.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import ru.iakovlysenko.wallet.dto.WalletBalanceResponse;
import ru.iakovlysenko.wallet.dto.WalletOperationResponse;
import ru.iakovlysenko.wallet.exception.InsufficientFundsException;
import ru.iakovlysenko.wallet.exception.WalletNotFoundException;
import ru.iakovlysenko.wallet.model.OperationType;
import ru.iakovlysenko.wallet.model.Wallet;
import ru.iakovlysenko.wallet.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Реализация {@link WalletService}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;


    @Override
    public Mono<WalletOperationResponse> performOperation(UUID walletId, OperationType operationType, Long amount) {
        BigDecimal amountDecimal = BigDecimal.valueOf(amount);

        Mono<WalletOperationResponse> operation = findWalletByIdDirect(walletId)
                .switchIfEmpty(createWalletIfNotExists(walletId))
                .flatMap(wallet -> {
                    BigDecimal newBalance;
                    if (operationType == OperationType.DEPOSIT) {
                        newBalance = wallet.getBalance().add(amountDecimal);
                    } else {
                        if (wallet.getBalance().compareTo(amountDecimal) < 0) {
                            return Mono.error(new InsufficientFundsException(
                                    String.format("Недостаточно средств. Текущий баланс: %s, требуется: %s",
                                            wallet.getBalance(), amountDecimal)));
                        }
                        newBalance = wallet.getBalance().subtract(amountDecimal);
                    }

                    return updateWalletBalance(walletId, newBalance)
                            .then(findWalletByIdDirect(walletId))
                            .map(updatedWallet -> new WalletOperationResponse(
                                    updatedWallet.getId(),
                                    updatedWallet.getBalance(),
                                    String.format("Операция %s выполнена успешно", operationType.name())
                            ));
                });

        return transactionalOperator.transactional(operation);
    }

    @Override
    public Mono<WalletBalanceResponse> getBalance(UUID walletId) {
        return findWalletById(walletId)
                .map(wallet -> new WalletBalanceResponse(wallet.getId(), wallet.getBalance()));
    }

    private Mono<Wallet> findWalletById(UUID walletId) {
        log.debug("Поиск кошелька по id: {}", walletId);
        return walletRepository.findById(walletId)
                .doOnNext(wallet -> log.debug("Найден кошелек: id={}, balance={}", wallet.getId(), wallet.getBalance()))
                .doOnError(error -> log.error("Ошибка поиска кошелька {}: {}", walletId, error.getMessage()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Кошелек не найден: {}", walletId);
                    return Mono.error(new WalletNotFoundException("Кошелек не найден: " + walletId));
                }));
    }

    private Mono<Wallet> findWalletByIdDirect(UUID walletId) {
        log.debug("Поиск кошелька: {}", walletId);
        return databaseClient.sql("SELECT * FROM wallet.wallets WHERE id = :id")
                .bind("id", walletId)
                .map((row, metadata) -> {
                    Wallet wallet = new Wallet(
                            row.get("id", UUID.class),
                            row.get("balance", java.math.BigDecimal.class)
                    );
                    log.debug("Кошелек найден: id={}, balance={}", wallet.getId(), wallet.getBalance());
                    return wallet;
                })
                .one()
                .doOnError(error -> log.error("Ошибка при поиске кошелька {}: {}", walletId, error.getMessage()));
    }

    private Mono<Wallet> createWalletIfNotExists(UUID walletId) {
        log.debug("Кошелек {} не найден, создаем новый с балансом 0", walletId);
        return databaseClient.sql("INSERT INTO wallet.wallets (id, balance) VALUES (:id, :balance) ON CONFLICT (id) DO NOTHING")
                .bind("id", walletId)
                .bind("balance", BigDecimal.ZERO)
                .fetch()
                .rowsUpdated()
                .then(findWalletByIdDirect(walletId))
                .switchIfEmpty(Mono.error(new WalletNotFoundException("Кошелек не найден: " + walletId)));
    }

    private Mono<Void> updateWalletBalance(UUID walletId, BigDecimal newBalance) {
        log.debug("Обновление баланса: id={}, newBalance={}", walletId, newBalance);
        return databaseClient.sql("UPDATE wallet.wallets SET balance = :balance WHERE id = :id")
                .bind("balance", newBalance)
                .bind("id", walletId)
                .fetch()
                .rowsUpdated()
                .doOnNext(rowsUpdated -> log.debug("Результат обновление: {} кол-во обновлений {}", rowsUpdated, walletId))
                .filter(rowsUpdated -> rowsUpdated > 0)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Ничего не обновилось для: {}", walletId);
                    return Mono.error(new WalletNotFoundException("Кошелек не найден: " + walletId));
                }))
                .then();
    }
}
