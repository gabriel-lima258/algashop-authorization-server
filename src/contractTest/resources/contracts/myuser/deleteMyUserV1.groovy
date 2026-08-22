package contracts.myuser

import org.springframework.cloud.contract.spec.Contract

// Exclusao logica (anonimizacao) do proprio perfil - so para usuarios CUSTOMER
Contract.make {
    request {
        method DELETE()
        url("/api/v1/users/me")
    }
    response {
        status 204
    }
}
