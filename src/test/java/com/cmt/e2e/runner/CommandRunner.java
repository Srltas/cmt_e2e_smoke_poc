package com.cmt.e2e.runner;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.cmt.e2e.command.Command;
import com.cmt.e2e.command.CommandResult;
import com.cmt.e2e.command.impls.LogCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandRunner {
    private static final Logger log = LoggerFactory.getLogger(CommandRunner.class);

    private static final int DEFAULT_TIMEOUT_SECONDS = 300;
    private final File workDir;

    /**
     * 생성자에서 작업 디렉터리를 미리 받아 저장
     * @param workDir 명령어를 실행할 작업 디렉터리
     */
    public CommandRunner(File workDir) {
        this.workDir = workDir;
    }

    public CommandResult run(Command command) throws IOException, InterruptedException {
        return run(command, DEFAULT_TIMEOUT_SECONDS);
    }

    public CommandResult run(Command command, long timeoutSeconds) throws IOException, InterruptedException {
        List<String> commandList = command.build();

        log.debug("Executing command list: {}", commandList);
        log.debug("CWD: {}", workDir.getAbsolutePath());
        log.debug("CMD: {}", String.join(" ", commandList));

        ProcessBuilder processBuilder = new ProcessBuilder(commandList);
        processBuilder.directory(workDir);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finishedInTime = process.waitFor(timeoutSeconds, SECONDS);
        CommandResult result;
        if (!finishedInTime) {
            process.destroyForcibly();
            result =  new CommandResult(output.toString(), -1, true);
        } else {
            result = new CommandResult(output.toString(), process.exitValue(), false);
        }
        log.debug("Command finished with exitCode: {}, timeOut: {}", result.exitCode(), result.timedOut());
        log.debug("Command output: {}", result.output());
        return result;
    }

    public CommandResult runInteractive(LogCommand command) throws Exception {
        return runInteractive(command, command.DEFAULT_RESPONDERS, DEFAULT_TIMEOUT_SECONDS);
    }

    public CommandResult runInteractive(Command command, Map<String, String> responders, long timeoutSeconds) throws Exception {
        List<String> commandList = command.build();
        ProcessBuilder processBuilder = new ProcessBuilder(commandList);
        processBuilder.directory(workDir);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), UTF_8))) {
            Future<String> outputReadingTask = executor.submit(() -> {
                StringBuilder taskOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        taskOutput.append(line).append("\n");
                        System.out.println(line); // Log real-time output for debugging

                        for (Map.Entry<String, String> entry : responders.entrySet()) {
                            if (Pattern.compile(entry.getKey()).matcher(line).find()) {
                                writer.write(entry.getValue());
                                writer.flush();
                                break;
                            }
                        }
                    }
                }
                return taskOutput.toString();
            });

            boolean finishedInTime = process.waitFor(timeoutSeconds, SECONDS);
            String capturedOutput = outputReadingTask.get(5, SECONDS);

            if (!finishedInTime) {
                process.destroyForcibly();
                return new CommandResult(capturedOutput, -1, true);
            }

            return new CommandResult(capturedOutput, process.exitValue(), false);
        } finally {
            executor.shutdownNow();
        }
    }
}
