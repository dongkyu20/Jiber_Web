package com.jiber.backend.chat;

import com.jiber.backend.chat.client.*;
import com.jiber.backend.chat.controller.*;
import com.jiber.backend.chat.dto.*;
import com.jiber.backend.chat.service.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

class OpenAiChatConfigurationTest {

    @Test
    void applicationYamlDefaultsUseOpenAiVersionedChatCompletionsPath() {
        var environment = applicationYamlEnvironment();
        var binder = Binder.get(environment);

        var connectionProperties = binder.bind(OpenAiConnectionProperties.CONFIG_PREFIX, OpenAiConnectionProperties.class).get();
        var chatProperties = binder.bind(OpenAiChatProperties.CONFIG_PREFIX, OpenAiChatProperties.class).get();

        assertThat(connectionProperties.getBaseUrl()).isEqualTo("https://api.openai.com");
        assertThat(chatProperties.getCompletionsPath()).isEqualTo("/v1/chat/completions");
    }

    private static MockEnvironment applicationYamlEnvironment() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();

        var environment = new MockEnvironment();
        assertThat(properties).isNotNull();
        properties.forEach((key, value) -> environment.setProperty(key.toString(), value.toString()));
        return environment;
    }
}
