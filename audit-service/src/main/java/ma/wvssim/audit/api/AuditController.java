package ma.wvssim.audit.api;

import ma.wvssim.audit.domain.AuditLogRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogRepository repository;

    public AuditController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AuditResponse> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "occurredAt"))
                .stream()
                .map(AuditResponse::from)
                .toList();
    }
}
