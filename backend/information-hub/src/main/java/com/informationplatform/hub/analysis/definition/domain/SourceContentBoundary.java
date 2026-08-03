package com.informationplatform.hub.analysis.definition.domain;

public record SourceContentBoundary(
        /** 不可信来源数据的开始标记。 */
        String startMarker,
        /** 不可信来源数据的结束标记。 */
        String endMarker) {

    public SourceContentBoundary {
        if (startMarker == null || startMarker.isBlank() || endMarker == null || endMarker.isBlank()) {
            throw new IllegalArgumentException("Source content boundary markers must not be blank");
        }
        if (startMarker.equals(endMarker)) {
            throw new IllegalArgumentException("Source content boundary markers must be different");
        }
    }

    /** 将序列化后的来源数据严格包裹在不可信数据边界内。 */
    public String wrap(String serializedSource) {
        if (serializedSource == null) {
            throw new IllegalArgumentException("Serialized source must not be null");
        }
        return startMarker + System.lineSeparator()
                + serializedSource + System.lineSeparator()
                + endMarker;
    }
}
