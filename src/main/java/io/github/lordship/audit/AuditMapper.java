package io.github.lordship.audit;


import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuditMapper {

    private AuditMapper() {}

    public static Map<String, Object> toMap(Record record) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (RecordComponent component : record.getClass().getRecordComponents()) {
            try {
                Object value = component.getAccessor().invoke(record);
                map.put(component.getName(), value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to map record for audit", e);
            }
        }
        return map;
    }
}
