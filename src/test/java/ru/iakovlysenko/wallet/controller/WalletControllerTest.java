package ru.iakovlysenko.wallet.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.iakovlysenko.wallet.dto.WalletBalanceResponse;
import ru.iakovlysenko.wallet.dto.WalletOperationResponse;
import ru.iakovlysenko.wallet.exception.InsufficientFundsException;
import ru.iakovlysenko.wallet.exception.WalletNotFoundException;
import ru.iakovlysenko.wallet.model.OperationType;
import ru.iakovlysenko.wallet.service.WalletService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(WalletControllerImpl.class)
class WalletControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private WalletService walletService;

    @Test
    @DisplayName("Тест API операции депозита: успешное пополнение баланса")
    void testDepositOperation() {
        UUID walletId = UUID.randomUUID();
        WalletOperationResponse response = new WalletOperationResponse(
                walletId,
                BigDecimal.valueOf(1000),
                "Операция DEPOSIT выполнена успешно"
        );

        when(walletService.performOperation(eq(walletId), eq(OperationType.DEPOSIT), eq(1000L)))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "walletId": "%s",
                            "operationType": "DEPOSIT",
                            "amount": 1000
                        }
                        """.formatted(walletId))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.walletId").isEqualTo(walletId.toString())
                .jsonPath("$.balance").isEqualTo(1000)
                .jsonPath("$.message").exists();
    }

    @Test
    @DisplayName("Тест API операции вывода средств: успешное списание с баланса")
    void testWithdrawOperation() {
        UUID walletId = UUID.randomUUID();
        WalletOperationResponse response = new WalletOperationResponse(
                walletId,
                BigDecimal.valueOf(500),
                "Операция WITHDRAW выполнена успешно"
        );

        when(walletService.performOperation(eq(walletId), eq(OperationType.WITHDRAW), eq(500L)))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "walletId": "%s",
                            "operationType": "WITHDRAW",
                            "amount": 500
                        }
                        """.formatted(walletId))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.walletId").isEqualTo(walletId.toString())
                .jsonPath("$.balance").isEqualTo(500);
    }

    @Test
    @DisplayName("Тест API вывода средств при недостаточном балансе: возврат статуса 422")
    void testWithdrawInsufficientFunds() {
        UUID walletId = UUID.randomUUID();

        when(walletService.performOperation(eq(walletId), eq(OperationType.WITHDRAW), eq(1000L)))
                .thenReturn(Mono.error(new InsufficientFundsException("Недостаточно средств")));

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "walletId": "%s",
                            "operationType": "WITHDRAW",
                            "amount": 1000
                        }
                        """.formatted(walletId))
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.walletId").isEqualTo(walletId.toString())
                .jsonPath("$.message").exists();
    }

    @Test
    @DisplayName("Тест API получения баланса кошелька")
    void testGetBalance() {
        UUID walletId = UUID.randomUUID();
        WalletBalanceResponse response = new WalletBalanceResponse(
                walletId,
                BigDecimal.valueOf(1500)
        );

        when(walletService.getBalance(walletId))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri("/api/v1/wallets/" + walletId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.walletId").isEqualTo(walletId.toString())
                .jsonPath("$.balance").isEqualTo(1500);
    }

    @Test
    @DisplayName("Тест API получения баланса несуществующего кошелька: возврат статуса 404")
    void testGetBalanceNotFound() {
        UUID walletId = UUID.randomUUID();

        when(walletService.getBalance(walletId))
                .thenReturn(Mono.error(new WalletNotFoundException("Кошелек не найден: " + walletId)));

        webTestClient.get()
                .uri("/api/v1/wallets/" + walletId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("NOT_FOUND")
                .jsonPath("$.status").isEqualTo(404);
    }

    @Test
    @DisplayName("Тест валидации запроса: отрицательная сумма должна возвращать статус 400")
    void testValidationError() {
        UUID walletId = UUID.randomUUID();

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "walletId": "%s",
                            "operationType": "DEPOSIT",
                            "amount": -100
                        }
                        """.formatted(walletId))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Тест валидации запроса: отсутствующие обязательные поля должны возвращать статус 400")
    void testMissingFields() {
        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest();
    }

}
