package io.github.lordship.audit;

import java.lang.reflect.RecordComponent;
import java.util.*;

public class AuditMapper {

    private AuditMapper() {}

    public record Diff(Map<String, Object> before, Map<String, Object> after) {}

    public static Diff diff(Record before, Record after){
        Map<String, Object> beforeMap = toMap(before);
        Map<String, Object> afterMap = toMap(after);

        Map<String, Object> changedBefore = new LinkedHashMap<>();
        Map<String, Object> changedAfter = new LinkedHashMap<>();

        for (String key : afterMap.keySet()) {
            Object oldVal = beforeMap.get(key);
            Object newVal = afterMap.get(key);
            if (!Objects.equals(oldVal, newVal)) {
                changedBefore.put(key, oldVal);
                changedAfter.put(key, newVal);
            }
        }
        return new Diff(changedBefore, changedAfter);
    }

    public static Map<String, Object> toMap(Record record) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (RecordComponent component : record.getClass().getRecordComponents()) {
            try {
                String name = component.getName();
                Object value = component.getAccessor().invoke(record);
                map.put(name, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to map record for audit", e);
            }
        }
        return map;
    }
}
