package com.algaworks.algashop.authorizationserver.application.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Sort;

// Extensão genérica de PageFilter que adiciona capacidade de ordenação.
// O tipo genérico <T> representa o enum de propriedades permitidas para ordenação,
// garantindo que cada filtro concreto defina suas próprias opções de sort válidas (type-safe).
// Hierarquia: PageFilter (page, size) → SortablePageFilter (sortByProperty, sortDirection) → filtro concreto (campos de busca)
// O bound <T extends Enum<T>> fecha a porta para o T virar String, que era o que a
// allowlist de campos ordenaveis existia para impedir.
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public abstract class SortablePageFilter<T extends Enum<T>> extends PageFilter {
    private T sortByProperty;
    private Sort.Direction sortDirection;

    // construtor do super class
    public SortablePageFilter(int size, int page) {
        super(size, page);
    }

    // Métodos abstratos que obrigam cada filtro concreto a definir valores padrão de ordenação,
    // evitando NullPointerException caso o cliente não informe parâmetros de sort.
    public abstract T getSortByPropertyOrDefault();
    public abstract Sort.Direction getSortDirectionOrDefault();
}
