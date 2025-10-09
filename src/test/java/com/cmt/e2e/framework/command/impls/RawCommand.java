package com.cmt.e2e.framework.command.impls;

import java.util.Arrays;
import java.util.List;

import com.cmt.e2e.framework.command.Command;

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
