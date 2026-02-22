package ru.kuzmich.objectmapperproject.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.kuzmich.objectmapperproject.dto.OrderRequestDto;
import ru.kuzmich.objectmapperproject.dto.OrderResponseDto;
import ru.kuzmich.objectmapperproject.exception.InsufficientStockException;
import ru.kuzmich.objectmapperproject.exception.ResourceNotFoundException;
import ru.kuzmich.objectmapperproject.model.Order;
import ru.kuzmich.objectmapperproject.model.OrderStatus;
import ru.kuzmich.objectmapperproject.service.OrderService;
import ru.kuzmich.objectmapperproject.util.TestDataFactory;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderRequestDto testOrderRequest;
    private OrderResponseDto testOrderResponse;
    private List<OrderResponseDto> testOrderResponseList;
    private List<Long> testProductIds;

    @BeforeEach
    void setUp() {
        testProductIds = Arrays.asList(1L, 2L, 3L);
        testOrderRequest = TestDataFactory.createOrderRequestDto(1L, testProductIds);

        testOrderResponse = TestDataFactory.createOrderResponseDto(1L, "Ivan Ivanov",
            BigDecimal.valueOf(1119.97));

        testOrderResponseList = Arrays.asList(
            TestDataFactory.createOrderResponseDto(1L, "John Doe", BigDecimal.valueOf(1119.97)),
            TestDataFactory.createOrderResponseDto(2L, "John Doe", BigDecimal.valueOf(1119.97))
        );
    }

    @Test
    void shouldCreateOrder() throws Exception {
        when(orderService.createOrder(any(OrderRequestDto.class))).thenReturn(testOrderResponse);

        ResultActions result = mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testOrderRequest)));

        result.andDo(print())
            .andExpect(status().isCreated());

        verify(orderService, times(1)).createOrder(any(OrderRequestDto.class));
    }

    @Test
    void shouldReturn400WhenNoCustomerIdentifier() throws Exception {
        OrderRequestDto invalidRequest = new OrderRequestDto();
        invalidRequest.setProductIds(testProductIds);
        invalidRequest.setShippingAddress("123 Test St");

        ResultActions result = mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)));

        result.andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenNoProducts() throws Exception {
        OrderRequestDto invalidRequest = TestDataFactory.createOrderRequestDto(1L, List.of());

        ResultActions result = mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)));

        result.andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        when(orderService.createOrder(any(OrderRequestDto.class)))
            .thenThrow(new ResourceNotFoundException("Customer not found with id: 999"));

        ResultActions result = mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                TestDataFactory.createOrderRequestDto(999L, testProductIds))));

        result.andDo(print())
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenProductsNotFound() throws Exception {
        when(orderService.createOrder(any(OrderRequestDto.class)))
            .thenThrow(new ResourceNotFoundException("One or more products not found"));

        ResultActions result = mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testOrderRequest)));

        result.andDo(print())
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenProductOutOfStock() throws Exception {
        when(orderService.createOrder(any(OrderRequestDto.class)))
            .thenThrow(new InsufficientStockException("Product 'Laptop' is out of stock"));

        ResultActions result = mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testOrderRequest)));

        result.andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnOrderById() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(testOrderResponse);

        ResultActions result = mockMvc.perform(get("/api/v1/orders/{id}", 1)
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isOk());

        verify(orderService, times(1)).getOrderById(1L);
    }

    @Test
    void shouldReturn404WhenOrderNotFound() throws Exception {
        when(orderService.getOrderById(999L))
            .thenThrow(new ResourceNotFoundException("Order not found with id: 999"));

        ResultActions result = mockMvc.perform(get("/api/v1/orders/{id}", 999)
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnAllOrders() throws Exception {
        when(orderService.getAllOrders()).thenReturn(testOrderResponseList);

        ResultActions result = mockMvc.perform(get("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isOk());

        verify(orderService, times(1)).getAllOrders();
    }

    @Test
    void shouldReturnEmptyList() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of());

        ResultActions result = mockMvc.perform(get("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isOk());

        verify(orderService, times(1)).getAllOrders();
    }

    @Test
    void shouldReturnOrdersByCustomer() throws Exception {
        when(orderService.getOrdersByCustomer(1L)).thenReturn(testOrderResponseList);

        ResultActions result = mockMvc.perform(get("/api/v1/orders/customer/{customerId}", 1)
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isOk());

        verify(orderService, times(1)).getOrdersByCustomer(1L);
    }

    @Test
    void shouldReturnEmptyListWhenNoOrders() throws Exception {
        when(orderService.getOrdersByCustomer(999L)).thenReturn(List.of());

        ResultActions result = mockMvc.perform(get("/api/v1/orders/customer/{customerId}", 999)
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isOk());

        verify(orderService, times(1)).getOrdersByCustomer(999L);
    }

    @Test
    void shouldUpdateOrderStatus() throws Exception {
        OrderResponseDto updatedResponse = testOrderResponse;
        updatedResponse.setOrderStatus(OrderStatus.SHIPPED);

        when(orderService.updateOrderStatus(eq(1L), eq("SHIPPED"))).thenReturn(updatedResponse);

        ResultActions result = mockMvc.perform(patch("/api/v1/orders/{id}/status", 1)
            .param("status", "SHIPPED")
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isOk());

        verify(orderService, times(1)).updateOrderStatus(1L, "SHIPPED");
    }

    @Test
    void shouldReturn400WhenInvalidStatus() throws Exception {
        when(orderService.updateOrderStatus(eq(1L), eq("INVALID")))
            .thenThrow(new IllegalArgumentException("Invalid status"));

        ResultActions result = mockMvc.perform(patch("/api/v1/orders/{id}/status", 1)
            .param("status", "INVALID")
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnOrderAsJson() throws Exception {
        String expectedJson = "{\"orderId\":1,\"status\":\"PENDING\"}";
        Order order = new Order();

        when(orderService.getOrderById(1L)).thenReturn(testOrderResponse);
        when(orderService.convertFromJson(anyString())).thenReturn(order);
        when(orderService.convertToJson(order)).thenReturn(expectedJson);

        ResultActions result = mockMvc.perform(get("/api/v1/orders/{id}/json", 1)
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string(expectedJson));

        verify(orderService, times(1)).getOrderById(1L);
        verify(orderService, times(1)).convertFromJson(anyString());
        verify(orderService, times(1)).convertToJson(any(Order.class));
    }
}
