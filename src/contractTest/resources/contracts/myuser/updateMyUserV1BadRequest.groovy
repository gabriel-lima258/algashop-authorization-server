package contracts.myuser

import org.springframework.cloud.contract.spec.Contract

// name em branco viola o @NotBlank do MyUserUpdateInput -> 400 invalid-fields
Contract.make {
    request {
        method PUT()
        headers {
            accept 'application/json'
            contentType 'application/json'
        }
        urlPath("/api/v1/users/me") {
            body([
                    name: ""
            ])
        }
    }
    response {
        status 400
        headers {
            contentType 'application/problem+json'
        }
        body([
                instance: fromRequest().path(),
                type: "/errors/invalid-fields",
                title: "Invalid fields",
                detail: "One or more fields are invalid"
        ])
    }
}
