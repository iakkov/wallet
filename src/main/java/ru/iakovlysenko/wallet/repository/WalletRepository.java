package ru.iakovlysenko.wallet.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.iakovlysenko.wallet.model.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Репозиторий R2DBC.
 */
@Repository
public interface WalletRepository extends R2dbcRepository<Wallet, UUID> {

    @Query("SELECT * FROM wallet.wallets WHERE id = :id")
    @Override
    Mono<Wallet> findById(UUID id);

    @Query("UPDATE wallet.wallets SET balance = :balance WHERE id = :id")
    Mono<Integer> updateBalance(UUID id, BigDecimal balance);

    @Query("INSERT INTO wallet.wallets (id, balance) VALUES (:id, :balance) ON CONFLICT (id) DO NOTHING")
    Mono<Integer> insertOrIgnore(UUID id, BigDecimal balance);
}
