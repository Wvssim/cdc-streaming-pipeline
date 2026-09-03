package ma.wvssim.audit.consumer;

import ma.wvssim.audit.domain.AuditLogRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceReplayTest {

    @Test
    void rejouerLeMemeEvenementNeCreePasDeDoublon() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.existsById("lsn-424242")).thenReturn(false, true);
        AuditService service = new AuditService(repository);

        String event = """
                {"payload":{"before":null,"after":{"id":7,"filename":"rapport.pdf",
                "content_type":"application/pdf","size":1024,"storage_key":"documents/rapport.pdf",
                "uploaded_by":"test","uploaded_at":"2026-09-03T10:00:00Z"},
                "source":{"lsn":424242},"op":"c","ts_ms":1788429600000}}
                """;

        service.record(event);
        service.record(event);

        verify(repository, times(2)).existsById("lsn-424242");
        verify(repository, times(1)).save(any());
    }
}
