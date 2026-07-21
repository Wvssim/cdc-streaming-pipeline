package ma.wvssim.blockchain.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/** Recupere le contenu d'un document dans MinIO (cote lecture du Claim Check). */
@Service
public class StorageService {

    private final MinioClient minioClient;
    private final String bucket;

    public StorageService(MinioClient minioClient, MinioProperties props) {
        this.minioClient = minioClient;
        this.bucket = props.bucket();
    }

    public byte[] fetch(String storageKey) {
        try (InputStream in = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(storageKey)
                .build())) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new StorageException("lecture de '" + storageKey + "' impossible dans MinIO", e);
        }
    }
}
