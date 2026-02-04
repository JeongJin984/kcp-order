package kcp.common.response;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

@Builder
public record PaginationResponse<T> (
    List<T> data,
    long totalCount,
    int totalPage,
    int currentPage,
    boolean isFirst,
    boolean isLast
) {
    public static <T> PaginationResponse<T> from(Page<T> page) {
        return PaginationResponse.<T>builder()
            .data(page.getContent())
            .totalCount(page.getTotalElements())
            .totalPage(page.getTotalPages())
            .currentPage(page.getNumber())
            .isFirst(page.isFirst())
            .isLast(page.isLast())
            .build();
    }
}
