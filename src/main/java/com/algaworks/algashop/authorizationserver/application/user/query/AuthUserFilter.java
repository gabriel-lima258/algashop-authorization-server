package com.algaworks.algashop.authorizationserver.application.user.query;

import com.algaworks.algashop.authorizationserver.application.util.SortablePageFilter;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;
import lombok.*;
import org.springframework.data.domain.Sort;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuthUserFilter extends SortablePageFilter<AuthUserFilter.SortType> {

    private String name;
    private String email;
    private AuthUserType type;

    // usa o que o cliente mandou; se veio vazio, cai no default
    @Override
    public SortType getSortByPropertyOrDefault() {
        return getSortByProperty() != null ? getSortByProperty() : SortType.NAME;
    }

    @Override
    public Sort.Direction getSortDirectionOrDefault() {
        return getSortDirection() != null ? getSortDirection() : Sort.Direction.ASC;
    }

    // enum em vez de String solta: o cliente so consegue ordenar pelos campos
    @Getter
    @RequiredArgsConstructor
    public enum SortType {
        NAME("name"),
        EMAIL("email"),
        TYPE("type");

        private final String propertyName;
    }
}
