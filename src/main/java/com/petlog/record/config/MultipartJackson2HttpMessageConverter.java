package com.petlog.record.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;


import java.lang.reflect.Type;

/**
 * [멀티파트 데이터 처리를 위한 메시지 컨버터]
 * Multipart/form-data 요청 시, JSON 데이터가 'application/octet-stream'으로 전달되는 경우
 * 이를 Jackson을 통해 객체로 역직렬화(Deserialization)할 수 있도록 지원하는 설정
 */
@Component
public class MultipartJackson2HttpMessageConverter extends AbstractJackson2HttpMessageConverter {

    /**
     * 컨텍스트 생성자
     * application/octet-stream 타입을 Jackson 컨버터가 처리할 수 있는 타입으로 등록
     */
    public MultipartJackson2HttpMessageConverter(ObjectMapper objectMapper) {
        super(objectMapper, MediaType.APPLICATION_OCTET_STREAM);
    }
    
    /**
     * [쓰기 권한 제한]
     * 해당 컨버터는 요청을 읽어오는(역직렬화) 용도로만 사용하며,
     * 외부로 응답을 보낼 때(직렬화)는 사용되지 않도록 모든 canWrite 메서드를 false로 설정
     */
    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }


    @Override
    public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
        return false;
    }


    @Override
    protected boolean canWrite(MediaType mediaType) {
        return false;
    }

}

