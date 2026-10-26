package dev.patrick.urlshortener.controller.exceptionHandler;

import dev.patrick.urlshortener.component.SafeBrowsingUnavailableException;
import dev.patrick.urlshortener.service.LinkServiceImpl.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
}
