package com.cmt.e2e.command.impls;

import java.util.Arrays;
import java.util.List;

import com.cmt.e2e.command.Command;

public class RawCommand implements Command {

    private final List<String> command;

    public RawCommand(String... command) {
        this.command = Arrays.asList(command);
    }

    @Override
    public List<String> build() {
        return this.command;
    }
}
