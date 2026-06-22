package com.itma.gestionProjet.dtos;


import lombok.Data;

@Data
public class ApiResponse<T>  {
    private Integer responseCode;
    private String message;
    private T data;

    public ApiResponse(Integer responseCode, String message, Object data) {
        this.responseCode = responseCode;
        this.message = message;
        this.data = (T) data;
    }
}