package com.inventory.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {

    private UUID id;
    private String name;
    private String type;
    private String gstin;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phone;
    private String email;

    @JsonProperty("isActive")
    private boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UUID ownerId;
    private String ownerName;
}
