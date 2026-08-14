package io.github.lordship.lots;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.List;

public record ShapeData(
        List<Point> vertices,
        BoundingBox bbox,
        Point centroid
) {
    public ShapeData {
        vertices = List.copyOf(vertices);
        if (vertices.size() < 3) {
            throw new IllegalArgumentException("shape_data must have at least 3 vertices");
        }
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    public record Point(double x, double y) {}

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    public record BoundingBox(double minX, double minY, double maxX, double maxY) {}
}