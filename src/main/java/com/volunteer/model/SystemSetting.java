package com.volunteer.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing an administrative system-wide setting.
 */
public class SystemSetting {

    private String key;
    private String value;
    private LocalDateTime updatedAt;
    private Long updatedBy;

    public SystemSetting() {
        this.updatedAt = LocalDateTime.now();
    }

    public SystemSetting(String key, String value, Long updatedBy) {
        this.key = key;
        this.value = value;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemSetting that = (SystemSetting) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "SystemSetting [key=" + key + ", value=" + value + "]";
    }
}
