package com.airepair.order;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
public class OrderImage {
 public Long id;
 public Long orderId;
 public Long uploaderId;
 @JsonIgnore public String storageKey;
 public String contentType;
 public LocalDateTime createdAt;
}
