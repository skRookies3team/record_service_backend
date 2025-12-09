package com.petlog.record.exception;

// ResourceNotFoundException은 보통 EntityNotFoundException을 상속하거나 
// GlobalExceptionHandler에서 404 NOT FOUND로 처리합니다.
// 여기서는 간단한 RuntimeException으로 정의합니다.
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}