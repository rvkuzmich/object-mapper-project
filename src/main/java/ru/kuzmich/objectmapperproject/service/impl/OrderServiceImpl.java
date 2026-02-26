package ru.kuzmich.objectmapperproject.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kuzmich.objectmapperproject.dto.OrderRequestDto;
import ru.kuzmich.objectmapperproject.dto.OrderResponseDto;
import ru.kuzmich.objectmapperproject.dto.ProductDto;
import ru.kuzmich.objectmapperproject.exception.InsufficientStockException;
import ru.kuzmich.objectmapperproject.exception.ResourceNotFoundException;
import ru.kuzmich.objectmapperproject.model.Customer;
import ru.kuzmich.objectmapperproject.model.Order;
import ru.kuzmich.objectmapperproject.model.OrderStatus;
import ru.kuzmich.objectmapperproject.model.Product;
import ru.kuzmich.objectmapperproject.repository.CustomerRepository;
import ru.kuzmich.objectmapperproject.repository.OrderRepository;
import ru.kuzmich.objectmapperproject.repository.ProductRepository;
import ru.kuzmich.objectmapperproject.service.OrderService;
import ru.kuzmich.objectmapperproject.service.ProductService;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;

    private final CustomerRepository customerRepository;

    private final ProductRepository productRepository;

    private final ProductService productService;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto orderRequest) {
        if (orderRequest == null) {
            throw new IllegalArgumentException("Order request cannot be null");
        }

        Customer customer = getOrCreateCustomer(orderRequest);

        List<Product> products = getProductsWithStockCheck(orderRequest.getProductIds());

        updateProductStock(products);

        Order order = new Order(customer, products, orderRequest.getShippingAddress());
        Order savedOrder = orderRepository.save(order);

        return convertToResponseDto(savedOrder);
    }

    @Override
    public OrderResponseDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return convertToResponseDto(order);
    }

    @Override
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAll().stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponseDto> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerCustomerId(customerId).stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        order.setOrderStatus(Enum.valueOf(OrderStatus.class, status.toUpperCase()));
        Order updatedOrder = orderRepository.save(order);

        return convertToResponseDto(updatedOrder);
    }

    @Override
    public OrderResponseDto convertToResponseDto(Order order) {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setOrderId(order.getOrderId());
        dto.setOrderDate(order.getOrderDate());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setOrderStatus(order.getOrderStatus());

        OrderResponseDto.CustomerInfoDto customerInfo = new OrderResponseDto.CustomerInfoDto();
        customerInfo.setCustomerId(order.getCustomer().getCustomerId());
        customerInfo.setFullName(order.getCustomer().getFullName());
        customerInfo.setEmail(order.getCustomer().getEmail());
        dto.setCustomerInfo(customerInfo);

        List<ProductDto> productDTOs = order.getProducts().stream()
            .map(productService::convertToDto)
            .collect(Collectors.toList());
        dto.setProducts(productDTOs);

        return dto;
    }

    @Override
    public String convertToJson(Order order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (Exception e) {
            throw new RuntimeException("Error converting order to JSON", e);
        }
    }

    @Override
    public Order convertFromJson(String json) {
        try {
            return objectMapper.readValue(json, Order.class);
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to order", e);
        }
    }

    private Customer getOrCreateCustomer(OrderRequestDto orderRequest) {
        if (orderRequest.getCustomerId() != null) {
            return customerRepository.findById(orderRequest.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + orderRequest.getCustomerId()));
        }

        if (orderRequest.getEmail() != null) {
            Customer customer = new Customer();
            customer.setEmail(orderRequest.getEmail());
            customer.setContactNumber(orderRequest.getContactNumber());
            customer.setFirstName("Guest");
            customer.setLastName("User");

            return customerRepository.save(customer);
        }

        throw new IllegalArgumentException("Either customer ID or email must be provided");
    }

    private List<Product> getProductsWithStockCheck(List<Long> productIds) {
        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            throw new ResourceNotFoundException("One or more products not found");
        }

        for (Product product : products) {
            if (product.getQuantityInStock() <= 0) {
                throw new InsufficientStockException("Product '" + product.getName() + "' is out of stock");
            }
        }

        return products;
    }

    @Transactional
    protected void updateProductStock(List<Product> products) {
        for (Product product : products) {
            product.setQuantityInStock(product.getQuantityInStock() - 1);
            productRepository.save(product);
        }
    }
}
