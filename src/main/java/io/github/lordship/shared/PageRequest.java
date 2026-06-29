package io.github.lordship.shared;

/**
 * Generic pagination + sort input for repository list queries.
 * Use {@link #of} from controllers/services so defaults and validation
 * are applied in exactly one place.
 */
public record PageRequest(
        int page,
        int pageSize,
        String sortBy,
        boolean ascending
) {

    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 200;

    public PageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (sortBy == null || sortBy.isBlank()) {
            throw new IllegalArgumentException("sortBy must not be blank");
        }
    }

    /**
     * Builds a PageRequest applying defaults for null page/pageSize.
     * sortBy is required since it must always be validated against an
     * allow-list by the calling repository.
     */
    public static PageRequest of(Integer page, Integer pageSize, String sortBy, Boolean ascending) {
        return new PageRequest(
                page == null ? 0 : page,
                pageSize == null ? DEFAULT_PAGE_SIZE : pageSize,
                sortBy,
                ascending != null && ascending
        );
    }

    public int offset() {
        return page * pageSize;
    }
}
