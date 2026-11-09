package org.example.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Role based access: each area is open to its role and the roles above it.
 *
 * <pre>
 *                 /api/user  /api/admin  /api/superadmin
 * anonymous          401        401          401
 * USER               200        403          403
 * ADMIN              200        200          403
 * SUPERADMIN         200        200          200
 * </pre>
 */
@RunOnProfiles({"dev", "stage"})
public class RoleAccessTest extends SecurityTest {

    static Stream<Arguments> accessMatrix() {
        return Stream.of(
                arguments(Role.USER, Constants.API_USER_AREA, 200),
                arguments(Role.USER, Constants.API_ADMIN_AREA, 403),
                arguments(Role.USER, Constants.API_SUPERADMIN_AREA, 403),
                arguments(Role.ADMIN, Constants.API_USER_AREA, 200),
                arguments(Role.ADMIN, Constants.API_ADMIN_AREA, 200),
                arguments(Role.ADMIN, Constants.API_SUPERADMIN_AREA, 403),
                arguments(Role.SUPERADMIN, Constants.API_USER_AREA, 200),
                arguments(Role.SUPERADMIN, Constants.API_ADMIN_AREA, 200),
                arguments(Role.SUPERADMIN, Constants.API_SUPERADMIN_AREA, 200));
    }

    /** REQ-032: 403 = authenticated but not allowed. */
    @ParameterizedTest(name = "{0} on {1} → {2}")
    @MethodSource("accessMatrix")
    @Tag("REQ-032")
    void shouldGrantAccessByRole(Role role, String area, int expectedStatus) {
        given(role.spec())
        .when()
                .get(area)
        .then()
                .statusCode(expectedStatus);
    }

    /** REQ-032: 401 = not authenticated at all. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/api/user", "/api/admin", "/api/superadmin"})
    @Tag("REQ-013")
    @Tag("REQ-032")
    void shouldRejectAnonymousAccess(String area) {
        given()
        .when()
                .get(area)
        .then()
                .statusCode(401);
    }
}
