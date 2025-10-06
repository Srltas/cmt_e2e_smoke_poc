package com.cmt.e2e.assertion.strategies;

import java.io.IOException;
import java.nio.file.Path;

import com.cmt.e2e.command.CommandResult;

@FunctionalInterface
public interface VerificationStrategy {
    void verify(CommandResult actualResult, Path expectedAnswerPath) throws IOException, AssertionError;
}
