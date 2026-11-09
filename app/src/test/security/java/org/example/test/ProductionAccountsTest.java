package org.example.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The only security test that runs in production (REQ-038): the well-known test accounts and
 * their published passwords must not work there.
 */
@RunOnProfiles("prod")
public class ProductionAccountsTest extends SecurityTest {

    /** REQ-037 */
    @ParameterizedTest(name = "{0}")
    @EnumSource(Role.class)
    @Tag("REQ-037")
    void shouldRejectTestAccounts(Role role) {
        login(role)
                .then().statusCode(401);
    }
}
