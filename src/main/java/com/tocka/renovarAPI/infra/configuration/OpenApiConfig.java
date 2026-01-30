package com.tocka.renovarAPI.infra.configuration;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Renovar API")
                .version("1.0")
                .description(
                    "API do Re:Novar, um sistema de monitoramento e prevenção da ludopatia "
                    + "que utiliza dados comportamentais e aprendizado de máquina para identificar riscos, "
                    + "gerar alertas e apoiar o autocuidado do usuário."
                )
            )
            .tags(Arrays.asList(
                new Tag().name("Autenticação e Registro de Pacientes").description("Endpoints relacionados à todas as questões de autenticação e registro de pacientes no sistema."),
                new Tag().name("Dashboard do Paciente").description("Endpoints relacionados ao dashboard do paciente, é o ponto de entrada para o paciente visualizar suas informações e métricas principais."),
                new Tag().name("Apostas").description("Endpoints relacionados ao gerenciamento e visualização de apostas."),
                new Tag().name("Perfil do Paciente").description("Endpoints relacionados ao perfil e dados pessoais do paciente."),
                new Tag().name("Relatórios Mensais").description("Endpoints relacionados à geração, visualização e download de relatórios mensais de atividades e métricas do paciente.")
            )
        );
    }
}
