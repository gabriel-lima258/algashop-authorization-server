package com.algaworks.algashop.authorizationserver.application.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Contrato de pagina proprio da API. Existe para NAO serializar o Page do Spring Data
// direto na resposta: o JSON dele muda entre versoes e carrega campos internos
// (pageable, sort, first/last) que viram contrato sem ninguem decidir isso.
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageModel<T> implements Serializable {
    private int number;
    private int size;
    private int totalPages;
    private long totalElements;

    @Builder.Default
    private List<T> content = new ArrayList<>();

    // atalho para quem ja recebeu um Page pronto do repositorio.
    // quem monta a consulta na mao com MongoOperations usa o builder(),
    // porque ali nao existe Page - o count e o find sao duas chamadas separadas
    public static <T> PageModel<T> of(Page<T> page) {
        return PageModel.<T>builder()
                .content(page.getContent())
                .number(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .build();
    }
}
