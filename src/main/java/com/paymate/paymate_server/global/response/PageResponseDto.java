package com.paymate.paymate_server.global.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Page를 그대로 JSON으로 직렬화하면 Spring Data에서 경고가 발생함.
 * 이 DTO로 감싸서 반환하면 JSON 구조가 안정적으로 유지됨.
 */
@Getter
public class PageResponseDto<T> {

    private final List<T> content;
    private final int totalPages;
    private final long totalElements;
    private final int number;
    private final int size;
    private final boolean first;
    private final boolean last;

    public PageResponseDto(List<T> content, int totalPages, long totalElements, int number, int size, boolean first, boolean last) {
        this.content = content;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.number = number;
        this.size = size;
        this.first = first;
        this.last = last;
    }

    public static <T> PageResponseDto<T> of(Page<T> page) {
        return new PageResponseDto<>(
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast()
        );
    }
}
