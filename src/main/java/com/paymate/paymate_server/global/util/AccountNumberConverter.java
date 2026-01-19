package com.paymate.paymate_server.global.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AccountNumberConverter implements AttributeConverter<String, String> {

    private final AesUtil aesUtil = new AesUtil(); // 위에서 만든 유틸 사용

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return aesUtil.encrypt(attribute); // DB 저장 전 암호화
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return aesUtil.decrypt(dbData); // DB 조회 후 복호화
    }
}