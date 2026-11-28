package gecko.rest.model;

/** Describes a page within the filtered result set. */
public record PageMetadata(int number, int size, int totalElements, int totalPages) {
}
