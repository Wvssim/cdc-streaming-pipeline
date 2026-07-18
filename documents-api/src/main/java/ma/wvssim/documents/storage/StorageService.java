package ma.wvssim.documents.storage;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/** Depose les fichiers dans MinIO (materialisation du Claim Check). */
@Service
public class StorageService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final MinioClient minioClient;
    private final String bucket;

    public StorageService(MinioClient minioClient, MinioProperties props) {
        this.minioClient = minioClient;
        this.bucket = props.bucket();
    }

    /** Depose le fichier et renvoie sa cle de stockage (a persister en base). */
    public String store(MultipartFile file) {
        String key = StorageKeys.forUpload(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? DEFAULT_CONTENT_TYPE : file.getContentType();
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .stream(in, file.getSize(), -1)
                            .contentType(contentType)
                            .build());
        } catch (IOException e) {
            throw new StorageException("lecture du fichier impossible", e);
        } catch (Exception e) {
            throw new StorageException("depot dans MinIO impossible", e);
        }
        return key;
    }
}
