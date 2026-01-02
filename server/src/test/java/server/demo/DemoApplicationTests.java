package server.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("当前项目启动依赖完整运行时环境（DB/Redis等），在单元测试环境仅保留纯单测即可")
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
