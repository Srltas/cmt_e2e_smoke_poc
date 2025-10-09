package com.cmt.e2e.framework.command;

public record CommandResult(String output, int exitCode, boolean timedOut) {
}
