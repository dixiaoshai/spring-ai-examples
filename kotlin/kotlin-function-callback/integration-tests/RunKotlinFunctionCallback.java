///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.zeroturnaround:zt-exec:1.12
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
//JAVA 17
//FILES ExampleInfo.json
//SOURCES ../../../integration-testing/jbang-lib/IntegrationTestUtils.java

/*
 * kotlin-function-callback 的集成测试启动器
 * 重构为使用集中式工具类
 */

public class RunKotlinFunctionCallback {
    
    public static void main(String... args) throws Exception {
        IntegrationTestUtils.runIntegrationTest("kotlin-function-callback");
    }
}
