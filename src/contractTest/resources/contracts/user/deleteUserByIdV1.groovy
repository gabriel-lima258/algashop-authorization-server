package contracts.user

import org.springframework.cloud.contract.spec.Contract

// DELETE aqui promete apenas o status: o corpo e vazio e o efeito colateral
// (anonimizacao, nao remocao fisica) e semantica do servico, invisivel ao contrato
Contract.make {

    request {
        method DELETE()
        headers {
            accept 'application/json'
        }
        url("/api/v1/users/019d7764-3e11-7000-8000-000000000003")
    }

    response {
        status 204
    }
}
