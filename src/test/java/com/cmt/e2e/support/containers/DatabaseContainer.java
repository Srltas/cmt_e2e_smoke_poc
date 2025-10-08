package com.cmt.e2e.support.containers;

import com.cmt.e2e.support.Drivers.DB;
import org.testcontainers.containers.GenericContainer;

public interface DatabaseContainer {

    /**
     * 컨테이너가 실행 중인 호스트 이름을 반환
     */
    String getHost();

    /**
     * 외부에 노출된 DB 포트 번호를 반환
     */
    Integer getDatabasePort();

    /**
     * 이 컨테이너의 DB 종류를 반환
     */
    DB getDbType();

    /**
     * 내부적으로 사용하는 Testcontainers의 GenericContainer 인스턴스를 반환
     */
    GenericContainer<?> getContainer();
}
