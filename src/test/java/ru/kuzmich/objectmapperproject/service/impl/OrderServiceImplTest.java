package ru.kuzmich.objectmapperproject.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kuzmich.objectmapperproject.dto.OrderRequestDto;
import ru.kuzmich.objectmapperproject.dto.OrderResponseDto;
import ru.kuzmich.objectmapperproject.exception.InsufficientStockException;
import ru.kuzmich.objectmapperproject.exception.ResourceNotFoundException;
import ru.kuzmich.objectmapperproject.model.Customer;
import ru.kuzmich.objectmapperproject.model.Order;
import ru.kuzmich.objectmapperproject.model.OrderStatus;
import ru.kuzmich.objectmapperproject.model.Product;
import ru.kuzmich.objectmapperproject.repository.CustomerRepository;
import ru.kuzmich.objectmapperproject.repository.OrderRepository;
import ru.kuzmich.objectmapperproject.repository.ProductRepository;
import ru.kuzmich.objectmapperproject.service.ProductService;
import ru.kuzmich.objectmapperproject.util.TestDataFactory;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<Customer> customerCaptor;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private Customer testCustomer;
    private List<Product> testProducts;
    private Order testOrder;
    private OrderRequestDto testOrderRequest;
    private List<Long> testProductIds;

    @BeforeEach
    void setUp() {
        testCustomer = TestDataFactory.createDefaultCustomer();
        testProducts = TestDataFactory.createProductList();
        testOrder = TestDataFactory.createDefaultOrder();
        testProductIds = Arrays.asList(1L, 2L, 3L);
        testOrderRequest = TestDataFactory.createOrderRequestDto(1L, testProductIds);
    }

    @Test
    void shouldCreateOrderSuccessfullyForExistingCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(productRepository.findAllById(testProductIds)).thenReturn(testProducts);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        when(productService.convertToDto(any(Product.class)))
            .thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                return TestDataFactory.createProductDto(
                    product.getProductId(),
                    product.getName(),
                    product.getPrice(),
                    product.getQuantityInStock()
                );
            });

        OrderResponseDto result = orderService.createOrder(testOrderRequest);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);

        verify(customerRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).findAllById(testProductIds);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        verify(productRepository, times(3)).save(any(Product.class));
        verify(productService, times(3)).convertToDto(any(Product.class));

        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getCustomer()).isEqualTo(testCustomer);
        assertThat(capturedOrder.getProducts()).hasSize(3);
        assertThat(capturedOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shouldCreateOrderSuccessfullyForGuest() {
        OrderRequestDto guestRequest = TestDataFactory.createGuestOrderRequestDto(
            "i.ivanov@test.ru", testProductIds);

        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        when(productRepository.findAllById(testProductIds)).thenReturn(testProducts);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        when(productService.convertToDto(any(Product.class)))
            .thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                return TestDataFactory.createProductDto(
                    product.getProductId(),
                    product.getName(),
                    product.getPrice(),
                    product.getQuantityInStock()
                );
            });

        OrderResponseDto result = orderService.createOrder(guestRequest);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);

        verify(customerRepository, times(1)).save(customerCaptor.capture());
        Customer capturedCustomer = customerCaptor.getValue();
        assertThat(capturedCustomer.getEmail()).isEqualTo("i.ivanov@test.ru");
        assertThat(capturedCustomer.getFirstName()).isEqualTo("Guest");
        assertThat(capturedCustomer.getLastName()).isEqualTo("User");
        assertThat(capturedCustomer.getContactNumber()).isEqualTo("+1234567890");

        verify(orderRepository, times(1)).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getCustomer()).isEqualTo(testCustomer);
        assertThat(capturedOrder.getProducts()).hasSize(3);
        assertThat(capturedOrder.getShippingAddress()).isEqualTo("123 Test St, Test City");

        verify(productRepository, times(3)).save(any(Product.class));

        verify(productService, times(3)).convertToDto(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenNoCustomerIdOrEmail() {
        OrderRequestDto invalidRequest = new OrderRequestDto();
        invalidRequest.setProductIds(testProductIds);
        invalidRequest.setShippingAddress("123 Test St");

        assertThatThrownBy(() -> orderService.createOrder(invalidRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Either customer ID or email must be provided");

        verify(customerRepository, never()).findById(any());
        verify(customerRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCustomerNotFound() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(
            TestDataFactory.createOrderRequestDto(999L, testProductIds)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Customer not found with id: 999");

        verify(productRepository, never()).findAllById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenProductsNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(productRepository.findAllById(testProductIds)).thenReturn(
            List.of(testProducts.get(0)));

        assertThatThrownBy(() -> orderService.createOrder(testOrderRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("One or more products not found");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenProductOutOfStock() {
        Product outOfStockProduct = TestDataFactory.createProduct(1L, "Laptop",
            BigDecimal.valueOf(999.99), 0);
        List<Product> productsWithOutOfStock = Arrays.asList(
            outOfStockProduct,
            testProducts.get(1),
            testProducts.get(2)
        );

        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(productRepository.findAllById(testProductIds)).thenReturn(productsWithOutOfStock);

        assertThatThrownBy(() -> orderService.createOrder(testOrderRequest))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("Product 'Laptop' is out of stock");

        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenRequestIsNull() {
        assertThatThrownBy(() -> orderService.createOrder(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Order request cannot be null");

        verify(customerRepository, never()).findById(any());
        verify(customerRepository, never()).save(any());
        verify(productRepository, never()).findAllById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldReturnOrderWhenIdExists() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        when(productService.convertToDto(any(Product.class)))
            .thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                return TestDataFactory.createProductDto(
                    product.getProductId(),
                    product.getName(),
                    product.getPrice(),
                    product.getQuantityInStock()
                );
            });

        OrderResponseDto result = orderService.getOrderById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);

        verify(orderRepository, times(1)).findById(1L);
        verify(productService, times(3)).convertToDto(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenIdNotExists() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Order not found with id: 999");

        verify(orderRepository, times(1)).findById(999L);
    }

    @Test
    void shouldReturnAllOrders() {
        List<Order> orders = Arrays.asList(
            TestDataFactory.createOrder(1L, testCustomer, testProducts, OrderStatus.PENDING),
            TestDataFactory.createOrder(2L, testCustomer, testProducts, OrderStatus.SHIPPED)
        );

        when(orderRepository.findAll()).thenReturn(orders);

        when(productService.convertToDto(any(Product.class)))
            .thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                return TestDataFactory.createProductDto(
                    product.getProductId(),
                    product.getName(),
                    product.getPrice(),
                    product.getQuantityInStock()
                );
            });

        List<OrderResponseDto> result = orderService.getAllOrders();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOrderId()).isEqualTo(1L);
        assertThat(result.get(0).getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.get(1).getOrderId()).isEqualTo(2L);
        assertThat(result.get(1).getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);

        verify(orderRepository, times(1)).findAll();
        verify(productService, times(6)).convertToDto(any(Product.class));
    }

    @Test
    void shouldReturnEmptyListWhenNoOrders() {
        when(orderRepository.findAll()).thenReturn(List.of());

        List<OrderResponseDto> result = orderService.getAllOrders();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnOrdersByCustomer() {
        List<Order> orders = Arrays.asList(
            TestDataFactory.createOrder(1L, testCustomer, testProducts, OrderStatus.PENDING),
            TestDataFactory.createOrder(2L, testCustomer, testProducts, OrderStatus.DELIVERED)
        );

        when(orderRepository.findByCustomerCustomerId(1L)).thenReturn(orders);

        when(productService.convertToDto(any(Product.class)))
            .thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                return TestDataFactory.createProductDto(
                    product.getProductId(),
                    product.getName(),
                    product.getPrice(),
                    product.getQuantityInStock()
                );
            });

        List<OrderResponseDto> result = orderService.getOrdersByCustomer(1L);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        assertThat(result.get(0).getOrderId()).isEqualTo(1L);
        assertThat(result.get(0).getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.get(0).getCustomerInfo()).isNotNull();
        assertThat(result.get(0).getCustomerInfo().getFullName()).isEqualTo("Ivan Ivanov");
        assertThat(result.get(0).getProducts()).hasSize(3);

        assertThat(result.get(1).getOrderId()).isEqualTo(2L);
        assertThat(result.get(1).getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(result.get(1).getCustomerInfo()).isNotNull();
        assertThat(result.get(1).getProducts()).hasSize(3);

        verify(orderRepository, times(1)).findByCustomerCustomerId(1L);
        verify(productService, times(6)).convertToDto(any(Product.class));
    }

    @Test
    void shouldReturnEmptyListWhenCustomerHasNoOrders() {
        when(orderRepository.findByCustomerCustomerId(999L)).thenReturn(List.of());

        List<OrderResponseDto> result = orderService.getOrdersByCustomer(999L);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(orderRepository, times(1)).findByCustomerCustomerId(999L);
    }

    @Test
    void shouldUpdateOrderStatusSuccessfully() {
        Order order = TestDataFactory.createOrder(1L, testCustomer, testProducts, OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        when(productService.convertToDto(any(Product.class)))
            .thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                return TestDataFactory.createProductDto(
                    product.getProductId(),
                    product.getName(),
                    product.getPrice(),
                    product.getQuantityInStock()
                );
            });

        OrderResponseDto result = orderService.updateOrderStatus(1L, "SHIPPED");

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(result.getCustomerInfo()).isNotNull();
        assertThat(result.getProducts()).hasSize(3);

        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(orderCaptor.capture());

        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);

        verify(productService, times(3)).convertToDto(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(999L, "SHIPPED"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Order not found with id: 999");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenInvalidStatus() {
        Order order = TestDataFactory.createOrder(1L, testCustomer, testProducts,
            OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "INVALID_STATUS"))
            .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldConvertOrderToResponseDto() {
        when(productService.convertToDto(any(Product.class)))
            .thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                return TestDataFactory.createProductDto(
                    product.getProductId(),
                    product.getName(),
                    product.getPrice(),
                    product.getQuantityInStock()
                );
            });

        OrderResponseDto result = orderService.convertToResponseDto(testOrder);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getCustomerInfo()).isNotNull();
        assertThat(result.getCustomerInfo().getFullName()).isEqualTo("Ivan Ivanov");
        assertThat(result.getProducts()).hasSize(3);

        verify(productService, times(3)).convertToDto(any(Product.class));
    }

    @Test
    void shouldConvertOrderToJson() throws Exception {
        String expectedJson = "{\"orderId\":1,\"status\":\"PENDING\"}";
        when(objectMapper.writeValueAsString(testOrder)).thenReturn(expectedJson);

        String result = orderService.convertToJson(testOrder);

        assertThat(result).isEqualTo(expectedJson);
        verify(objectMapper, times(1)).writeValueAsString(testOrder);
    }

    @Test
    void shouldConvertJsonToOrder() throws Exception {
        String json = "{\"orderId\":1,\"status\":\"PENDING\"}";
        when(objectMapper.readValue(json, Order.class)).thenReturn(testOrder);

        Order result = orderService.convertFromJson(json);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        verify(objectMapper, times(1)).readValue(json, Order.class);
    }

    @Test
    void shouldThrowExceptionWhenJsonConversionFails() throws Exception {
        when(objectMapper.writeValueAsString(testOrder)).thenThrow(
            new RuntimeException("JSON Error"));

        assertThatThrownBy(() -> orderService.convertToJson(testOrder))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Error converting order to JSON");
    }
}
