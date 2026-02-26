package ru.kuzmich.objectmapperproject.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
import ru.kuzmich.objectmapperproject.dto.ProductDto;
import ru.kuzmich.objectmapperproject.exception.ResourceNotFoundException;
import ru.kuzmich.objectmapperproject.model.Product;
import ru.kuzmich.objectmapperproject.repository.ProductRepository;
import ru.kuzmich.objectmapperproject.util.TestDataFactory;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private Product testProduct;
    private ProductDto testProductDto;

    @BeforeEach
    void setUp() {
        testProduct = TestDataFactory.createProduct(1L, "Laptop", BigDecimal.valueOf(999.99), 10);
        testProductDto = TestDataFactory.createProductDto(1L, "Laptop", BigDecimal.valueOf(999.99),
            10);
    }

    @Test
    void shouldReturnAllProducts() {
        List<Product> products = TestDataFactory.createProductList();
        when(productRepository.findAll()).thenReturn(products);

        when(objectMapper.convertValue(any(Product.class), eq(ProductDto.class)))
            .thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                return TestDataFactory.createProductDto(
                    product.getProductId(),
                    product.getName(),
                    product.getPrice(),
                    product.getQuantityInStock()
                );
            });

        List<ProductDto> result = productService.getAllProducts();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("Laptop");
        assertThat(result.get(1).getName()).isEqualTo("Mouse");
        assertThat(result.get(2).getName()).isEqualTo("Keyboard");

        verify(productRepository, times(1)).findAll();
        verify(objectMapper, times(3)).convertValue(any(Product.class), eq(ProductDto.class));
    }

    @Test
    void shouldReturnEmptyListWhenNoProducts() {
        when(productRepository.findAll()).thenReturn(List.of());

        List<ProductDto> result = productService.getAllProducts();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(productRepository, times(1)).findAll();

        verify(objectMapper, never()).convertValue(any(), (Class<?>) any());
        verify(objectMapper, never()).convertValue(any(), (TypeReference<?>) any());
    }

    @Test
    void shouldReturnProductWhenIdExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(objectMapper.convertValue(testProduct, ProductDto.class)).thenReturn(
            testProductDto);

        ProductDto result = productService.getProductById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Laptop");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(999.99));

        verify(productRepository, times(1)).findById(1L);
        verify(objectMapper, times(1)).convertValue(testProduct, ProductDto.class);
    }

    @Test
    void shouldThrowExceptionWhenIdNotExists() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Product not found with id: 999");

        verify(productRepository, times(1)).findById(999L);

        verify(objectMapper, never()).convertValue(any(), any(Class.class));
        verify(objectMapper, never()).convertValue(any(), any(TypeReference.class));
    }

    @Test
    void shouldCreateProductSuccessfully() {
        ProductDto newProductDto = TestDataFactory.createProductDto(null, "New Product",
            BigDecimal.valueOf(499.99), 20);
        Product newProduct = TestDataFactory.createProduct(null, "New Product",
            BigDecimal.valueOf(499.99), 20);
        Product savedProduct = TestDataFactory.createProduct(4L, "New Product",
            BigDecimal.valueOf(499.99), 20);

        when(productRepository.existsByName("New Product")).thenReturn(false);
        when(objectMapper.convertValue(newProductDto, Product.class)).thenReturn(newProduct);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(objectMapper.convertValue(savedProduct, ProductDto.class))
            .thenReturn(TestDataFactory.createProductDto(4L, "New Product",
                BigDecimal.valueOf(499.99), 20));

        ProductDto result = productService.createProduct(newProductDto);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(4L);
        assertThat(result.getName()).isEqualTo("New Product");

        verify(productRepository, times(1)).existsByName("New Product");
        verify(productRepository, times(1)).save(productCaptor.capture());

        Product capturedProduct = productCaptor.getValue();
        assertThat(capturedProduct.getName()).isEqualTo("New Product");
        assertThat(capturedProduct.getPrice()).isEqualTo(BigDecimal.valueOf(499.99));
    }

    @Test
    void shouldThrowExceptionWhenNameExists() {
        ProductDto existingProductDto = TestDataFactory.createProductDto(null, "Laptop",
            BigDecimal.valueOf(999.99), 10);
        when(productRepository.existsByName("Laptop")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(existingProductDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Product with name 'Laptop' already exists");

        verify(productRepository, times(1)).existsByName("Laptop");
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDtoIsNull() {
        assertThatThrownBy(() -> productService.createProduct(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ProductDto cannot be null");

        verify(productRepository, never()).existsByName(any());
        verify(productRepository, never()).save(any());
        verify(objectMapper, never()).convertValue(any(), (Class<?>) any());
    }

    @Test
    void shouldUpdateProductSuccessfully() {
        ProductDto updateDto = TestDataFactory.createProductDto(1L, "Updated Laptop",
            BigDecimal.valueOf(899.99), 15);

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(objectMapper.convertValue(testProduct, ProductDto.class)).thenReturn(updateDto);

        ProductDto result = productService.updateProduct(1L, updateDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Laptop");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(899.99));
        assertThat(result.getQuantityInStock()).isEqualTo(15);

        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(productCaptor.capture());

        Product capturedProduct = productCaptor.getValue();
        assertThat(capturedProduct.getName()).isEqualTo("Updated Laptop");
        assertThat(capturedProduct.getPrice()).isEqualTo(BigDecimal.valueOf(899.99));
        assertThat(capturedProduct.getQuantityInStock()).isEqualTo(15);
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        ProductDto updateDto = TestDataFactory.createProductDto(999L, "Updated",
            BigDecimal.valueOf(100), 5);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(999L, updateDto))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Product not found with id: 999");

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldDeleteProductSuccessfully() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        productService.deleteProduct(1L);

        verify(productRepository, times(1)).existsById(1L);
        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    void shouldConvertProductToDto() {
        when(objectMapper.convertValue(testProduct, ProductDto.class)).thenReturn(
            testProductDto);

        ProductDto result = productService.convertToDto(testProduct);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Laptop");
    }

    @Test
    void shouldConvertDtoToProduct() {
        when(objectMapper.convertValue(testProductDto, Product.class)).thenReturn(testProduct);

        Product result = productService.convertToEntity(testProductDto);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Laptop");
    }

    @Test
    void shouldConvertProductToJson() throws Exception {
        String expectedJson = "{\"productId\":1,\"name\":\"Laptop\"}";
        when(objectMapper.writeValueAsString(testProduct)).thenReturn(expectedJson);

        String result = productService.convertToJson(testProduct);

        assertThat(result).isEqualTo(expectedJson);
    }

    @Test
    void shouldThrowExceptionWhenJsonConversionFails() throws Exception {
        when(objectMapper.writeValueAsString(testProduct)).thenThrow(
            new RuntimeException("JSON Error"));

        assertThatThrownBy(() -> productService.convertToJson(testProduct))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Error converting product to JSON");
    }
}