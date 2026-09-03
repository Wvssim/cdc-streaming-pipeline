package ma.wvssim.documents.domain;

import ma.wvssim.documents.storage.StorageService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceDownloadTest {

    @Test
    void restitueLeContenuEtLesMetadonneesDepuisMinio() {
        DocumentRepository repository = mock(DocumentRepository.class);
        StorageService storage = mock(StorageService.class);
        DocumentService service = new DocumentService(repository, storage);
        Document document = new Document(
                "rapport final.pdf", "application/pdf", 4L,
                "documents/rapport-final.pdf", "wassim", OffsetDateTime.now());
        byte[] expected = {1, 2, 3, 4};

        when(repository.findById(42L)).thenReturn(Optional.of(document));
        when(storage.read("documents/rapport-final.pdf")).thenReturn(expected);

        DownloadedDocument result = service.download(42L);

        assertThat(result.filename()).isEqualTo("rapport final.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.content()).containsExactly(expected);
        verify(storage).read("documents/rapport-final.pdf");
    }
}
