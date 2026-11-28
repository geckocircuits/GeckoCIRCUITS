package gecko.rest.controller;

import gecko.rest.model.PageMetadata;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Validates list query parameters and slices a deterministic sorted result. */
final class ListPage {
    private final int number;
    private final int size;
    private final String field;
    private final boolean descending;

    ListPage(int number, int size, String sort) {
        if (number < 0 || size < 1) {
            throw invalid("Page must be nonnegative and size must be positive.");
        }
        String[] parts = sort.split(",", -1);
        if (parts.length != 2 || !List.of("id", "createdAt").contains(parts[0])
                || !List.of("asc", "desc").contains(parts[1])) {
            throw invalid("Sort must be id or createdAt, followed by asc or desc.");
        }
        this.number = number;
        this.size = Math.min(size, 100);
        field = parts[0];
        descending = parts[1].equals("desc");
    }

    <T> List<T> select(List<T> items, Map<String, Comparator<T>> orders) {
        Comparator<T> comparator = orders.get(field);
        if (descending) comparator = comparator.reversed();
        return items.stream().sorted(comparator).skip((long) number * size).limit(size).toList();
    }

    PageMetadata metadata(int total) {
        return new PageMetadata(number, size, total, (int) (((long) total + size - 1) / size));
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
