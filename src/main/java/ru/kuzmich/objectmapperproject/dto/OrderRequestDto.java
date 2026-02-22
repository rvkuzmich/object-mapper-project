package ru.kuzmich.objectmapperproject.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDto {

    @NotNull(message = "Customer ID is required")
    @JsonProperty("customer_id")
    private Long customerId;

    @NotEmpty(message = "Product IDs list cannot be empty")
    @JsonProperty("product_ids")
    private List<Long> productIds;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address cannot exceed 500 characters")
    @JsonProperty("shipping_address")
    private String shippingAddress;

    @Email(message = "Invalid email format")
    private String email; // Optional: for new customer

    @Pattern(regexp = "^[0-9+\\-\\s()]{10,15}$", message = "Invalid contact number format")
    @JsonProperty("contact_number")
    private String contactNumber;
}
