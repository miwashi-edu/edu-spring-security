package org.example.test;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

public class SkipReasonLogger implements TestWatcher {

    @Override
    public void testAborted(ExtensionContext ctx, Throwable cause) {
        System.out.println("SKIPPED " + ctx.getDisplayName() + ": " + cause.getMessage());
    }

    @Override
    public void testDisabled(ExtensionContext ctx, Optional<String> reason) {
        System.out.println("DISABLED " + ctx.getDisplayName() + ": " + reason.orElse(""));
    }
}