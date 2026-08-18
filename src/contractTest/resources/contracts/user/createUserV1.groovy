package contracts.user

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    request {
        method POST()
        headers {
            accept 'application/json'
            contentType 'application/json'
        }
        urlPath("/api/v1/users") {
            body([
                    name: value(
                            test("Alice Operator"),
                            stub(nonBlank())
                    ),
                    email: value(
                            test("alice.operator@algashop.com"),
                            stub(nonBlank())
                    ),
                    type: value(
                            test("OPERATOR"),
                            stub(regex('MANAGER|OPERATOR|CUSTOMER'))
                    )
            ])
        }
    }
    response {
        status 201
        headers {
            contentType 'application/json'
        }
        // a senha temporaria nao aparece no response - e gerada pelo servidor
        // e entregue por outro canal; o contrato afirma isso por omissao
        body([
                id: anyUuid(),
                name: fromRequest().body('$.name'),
                email: fromRequest().body('$.email'),
                type: fromRequest().body('$.type'),
                enabled: true
        ])
    }
}
