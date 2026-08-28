package com.algaworks.algashop.authorizationserver.infrastructure.persistence.auth;

import com.algaworks.algashop.authorizationserver.application.security.SecurityCheckApplicationService;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserFilter;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserNotFoundException;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserOutput;
import com.algaworks.algashop.authorizationserver.application.user.query.AuthUserQueryService;
import com.algaworks.algashop.authorizationserver.application.util.PageModel;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUser;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserRepository;
import com.algaworks.algashop.authorizationserver.domain.user.AuthUserType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthUserQueryServiceImpl implements AuthUserQueryService {

    private final AuthUserRepository authUserRepository;
    private final EntityManager entityManager;

    private final SecurityCheckApplicationService securityCheck;

    @Override
    public AuthUserOutput findById(UUID userId) {
        return authUserRepository.findById(userId)
                .flatMap(authUser -> {
                    // nega quando NAO pode ver - canViewUser true significa permissao
                    if (!securityCheck.canViewUser(authUser.getType(), authUser.getId())) {
                        throw new AccessDeniedException("Cannot view user of type " + authUser.getType());
                    }

                    return Optional.of(authUser);
                })
                .map(AuthUserOutput::from)
                .orElseThrow(() -> new AuthUserNotFoundException(userId));
    }

    @Override
    public PageModel<AuthUserOutput> findAll(AuthUserFilter filter) {
        Long totalQueryResults = countTotalQueryResults(filter);

        if (totalQueryResults.equals(0L)) {
            // se o total for vazio retorne uma pagina vazia
            PageRequest pageRequest = PageRequest.of(filter.getPage(), filter.getSize());
            return PageModel.of(new PageImpl<>(new ArrayList<>(), pageRequest, totalQueryResults));
        }

        return filterQuery(filter, totalQueryResults);
    }

    private Long countTotalQueryResults(AuthUserFilter filter) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
        Root<AuthUser> root = criteriaQuery.from(AuthUser.class);


        criteriaQuery.select(builder.count(root))
                .where(toPredicates(builder, root, filter));

        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }

    private Predicate[] toPredicates(CriteriaBuilder builder, Root<AuthUser> root, AuthUserFilter filter) {
        List<Predicate> predicates = new ArrayList<>();

        if (filter.getName() != null && !filter.getName().isBlank()) {
            predicates.add(builder.like(builder.lower(root.get("name")), "%" + filter.getName().toLowerCase() + "%"));
        }

        if (filter.getEmail() != null && !filter.getEmail().isBlank()) {
            predicates.add(builder.like(builder.lower(root.get("email")), "%" + filter.getEmail().toLowerCase() + "%"));
        }

        if (filter.getType() != null) {
            predicates.add(builder.equal(root.get("type"), filter.getType()));
        }

        // remove o tipo customer na listagem de users
        predicates.add(builder.not(builder.equal(root.get("type"), AuthUserType.CUSTOMER)));

        return predicates.toArray(new Predicate[]{});
    }

    private PageModel<AuthUserOutput> filterQuery(AuthUserFilter filter, Long totalQueryResults) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuthUserOutput> criteriaQuery = builder.createQuery(AuthUserOutput.class);
        Root<AuthUser> root = criteriaQuery.from(AuthUser.class);

        criteriaQuery.select(
                builder.construct(AuthUserOutput.class,
                        root.get("id"),
                        root.get("name"),
                        root.get("email"),
                        root.get("type"),
                        root.get("enabled")
                )
        );

        Predicate[] predicates = toPredicates(builder, root, filter);
        Order sortOrder = toSortOrder(builder, root, filter);

        criteriaQuery.where(predicates);
        if (sortOrder != null) {
            criteriaQuery.orderBy(sortOrder);
        }

        TypedQuery<AuthUserOutput> typedQuery = entityManager.createQuery(criteriaQuery);

        typedQuery.setFirstResult(filter.getSize() * filter.getPage());
        typedQuery.setMaxResults(filter.getSize());

        PageRequest pageRequest = PageRequest.of(filter.getPage(), filter.getSize());

        return PageModel.of(new PageImpl<>(typedQuery.getResultList(), pageRequest, totalQueryResults));
    }

    private Order toSortOrder(CriteriaBuilder builder, Root<AuthUser> root, AuthUserFilter filter) {
        if (filter.getSortDirectionOrDefault() == Sort.Direction.ASC) {
            return builder.asc(root.get(filter.getSortByPropertyOrDefault().getPropertyName()));
        }

        if (filter.getSortDirectionOrDefault() == Sort.Direction.DESC) {
            return builder.desc(root.get(filter.getSortByPropertyOrDefault().getPropertyName()));
        }

        return null;
    }


}
