package com.tocka.renovarAPI.infra.configuration;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioClientConfig {

    @Value("${minio.url}")
    private String url;

    @Value("${minio.username}")
    private String username;

    @Value("${minio.password}")
    private String password;

    @Value("${minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        // 1. Constrói o client de forma limpa
        MinioClient client = MinioClient.builder()
                .endpoint(url)
                .credentials(username, password)
                .build();

        // 2. Validação inicial (Opcional, mas evita erros no meio do código)
        try {
            boolean found = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                System.out.println("MinIO: Bucket '" + bucket + "' criado com sucesso.");
            }
        } catch (Exception e) {
            // Se cair aqui, pode ser erro de credenciais ou rede
            System.err.println("CUIDADO: Não foi possível conectar ao MinIO ou validar o bucket.");
            System.err.println("Erro: " + e.getMessage());
        }

        return client;
    }
}