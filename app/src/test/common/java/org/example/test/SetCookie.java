package org.example.test;

import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Parsed {@code Set-Cookie} header, to assert cookie attributes such as {@code HttpOnly},
 * {@code Secure}, {@code SameSite}, {@code Path} and {@code Max-Age}.
 */
public record SetCookie(String name, String value, Map<String, String> attributes) {

    /** The {@code Set-Cookie} header for {@code name}, if the response sets it. */
    public static Optional<SetCookie> of(Response response, String name) {
        return response.headers().getValues("Set-Cookie").stream()
                .filter(h -> h.startsWith(name + "="))
                .findFirst()
                .map(SetCookie::parse);
    }

    public static SetCookie parse(String header) {
        String[] parts = header.split(";");
        String[] nameValue = parts[0].split("=", 2);
        Map<String, String> attributes = new HashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String[] kv = parts[i].trim().split("=", 2);
            attributes.put(kv[0].toLowerCase(Locale.ROOT), kv.length > 1 ? kv[1] : "");
        }
        return new SetCookie(nameValue[0], nameValue.length > 1 ? nameValue[1] : "", attributes);
    }

    public boolean httpOnly() {
        return attributes.containsKey("httponly");
    }

    public boolean secure() {
        return attributes.containsKey("secure");
    }

    public String sameSite() {
        return attributes.getOrDefault("samesite", "");
    }

    public String path() {
        return attributes.getOrDefault("path", "");
    }

    /** True if the cookie is being deleted ({@code Max-Age=0} or empty value). */
    public boolean cleared() {
        return "0".equals(attributes.get("max-age")) || value.isEmpty();
    }
}
