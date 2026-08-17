package contracts.user

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    request {
        method GET()
        headers {
            accept "application/json"
        }
        // aqui o parametro de pagina chama-se "page" (PageFilter deste servico),
        // diferente do product-catalog, que expoe "number" nos contratos de categoria
        url("/api/v1/users") {
            queryParameters {
                parameter("size", value(
                        stub(optional(anyNumber())),
                        test(10)
                ))
                parameter("page", value(
                        stub(optional(anyNumber())),
                        test(0)
                ))
            }
        }
    }

    response {
        status 200
        headers {
            contentType "application/json"
        }
        body([
                size: fromRequest().query("size"),
                number: 0,
                totalElements: 2,
                totalPages: 1,
                content: [
                        [
                                id: anyUuid(),
                                name: 'John Manager',
                                email: 'john.manager@algashop.com',
                                type: 'MANAGER',
                                enabled: true
                        ],
                        [
                                id: anyUuid(),
                                name: 'Alice Operator',
                                email: 'alice.operator@algashop.com',
                                type: 'OPERATOR',
                                enabled: true
                        ]
                ]
        ])
    }
}
