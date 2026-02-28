package ru.kuzmich.objectmapperproject.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import ru.kuzmich.objectmapperproject.model.OrderStatus;

@Data
public class OrderResponseDto {

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("customer")
    private CustomerInfoDto customerInfo;

    private List<ProductDto> products;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("order_date")
    private LocalDateTime orderDate;

    @JsonProperty("shipping_address")
    private String shippingAddress;

    @JsonProperty("total_price")
    private BigDecimal totalPrice;

    @JsonProperty("order_status")
    private OrderStatus orderStatus;

    public static class CustomerInfoDto {

        @JsonProperty("customer_id")
        private Long customerId;
        @JsonProperty("full_name")
        private String fullName;
        private String email;

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
