package ru.kuzmich.objectmapperproject.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import ru.kuzmich.objectmapperproject.dto.OrderRequestDto;
import ru.kuzmich.objectmapperproject.dto.OrderResponseDto;
import ru.kuzmich.objectmapperproject.dto.ProductDto;
import ru.kuzmich.objectmapperproject.model.Customer;
import ru.kuzmich.objectmapperproject.model.Order;
import ru.kuzmich.objectmapperproject.model.OrderStatus;
import ru.kuzmich.objectmapperproject.model.Product;

public class TestDataFactory {

    public static Product createProduct(Long id, String name, BigDecimal price, int quantity) {
        Product product = new Product();
        product.setProductId(id);
        product.setName(name);
        product.setDescription("Description for " + name);
        product.setPrice(price);
        product.setQuantityInStock(quantity);
        return product;
    }

    public static ProductDto createProductDto(Long id, String name, BigDecimal price,
        int quantity) {
        ProductDto dto = new ProductDto();
        dto.setProductId(id);
        dto.setName(name);
        dto.setDescription("Description for " + name);
        dto.setPrice(price);
        dto.setQuantityInStock(quantity);
        return dto;
    }

    public static List<Product> createProductList() {
        return Arrays.asList(
            createProduct(1L, "Laptop", BigDecimal.valueOf(999.99), 10),
            createProduct(2L, "Mouse", BigDecimal.valueOf(29.99), 50),
            createProduct(3L, "Keyboard", BigDecimal.valueOf(89.99), 30)
        );
    }

    public static List<ProductDto> createProductDtoList() {
        return Arrays.asList(
            createProductDto(1L, "Laptop", BigDecimal.valueOf(999.99), 10),
            createProductDto(2L, "Mouse", BigDecimal.valueOf(29.99), 50),
            createProductDto(3L, "Keyboard", BigDecimal.valueOf(89.99), 30)
        );
    }

    public static Customer createCustomer(Long id, String firstName, String lastName,
        String email) {
        Customer customer = new Customer();
        customer.setCustomerId(id);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setContactNumber("+1234567890");
        return customer;
    }

    public static Customer createDefaultCustomer() {
        return createCustomer(1L, "Ivan", "Ivanov", "i.ivanov@test.ru");
    }

    public static Order createOrder(Long id, Customer customer, List<Product> products,
        OrderStatus status) {
        Order order = new Order();
        order.setOrderId(id);
        order.setCustomer(customer);
        order.setProducts(products);
        order.setOrderDate(LocalDateTime.now());
        order.setShippingAddress("123 Test St, Test City");
        order.setOrderStatus(status);
        order.calculateTotalPrice();
        return order;
    }

    public static Order createDefaultOrder() {
        return createOrder(1L, createDefaultCustomer(), createProductList(), OrderStatus.PENDING);
    }

    public static OrderRequestDto createOrderRequestDto(Long customerId, List<Long> productIds) {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setCustomerId(customerId);
        dto.setProductIds(productIds);
        dto.setShippingAddress("123 Test St, Test City");
        return dto;
    }

    public static OrderRequestDto createGuestOrderRequestDto(String email, List<Long> productIds) {
        OrderRequestDto dto = new OrderRequestDto();
        dto.setEmail(email);
        dto.setContactNumber("+1234567890");
        dto.setProductIds(productIds);
        dto.setShippingAddress("123 Test St, Test City");
        return dto;
    }

    public static OrderResponseDto createOrderResponseDto(Long id, String customerName,
        BigDecimal totalPrice) {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setOrderId(id);
        dto.setOrderDate(LocalDateTime.now());
        dto.setShippingAddress("123 Test St, Test City");
        dto.setTotalPrice(totalPrice);
        dto.setOrderStatus(OrderStatus.PENDING);

        OrderResponseDto.CustomerInfoDto customerInfo = new OrderResponseDto.CustomerInfoDto();
        customerInfo.setCustomerId(1L);
        customerInfo.setFullName(customerName);
        customerInfo.setEmail("i.ivanov@test.ru");
        dto.setCustomerInfo(customerInfo);

        return dto;
    }

    public static String asJsonString(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
