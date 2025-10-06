package com.cmt.e2e.command;

public record CommandResult(String output, int exitCode, boolean timedOut) {
}
