package contracts.user

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    request {
        method PUT()
        headers {
            accept 'application/json'
            contentType 'application/json'
        }
        urlPath("/api/v1/users/019d7764-3e11-7000-8000-000000000006") {
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
        status 404
        headers {
            contentType 'application/problem+json'
        }
        body([
                instance: fromRequest().path(),
                type: "/errors/not-found",
                title: "Not found"
        ])
    }
}
