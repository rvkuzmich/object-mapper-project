package ru.kuzmich.objectmapperproject.service;

import java.util.List;
import ru.kuzmich.objectmapperproject.dto.OrderRequestDto;
import ru.kuzmich.objectmapperproject.dto.OrderResponseDto;
import ru.kuzmich.objectmapperproject.model.Order;

public interface OrderService {

    OrderResponseDto createOrder(OrderRequestDto orderRequest);

    OrderResponseDto getOrderById(Long id);

    List<OrderResponseDto> getAllOrders();

    List<OrderResponseDto> getOrdersByCustomer(Long customerId);

    OrderResponseDto updateOrderStatus(Long orderId, String status);

    OrderResponseDto convertToResponseDto(Order order);

    String convertToJson(Order order);

    Order convertFromJson(String json);
}
