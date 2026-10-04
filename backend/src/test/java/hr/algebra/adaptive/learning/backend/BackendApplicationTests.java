package hr.algebra.adaptive.learning.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Trazi pgvector; pokriveno integracijskim testovima")
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
