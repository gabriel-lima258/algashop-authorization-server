package contracts.user

import org.springframework.cloud.contract.spec.Contract

Contract.make {

    request {
        method GET()
        headers {
            accept 'application/json'
        }
        url("/api/v1/users/019d7764-3e11-7000-8000-000000000001")
    }

    response {
        status 200
        headers {
            contentType 'application/json'
        }
        body([
                id: fromRequest().path(3),
                name: 'John Manager',
                email: 'john.manager@algashop.com',
                type: 'MANAGER',
                enabled: true
        ])
    }
}
