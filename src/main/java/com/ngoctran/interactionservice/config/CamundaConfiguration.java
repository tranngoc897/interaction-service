package com.ngoctran.interactionservice.config;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * Camunda BPM Configuration
 * Provides ProcessEngine, RepositoryService, and RuntimeService beans
 */
@Configuration
public class CamundaConfiguration {

    /**
     * Configure and create ProcessEngine bean
     */
    @Bean
    public ProcessEngine processEngine(DataSource dataSource, DataSourceTransactionManager transactionManager) {
        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();

        config.setDataSource(dataSource);
        config.setTransactionManager(transactionManager);

        // Basic configuration
        config.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        config.setJobExecutorActivate(false); // Disable job executor for embedded use
        config.setHistory(ProcessEngineConfiguration.HISTORY_ACTIVITY);

        // Create and return the process engine
        return config.buildProcessEngine();
    }

    /**
     * RepositoryService bean for process deployment
     */
    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    /**
     * RuntimeService bean for process execution
     */
    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }
}
