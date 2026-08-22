package contracts.myuser

import org.springframework.cloud.contract.spec.Contract

// O self-update so aceita name (MyUserUpdateInput): email, type e enabled nao sao
// editaveis pelo proprio usuario e o contrato afirma que permanecem os originais.
Contract.make {
    request {
        method PUT()
        headers {
            accept 'application/json'
            contentType 'application/json'
        }
        urlPath("/api/v1/users/me") {
            body([
                    name: value(
                            test("Victoria Garcia"),
                            stub(nonBlank())
                    )
            ])
        }
    }
    response {
        status 200
        headers {
            contentType 'application/json'
        }
        body([
                id: "019d7764-3e11-7000-8000-00000000000a",
                name: fromRequest().body('$.name'),
                email: 'bob.customer@gmail.com',
                type: 'CUSTOMER',
                enabled: true
        ])
    }
}
