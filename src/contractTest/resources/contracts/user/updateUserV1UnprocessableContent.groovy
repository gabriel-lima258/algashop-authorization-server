package contracts.user

import org.springframework.cloud.contract.spec.Contract

// tentar mudar o tipo de um usuario CUSTOMER e regra de dominio violada:
// AuthUser.setType lanca DomainException, que o ApiExceptionHandler traduz em 422.
// O contrato descreve a semantica para o consumidor: nao e "nao achei" (404)
// nem "request malformado" (400) - e "entendi, mas o negocio nao permite"
Contract.make {
    request {
        method PUT()
        headers {
            accept 'application/json'
            contentType 'application/json'
        }
        urlPath("/api/v1/users/019d7764-3e11-7000-8000-000000000007") {
            body([
                    name: value(
                            test("Bob Customer"),
                            stub(nonBlank())
                    ),
                    type: value(
                            test("MANAGER"),
                            stub(regex('MANAGER|OPERATOR'))
                    ),
                    enabled: value(
                            test(true),
                            stub(anyBoolean())
                    )
            ])
        }
    }
    response {
        status 422
        headers {
            contentType 'application/problem+json'
        }
        body([
                instance: fromRequest().path(),
                type: "/errors/unprocessable-content",
                title: "Unprocessable content",
                detail: "Cannot change type of a CUSTOMER user"
        ])
    }
}
