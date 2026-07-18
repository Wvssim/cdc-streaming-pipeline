package ma.wvssim.documents.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration de l'acces MinIO, liee au prefixe {@code minio.*} de application.yml. */
@ConfigurationProperties(prefix = "minio")
public record MinioProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket
) {
}
