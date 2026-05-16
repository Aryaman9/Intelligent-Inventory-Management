package com.inventory.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UpdateStoreRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 50)
    private String type;

    @Size(max = 15)
    private String gstin;

    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(min = 6, max = 6)
    private String pincode;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Size(max = 15)
    private String phone;

    @Email
    private String email;
}
