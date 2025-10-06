package com.cmt.e2e.command.migration;

import java.util.ArrayList;
import java.util.List;

import com.cmt.e2e.command.Command;

public abstract class MigrationCommand implements Command {
    protected static final String MIGRATION_SHELL = "./migration.sh";
    protected final String subCommand;
    protected  final List<String> options;

    public MigrationCommand(String subCommand, List<String> options) {
        this.subCommand = subCommand;
        this.options = options;
    }

    public List<String> build() {
        List<String> commandList = new ArrayList<>();
        commandList.add(MIGRATION_SHELL);
        if (subCommand != null && !subCommand.isBlank()) {
            commandList.add(subCommand);
        }
        commandList.addAll(options);
        return commandList;
    }
}
