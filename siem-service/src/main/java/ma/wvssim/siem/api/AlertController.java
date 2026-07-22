package ma.wvssim.siem.api;

import ma.wvssim.siem.domain.AlertRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRepository repository;

    public AlertController(AlertRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AlertResponse> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "raisedAt"))
                .stream()
                .map(AlertResponse::from)
                .toList();
    }
}
