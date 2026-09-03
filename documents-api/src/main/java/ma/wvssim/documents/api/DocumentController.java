package ma.wvssim.documents.api;

import ma.wvssim.documents.domain.Document;
import ma.wvssim.documents.domain.DocumentService;
import ma.wvssim.documents.domain.DownloadedDocument;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadedBy", defaultValue = "anonyme") String uploadedBy) {
        Document saved = service.upload(file, uploadedBy);
        return ResponseEntity
                .created(URI.create("/api/documents/" + saved.getId()))
                .body(DocumentResponse.from(saved));
    }

    @GetMapping
    public List<DocumentResponse> list() {
        return service.findAll().stream().map(DocumentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable Long id) {
        return DocumentResponse.from(service.findById(id));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        DownloadedDocument document = service.download(id);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(document.contentType());
        } catch (IllegalArgumentException ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(document.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(document.content());
    }

    @PutMapping("/{id}")
    public DocumentResponse rename(@PathVariable Long id, @RequestParam String filename) {
        return DocumentResponse.from(service.rename(id, filename));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
