package com.example.sintese_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "gemini.api-key=chave-de-teste",
        "app.rate-limit.enabled=false"
})
class SinteseApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
