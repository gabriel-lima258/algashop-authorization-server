package contracts.user

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    request {
        method PUT()
        headers {
            accept 'application/json'
            contentType 'application/json'
        }
        urlPath("/api/v1/users/019d7764-3e11-7000-8000-000000000005") {
            body([
                    name: value(
                            test("John Updated"),
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
        status 200
        headers {
            contentType 'application/json'
        }
        // e-mail nao faz parte do update — o contrato afirma que ele permanece o original
        body([
                id: fromRequest().path(3),
                name: fromRequest().body('$.name'),
                email: 'john.manager@algashop.com',
                type: fromRequest().body('$.type'),
                enabled: fromRequest().body('$.enabled')
        ])
    }
}
