package contracts.myuser

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    request {
        method POST()
        url("/api/v1/users/me/password-change")
    }
    response {
        status 200
    }
}
