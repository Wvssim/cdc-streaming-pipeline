package ma.wvssim.ocr.api;

import ma.wvssim.ocr.domain.ExtractedTextNotFoundException;
import ma.wvssim.ocr.domain.ExtractedTextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final ExtractedTextRepository repository;

    public OcrController(ExtractedTextRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{docId}")
    public OcrResponse get(@PathVariable Long docId) {
        return repository.findById(docId)
                .map(OcrResponse::from)
                .orElseThrow(() -> new ExtractedTextNotFoundException(docId));
    }
}
