package ru.iakovlysenko.wallet.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Сущность кошелька
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("wallets")
public class Wallet {

    /**
     * Уникальный идентификационный номер кошелька.
     */
    @Id
    @Column("id")
    private UUID id;

    /**
     * Баланс кошелька.
     */
    @Column("balance")
    private BigDecimal balance;

}
