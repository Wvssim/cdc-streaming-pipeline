package ma.wvssim.ocr.api;

import ma.wvssim.ocr.domain.ExtractedTextNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ExtractedTextNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(ExtractedTextNotFoundException e) {
        return e.getMessage();
    }
}
