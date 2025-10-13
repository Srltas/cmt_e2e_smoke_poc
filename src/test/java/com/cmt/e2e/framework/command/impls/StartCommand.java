package com.cmt.e2e.framework.command.impls;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.cmt.e2e.framework.command.CmtCommand;

public class StartCommand extends CmtCommand {

    private StartCommand(List<String> options) {
        super("start", options);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<String> options = new ArrayList<>();

        public Builder script(Path scriptPath) {
            options.add(scriptPath.toAbsolutePath().toString());
            return this;
        }

        public StartCommand build() {
            return new StartCommand(options);
        }
    }
}
