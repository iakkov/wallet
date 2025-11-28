package ru.iakovlysenko.wallet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.iakovlysenko.wallet.dto.WalletBalanceResponse;
import ru.iakovlysenko.wallet.dto.WalletOperationResponse;
import ru.iakovlysenko.wallet.exception.InsufficientFundsException;
import ru.iakovlysenko.wallet.exception.WalletNotFoundException;
import ru.iakovlysenko.wallet.model.OperationType;
import ru.iakovlysenko.wallet.model.Wallet;
import ru.iakovlysenko.wallet.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    @InjectMocks
    private WalletServiceImpl walletService;

    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        wallet = new Wallet(walletId, BigDecimal.valueOf(1000));
    }

    @Test
    @DisplayName("Тест операции депозита: пополнение баланса кошелька")
    void testDepositOperation() {
        Wallet updatedWallet = new Wallet(walletId, BigDecimal.valueOf(2000));

        when(walletRepository.findById(walletId))
                .thenReturn(Mono.just(wallet))
                .thenReturn(Mono.just(updatedWallet));
        when(walletRepository.updateBalance(eq(walletId), eq(BigDecimal.valueOf(2000))))
                .thenReturn(Mono.just(1));
        when(walletRepository.insertOrIgnore(any(UUID.class), any(BigDecimal.class)))
                .thenReturn(Mono.just(1));
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mono<WalletOperationResponse> result = walletService.performOperation(walletId, OperationType.DEPOSIT, 1000L);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.walletId().equals(walletId) &&
                        response.balance().compareTo(BigDecimal.valueOf(2000)) == 0 &&
                        response.message().contains("выполнена успешно")
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Тест операции вывода средств: списание с баланса кошелька")
    void testWithdrawOperation() {
        Wallet updatedWallet = new Wallet(walletId, BigDecimal.valueOf(500));

        when(walletRepository.findById(walletId))
                .thenReturn(Mono.just(wallet))
                .thenReturn(Mono.just(updatedWallet));
        when(walletRepository.updateBalance(eq(walletId), eq(BigDecimal.valueOf(500))))
                .thenReturn(Mono.just(1));
        when(walletRepository.insertOrIgnore(any(UUID.class), any(BigDecimal.class)))
                .thenReturn(Mono.just(1));
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mono<WalletOperationResponse> result = walletService.performOperation(walletId, OperationType.WITHDRAW, 500L);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.walletId().equals(walletId) &&
                        response.balance().compareTo(BigDecimal.valueOf(500)) == 0
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Тест вывода средств при недостаточном балансе: должна выбрасываться исключение")
    void testWithdrawInsufficientFunds() {
        when(walletRepository.findById(walletId))
                .thenReturn(Mono.just(wallet));
        when(walletRepository.insertOrIgnore(any(UUID.class), any(BigDecimal.class)))
                .thenReturn(Mono.just(1));
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mono<WalletOperationResponse> result = walletService.performOperation(walletId, OperationType.WITHDRAW, 2000L);

        StepVerifier.create(result)
                .expectError(InsufficientFundsException.class)
                .verify();
    }

    @Test
    @DisplayName("Тест создания кошелька при его отсутствии: автоматическое создание с нулевым балансом")
    void testCreateWalletIfNotExists() {
        Wallet newWallet = new Wallet(walletId, BigDecimal.ZERO);
        Wallet updatedWallet = new Wallet(walletId, BigDecimal.valueOf(1000));

        when(walletRepository.findById(walletId))
                .thenReturn(Mono.empty())
                .thenReturn(Mono.just(newWallet))
                .thenReturn(Mono.just(updatedWallet));
        when(walletRepository.insertOrIgnore(eq(walletId), eq(BigDecimal.ZERO)))
                .thenReturn(Mono.just(1));
        when(walletRepository.updateBalance(eq(walletId), eq(BigDecimal.valueOf(1000))))
                .thenReturn(Mono.just(1));
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mono<WalletOperationResponse> result = walletService.performOperation(walletId, OperationType.DEPOSIT, 1000L);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.walletId().equals(walletId) &&
                        response.balance().compareTo(BigDecimal.valueOf(1000)) == 0
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Тест получения баланса кошелька")
    void testGetBalance() {
        when(walletRepository.findById(walletId))
                .thenReturn(Mono.just(wallet));

        Mono<WalletBalanceResponse> result = walletService.getBalance(walletId);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.walletId().equals(walletId) &&
                        response.balance().compareTo(BigDecimal.valueOf(1000)) == 0
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Тест получения баланса несуществующего кошелька: должна выбрасываться исключение")
    void testGetBalanceNotFound() {
        when(walletRepository.findById(walletId))
                .thenReturn(Mono.empty());

        Mono<WalletBalanceResponse> result = walletService.getBalance(walletId);

        StepVerifier.create(result)
                .expectError(WalletNotFoundException.class)
                .verify();
    }

}
