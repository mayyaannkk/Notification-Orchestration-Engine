package com.mayyaannkk.notificationengine.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * This class exists ONLY for tests in the persistence module.
 * It lives in src/test/java — it is never compiled into the
 * production JAR, never deployed, never visible to other modules.
 *
 * Why do we need it?
 * @DataJpaTest searches upward from the test class's package for
 * a @SpringBootApplication class to bootstrap from. In a multi-module
 * project, the real one lives in the 'api' module which persistence
 * doesn't depend on. This test-only stub satisfies that search.
 *
 * Why @SpringBootApplication and not @SpringBootConfiguration?
 * @DataJpaTest specifically looks for @SpringBootApplication
 * (which includes @SpringBootConfiguration inside it).
 * Using just @SpringBootConfiguration alone doesn't work here.
 */
@SpringBootApplication
public class TestPersistenceApplication {
    // intentionally empty — no main() needed, this is never run directly
}