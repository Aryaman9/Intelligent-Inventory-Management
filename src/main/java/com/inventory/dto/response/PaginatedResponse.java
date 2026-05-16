package com.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaginatedResponse<T> {

    private List<T> items;
    private PaginationInfo pagination;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PaginationInfo {
        private long total;
        private int page;
        private int limit;
        private int pages;
    }

    public static <T> PaginatedResponse<T> of(Page<T> page) {
        return PaginatedResponse.<T>builder()
                .items(page.getContent())
                .pagination(PaginationInfo.builder()
                        .total(page.getTotalElements())
                        .page(page.getNumber())
                        .limit(page.getSize())
                        .pages(page.getTotalPages())
                        .build())
                .build();
    }
}
