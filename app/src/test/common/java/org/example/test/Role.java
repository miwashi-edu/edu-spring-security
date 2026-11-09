package org.example.test;

import io.restassured.specification.RequestSpecification;

/**
 * The three test accounts, for parameterized tests: {@code @EnumSource(Role.class)}.
 */
public enum Role {
    USER(Constants.USER_EMAIL),
    ADMIN(Constants.ADMIN_EMAIL),
    SUPERADMIN(Constants.SUPERADMIN_EMAIL);

    private final String email;

    Role(String email) {
        this.email = email;
    }

    public String email() {
        return email;
    }

    public String password() {
        return Constants.DEFAULT_PASSWORD;
    }

    /** Authenticated request spec, use as {@code given(role.spec())}. */
    public RequestSpecification spec() {
        return switch (this) {
            case USER -> BaseHttpTest.user;
            case ADMIN -> BaseHttpTest.admin;
            case SUPERADMIN -> BaseHttpTest.superAdmin;
        };
    }
}
