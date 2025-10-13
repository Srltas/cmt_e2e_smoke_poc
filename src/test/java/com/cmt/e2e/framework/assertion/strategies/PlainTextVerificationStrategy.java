package com.cmt.e2e.framework.assertion.strategies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.cmt.e2e.framework.assertion.VerificationFailedException;
import com.cmt.e2e.framework.command.CommandResult;

public class PlainTextVerificationStrategy implements VerificationStrategy {

    @Override
    public void verify(CommandResult actualResult, Path expectedAnswerPath) throws IOException {
        String actualText = trimLines(actualResult.output());
        String expectedText = trimLines(Files.readString(expectedAnswerPath));

        if (!actualText.equals(expectedText)) {
            throw new VerificationFailedException("Text content does not match.", actualText, expectedText);
        }
    }

    private String trimLines(String input) {
        if (input == null) return "";
        String[] lines = input.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines[i].trim());
        }
        return sb.toString();
    }
}
