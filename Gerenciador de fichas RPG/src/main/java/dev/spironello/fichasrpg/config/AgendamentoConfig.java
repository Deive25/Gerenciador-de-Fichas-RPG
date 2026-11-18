package dev.spironello.fichasrpg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga o {@code @Scheduled} (a limpeza das contas demo e dos refresh vencidos, 2.6). */
@Configuration
@EnableScheduling
public class AgendamentoConfig {
}
