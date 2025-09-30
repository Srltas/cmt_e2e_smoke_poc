package com.cmt.e2e.support;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ProcessRunner {

    private static final String MIGRATION_SHELL = "./migration.sh";
    private static final String SCRIPT_COMMAND = "script";
    private static final String LOG_COMMAND = "log";
    private static final String REPORT_COMMAND = "report";

    private static final String PRESS_ENTER = "<Press \\[enter\\] to continue...>";
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;

    private final File workDir;

    /**
     * 생성자에서 작업 디렉터리를 미리 받아 저장
     * @param workDir 명령어를 실행할 작업 디렉터리
     */
    public ProcessRunner(File workDir) {
        this.workDir = workDir;
    }

    /**
     * ./migration.sh script ... 명령어 실행
     * 타임아웃이 기본 5분으로 설정
     *
     * @param options
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public ProcessResult script(String[] options) throws IOException, InterruptedException {
        return script(options, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * ./migration.sh script ... 명령어 실행
     *
     * @param options
     * @param timeoutSeconds
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public ProcessResult script(String[] options, long timeoutSeconds) throws IOException, InterruptedException {
        String[] fullCommand = new String[options.length + 2];
        fullCommand[0] = MIGRATION_SHELL;
        fullCommand[1] = SCRIPT_COMMAND;
        System.arraycopy(options, 0, fullCommand, 2, options.length);

        return run(fullCommand, timeoutSeconds);
    }

    /**
     * ./migration.sh log ... 명령어를 실행
     * 프롬프트에 자동으로 응답하는 대화형 실행을 지원
     * 타임아웃이 기본 5분으로 설정
     *
     * @param options
     * @return
     * @throws Exception
     */
    public ProcessResult log(String[] options) throws Exception {
        return log(options, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * ./migration.sh log ... 명령어 실행
     * 프롬프트에 자동으로 응답하는 대화형 실행을 지원
     *
     * @param options
     * @param timeoutSeconds
     * @return
     * @throws Exception
     */
    public ProcessResult log(String[] options, long timeoutSeconds) throws Exception {
        String[] fullCommand = new String[options.length + 2];
        fullCommand[0] = MIGRATION_SHELL;
        fullCommand[1] = LOG_COMMAND;
        System.arraycopy(options, 0, fullCommand, 2, options.length);

        return runWithResponder(fullCommand, Map.of(PRESS_ENTER, "\n"), timeoutSeconds);
    }

    /**
     * ./migration.sh report ... 명령어 실행
     * 타임아웃이 기본 5분으로 설정
     *
     * @param options
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public ProcessResult report(String[] options) throws IOException, InterruptedException {
        return report(options, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * ./migration.sh report ... 명령어 실행
     *
     * @param options
     * @param timeoutSeconds
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public ProcessResult report(String[] options, long timeoutSeconds) throws IOException, InterruptedException {
        String[] fullCommand = new String[options.length + 2];
        fullCommand[0] = MIGRATION_SHELL;
        fullCommand[1] = REPORT_COMMAND;
        System.arraycopy(options, 0, fullCommand, 2, options.length);

        return run(fullCommand, timeoutSeconds);
    }

    /**
     * 외부 명령어를 실행하고 결과를 반환
     * 타임아웃이 기본 5분으로 설정
     *
     * @param command
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public ProcessResult run(String[] command) throws IOException, InterruptedException {
        return run(command, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 외부 명령어를 실행하고 결과를 반환
     *
     * @param command 실행할 명령어와 인자 배열 (e.g., new String[]{"./migration.sh", "unknown-command"})
     * @param timeoutSeconds 타임아웃 시간(초)
     * @return ProcessResult 객체
     * @throws IOException
     * @throws InterruptedException
     */
    public ProcessResult run(String[] command, long timeoutSeconds) throws IOException, InterruptedException {
        System.out.println("[RUN] cwd=" + workDir.getAbsolutePath());
        System.out.println("[RUN] cmd=" + String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        processBuilder.directory(workDir);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        // 지정된 시간 동안 프로세스가 끝나기를 기다림
        boolean finishedInTime = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finishedInTime) {
            // 타임아웃 발생 시, 프로세스를 강제 종료
            process.destroyForcibly();
            return new ProcessResult(output.toString(), -1, true);
        }

        // 정상 종료 시, 결과 반환
        return new ProcessResult(output.toString(), process.exitValue(), false);
    }

    /**
     * 대화형 프로세스를 실행하고, 특정 출력 패턴에 따라 자동으로 응답
     *
     * @param command
     * @param responders
     * @param timeoutSeconds
     * @return
     * @throws Exception
     */
    public ProcessResult runWithResponder(String[] command, Map<String, String> responders, long timeoutSeconds) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workDir);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            Future<String> outputReadingTask = executor.submit(() -> {
               StringBuilder taskOutput = new StringBuilder();
               try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                   String line;
                   while ((line = reader.readLine()) != null) {
                       taskOutput.append(line).append("\n");
                       System.out.println(line + "\n");

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

            boolean finishedInTime = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finishedInTime) {
                process.destroyForcibly();
                return new ProcessResult(outputReadingTask.get(1, TimeUnit.SECONDS), -1, true);
            }

            output.append(outputReadingTask.get(1, TimeUnit.SECONDS));
            return new ProcessResult(output.toString(), process.exitValue(), false);
        } finally {
            executor.shutdownNow();
        }
    }
}
