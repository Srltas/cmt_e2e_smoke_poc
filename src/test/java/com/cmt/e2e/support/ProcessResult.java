package com.cmt.e2e.support;

public record ProcessResult(String output, int exitCode, boolean timedOut) {
}
