package com.inventory.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreateStoreRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String type;

    @Size(max = 15)
    private String gstin;

    @NotBlank
    private String address;

    @NotBlank
    @Size(max = 100)
    private String city;

    @NotBlank
    @Size(max = 100)
    private String state;

    @NotBlank
    @Size(min = 6, max = 6)
    private String pincode;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Size(max = 15)
    private String phone;

    @Email
    private String email;
}
