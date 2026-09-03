package ma.wvssim.documents.domain;

import ma.wvssim.documents.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class DocumentService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final DocumentRepository repository;
    private final StorageService storageService;

    public DocumentService(DocumentRepository repository, StorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    /**
     * Depose le fichier dans MinIO puis enregistre les metadonnees en base.
     * L'INSERT en base est ce que Debezium capte pour emettre l'evenement.
     */
    @Transactional
    public Document upload(MultipartFile file, String uploadedBy) {
        String storageKey = storageService.store(file);
        String contentType = file.getContentType() == null ? DEFAULT_CONTENT_TYPE : file.getContentType();
        Document document = new Document(
                file.getOriginalFilename(),
                contentType,
                file.getSize(),
                storageKey,
                uploadedBy,
                OffsetDateTime.now());
        return repository.save(document);
    }

    @Transactional(readOnly = true)
    public List<Document> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Document findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public DownloadedDocument download(Long id) {
        Document document = findById(id);
        byte[] content = storageService.read(document.getStorageKey());
        return new DownloadedDocument(document.getFilename(), document.getContentType(), content);
    }

    /** Le flush JPA emet un UPDATE, capte par Debezium avec before/after grace a REPLICA IDENTITY FULL. */
    @Transactional
    public Document rename(Long id, String newFilename) {
        Document document = findById(id);
        document.rename(newFilename);
        return document;
    }

    /** Supprime la ligne (DELETE capte par Debezium) puis l'objet MinIO associe. */
    @Transactional
    public void delete(Long id) {
        Document document = findById(id);
        repository.delete(document);
        storageService.delete(document.getStorageKey());
    }
}
