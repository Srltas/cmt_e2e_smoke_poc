package com.cmt.e2e.framework.assertion.strategies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.cmt.e2e.framework.command.CommandResult;

public class MigrationSummaryVerificationStrategy implements VerificationStrategy{

    @Override
    public void verify(CommandResult actualResult, Path expectedAnswerPath) throws IOException, AssertionError {
        String sanitizedActual = sanitize(actualResult.output());
        String expected = Files.readString(expectedAnswerPath);

        String trimmedSanitizedActual = trimLines(sanitizedActual);
        String trimmedExpected = trimLines(expected);

        if (!trimmedSanitizedActual.equals(trimmedExpected)) {
            throw new AssertionError("Sanitized text content does not math.");
        }
    }

    private String sanitize(String output) {
        if (output == null) return "";
        return output
            .replaceAll("Migration started at .*", "Migration started at [TIMESTAMP]")
            .replaceAll("Time used: .*", "Time used: [DURATION]")
            .replaceAll("Reading <.*>", "Reading <[SCRIPT_PATH]>")
            .replaceAll("(?m)^Record Migration Progress: (?!100%).*\\R?", "")
            .replaceAll("(?m)^PUBLIC\\..*\\R?", "");
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
