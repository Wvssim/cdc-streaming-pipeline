package ma.wvssim.documents.api;

import ma.wvssim.documents.domain.DocumentService;
import ma.wvssim.documents.domain.DownloadedDocument;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentControllerDownloadTest {

    @Test
    void downloadRestitueLeContenuEtLesEntetesDuDocument() {
        DocumentService service = mock(DocumentService.class);
        byte[] content = "contenu original".getBytes();
        when(service.download(12L)).thenReturn(
                new DownloadedDocument("rapport final.pdf", "application/pdf", content));

        ResponseEntity<byte[]> response = new DocumentController(service).download(12L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(content.length);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment", "rapport%20final.pdf");
        assertThat(response.getBody()).isEqualTo(content);
    }
}
