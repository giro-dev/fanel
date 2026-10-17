package dev.agiro.fanel.wireguard.infra;

import dev.agiro.fanel.wireguard.api.WireGuardAdapter;
import dev.agiro.fanel.wireguard.domain.DisabledWireGuardAdapter;
import dev.agiro.fanel.wireguard.domain.WgCliAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(WireGuardProperties.class)
public class WireGuardConfiguration {
    @Bean
    Clock wireGuardClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnProperty(prefix = "fanel.wireguard", name = "enabled", havingValue = "true")
    WgCliAdapter activeWireGuardAdapter(WireGuardProperties properties) {
        properties.validateEnabledConfiguration();
        return new WgCliAdapter(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "fanel.wireguard", name = "enabled", havingValue = "false", matchIfMissing = true)
    WireGuardAdapter disabledWireGuardAdapter() {
        return new DisabledWireGuardAdapter();
    }
}
