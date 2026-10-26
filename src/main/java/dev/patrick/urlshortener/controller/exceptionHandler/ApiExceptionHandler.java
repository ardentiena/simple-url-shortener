package dev.patrick.urlshortener.controller.exceptionHandler;

import dev.patrick.urlshortener.component.CaptchaRequiredException;
import dev.patrick.urlshortener.component.CaptchaUnavailableException;
import dev.patrick.urlshortener.component.RateLimitExceededException;
import dev.patrick.urlshortener.component.SafeBrowsingUnavailableException;
import dev.patrick.urlshortener.service.LinkServiceImpl.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(InvalidLinkException.class)
    public ProblemDetail handleInvalidLink(InvalidLinkException e){
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(AliasAlreadyExistException.class)
    public ProblemDetail handleAliasAlreadyExist(AliasAlreadyExistException e){
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(LinkNotFoundException.class)
    public ProblemDetail handleLinkNotFound(LinkNotFoundException e){
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(LinkExpiredException.class)
    public ProblemDetail handleLinkExpired(LinkExpiredException e){
        return ProblemDetail.forStatusAndDetail(HttpStatus.GONE, e.getMessage());
    }

    @ExceptionHandler(SafeBrowsingUnavailableException.class)
    public ProblemDetail handleSafeBrowsingUnavailable(SafeBrowsingUnavailableException e){
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Link safety check is temporarily unavailable. please try again later.");
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(RateLimitExceededException e){
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "Limit is 10 attempts per minute.");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, "60")
                .body(problemDetail);
    }

    @ExceptionHandler(CaptchaRequiredException.class)
    public ProblemDetail handleCaptchaRequired(CaptchaRequiredException e){
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, e.getMessage()
        );
        problemDetail.setTitle("CAPTCHA required");
        problemDetail.setProperty("captchaRequired", true);
        problemDetail.setProperty("siteKey", e.getSiteKey());
        return problemDetail;
    }

    @ExceptionHandler(CaptchaUnavailableException.class)
    public ProblemDetail handleCaptchaUnavailable(CaptchaUnavailableException e){
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Captcha verification is temporarily unavailable. Please try again later."
        );
    }
}
