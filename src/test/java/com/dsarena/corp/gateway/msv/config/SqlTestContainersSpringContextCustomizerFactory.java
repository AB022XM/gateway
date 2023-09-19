package com.dsarena.corp.gateway.msv.config;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import tech.jhipster.config.JHipsterConstants;

public class SqlTestContainersSpringContextCustomizerFactory implements ContextCustomizerFactory {

    private Logger log = LoggerFactory.getLogger(SqlTestContainersSpringContextCustomizerFactory.class);

    private static SqlTestContainer prodTestContainer;

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        return (context, mergedConfig) -> {
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            TestPropertyValues testValues = TestPropertyValues.empty();
            EmbeddedSQL sqlAnnotation = AnnotatedElementUtils.findMergedAnnotation(testClass, EmbeddedSQL.class);
            boolean usingTestProdProfile = Arrays
                .asList(context.getEnvironment().getActiveProfiles())
                .contains("test" + JHipsterConstants.SPRING_PROFILE_PRODUCTION);
            if (null != sqlAnnotation && usingTestProdProfile) {
                log.debug("detected the EmbeddedSQL annotation on class {}", testClass.getName());
                log.info("Warming up the sql database");
                if (null == prodTestContainer) {
                    try {
                        Class<? extends SqlTestContainer> containerClass = (Class<? extends SqlTestContainer>) Class.forName(
                            this.getClass().getPackageName() + ".MysqlTestContainer"
                        );
                        prodTestContainer = beanFactory.createBean(containerClass);
                        beanFactory.registerSingleton(containerClass.getName(), prodTestContainer);
                        // ((DefaultListableBeanFactory)beanFactory).registerDisposableBean(containerClass.getName(), prodTestContainer);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                testValues =
                    testValues.and(
                        "spring.r2dbc.url=" +
                        prodTestContainer.getTestContainer().getJdbcUrl().replace("jdbc", "r2dbc") +
                        "?useUnicode=true&characterEncoding=utf8&useSSL=false&useLegacyDatetimeCode=false&createDatabaseIfNotExist=true"
                    );
                testValues = testValues.and("spring.r2dbc.username=" + prodTestContainer.getTestContainer().getUsername());
                testValues = testValues.and("spring.r2dbc.password=" + prodTestContainer.getTestContainer().getPassword());
                testValues =
                    testValues.and(
                        "spring.liquibase.url=" +
                        prodTestContainer.getTestContainer().getJdbcUrl() +
                        "?useUnicode=true&characterEncoding=utf8&useSSL=false&useLegacyDatetimeCode=false&createDatabaseIfNotExist=true"
                    );
            }
            testValues.applyTo(context);
        };
    }
}