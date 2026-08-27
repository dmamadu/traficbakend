package com.itma.gestionProjet.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class TacheNotFoundException extends RuntimeException {

    public TacheNotFoundException(String message) {
        super(message);
    }

}
