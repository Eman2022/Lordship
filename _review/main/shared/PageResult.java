package io.github.lordship.shared;

import java.util.List;
import java.util.function.Function;


// Generic paginated result wrapper for repository/service list queries.
public record PageResult<T>(
        List<T> content,
        int page,
        int pageSize,
        long totalElements,
        int totalPages
) {

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(
                content.stream().map(mapper).toList(),
                page,
                pageSize,
                totalElements,
                totalPages
        );
    }

    public static <T> PageResult<T> of(List<T> content, PageRequest request, long totalElements) {
        int totalPages = request.pageSize() == 0
                ? 0
                : (int) Math.ceil((double) totalElements / request.pageSize());

        return new PageResult<>(
                content,
                request.page(),
                request.pageSize(),
                totalElements,
                totalPages
        );
    }
}
