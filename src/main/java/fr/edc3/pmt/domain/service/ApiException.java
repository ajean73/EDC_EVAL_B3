package fr.edc3.pmt.domain.service;

public class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }
}
