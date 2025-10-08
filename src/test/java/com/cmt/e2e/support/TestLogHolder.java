package com.cmt.e2e.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 테스트 실행 중 발생하는 진단 로그를 스레드별로 임시 저장하는 유틸리티 클래스.
 * TestWatcher와 함께 사용되어 테스트 실패 시에만 로그를 파일에 기록하는 용도로 사용됩니다.
 */
public final class TestLogHolder {

    private static final ThreadLocal<List<String>> logHolder = ThreadLocal.withInitial(ArrayList::new);

    private TestLogHolder() {
    }

    /**
     * 현재 스레드의 로그 버퍼에 메시지를 추가합니다.
     *
     * @param message 로그 메시지
     */
    public static void log(String message) {
        logHolder.get().add(message);
    }

    /**
     * 현재 스레드의 로그 버퍼에 포맷팅된 메시지를 추가합니다.
     *
     * @param format    String.format 형식의 포맷 문자열
     * @param args      포맷 인자
     */
    public static void log(String format, Object... args) {
        logHolder.get().add(String.format(format, args));
    }

    /**
     * 현재 스레드에 기록된 모든 로그를 가져옵니다.
     *
     * @return 수정 불가능한 로그 리스트
     */
    public static List<String> getLogs() {
        return Collections.unmodifiableList(logHolder.get());
    }

    /**
     * 현재 스레드의 로그를 모두 지웁니다.
     */
    public static void clear() {
        logHolder.get().clear();
    }
}
