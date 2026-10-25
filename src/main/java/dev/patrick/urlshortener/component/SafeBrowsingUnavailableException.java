package dev.patrick.urlshortener.component;

public class SafeBrowsingUnavailableException extends RuntimeException{
    public SafeBrowsingUnavailableException(String message){
        super(message);
    }
}
