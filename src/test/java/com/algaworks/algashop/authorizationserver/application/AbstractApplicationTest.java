package com.algaworks.algashop.authorizationserver.application;

import com.algaworks.algashop.authorizationserver.util.TestContainerPostgresSQLConfig;
import com.algaworks.algashop.authorizationserver.util.TestSecurityConfig;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Import({
        TestContainerPostgresSQLConfig.class,
        TestSecurityConfig.class,
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractApplicationTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;
}
