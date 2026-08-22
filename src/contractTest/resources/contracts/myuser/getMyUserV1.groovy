package contracts.myuser

import org.springframework.cloud.contract.spec.Contract

// O usuario e sempre o autenticado: nenhum id no path. O id da resposta e o sub
// do token, que a MyUserBase mocka fixo.
Contract.make {
    request {
        method GET()
        headers {
            accept 'application/json'
        }
        url("/api/v1/users/me")
    }
    response {
        status 200
        headers {
            contentType 'application/json'
        }
        body([
                id: "019d7764-3e11-7000-8000-00000000000a",
                name: 'Bob Customer',
                email: 'bob.customer@gmail.com',
                type: 'CUSTOMER',
                enabled: true
        ])
    }
}
