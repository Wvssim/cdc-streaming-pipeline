package ma.wvssim.notification.api;

import ma.wvssim.notification.domain.NotificationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository repository;

    public NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "sentAt"))
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
