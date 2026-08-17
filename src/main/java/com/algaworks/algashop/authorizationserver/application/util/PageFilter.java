package com.algaworks.algashop.authorizationserver.application.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

// Base de todo filtro de listagem: garante que sempre exista pagina e tamanho.
// Os defaults evitam o pior caso - cliente que nao passa nada e recebe a colecao inteira.
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PageFilter {
    private int size = 15;
    private int page = 0;
}
