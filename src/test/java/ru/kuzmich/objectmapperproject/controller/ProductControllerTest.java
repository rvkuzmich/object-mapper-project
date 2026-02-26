package ru.kuzmich.objectmapperproject.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.kuzmich.objectmapperproject.dto.ProductDto;
import ru.kuzmich.objectmapperproject.exception.ResourceNotFoundException;
import ru.kuzmich.objectmapperproject.model.Product;
import ru.kuzmich.objectmapperproject.service.ProductService;
import ru.kuzmich.objectmapperproject.util.TestDataFactory;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private List<ProductDto> productDtoList;
    private ProductDto testProductDto;

    @BeforeEach
    void setUp() {
        productDtoList = TestDataFactory.createProductDtoList();
        testProductDto = TestDataFactory.createProductDto(1L, "Laptop", BigDecimal.valueOf(999.99),
            10);
    }

    @Test
    void shouldReturnAllProducts() throws Exception {
        when(productService.getAllProducts()).thenReturn(productDtoList);

        ResultActions result = mockMvc.perform(get("/api/v1/products")
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isOk());

        verify(productService, times(1)).getAllProducts();
    }

    @Test
    void shouldReturnEmptyList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of());

        ResultActions result = mockMvc.perform(get("/api/v1/products")
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(productService, times(1)).getAllProducts();
    }

    @Test
    void shouldReturnProductWhenIdExists() throws Exception {
        when(productService.getProductById(1L)).thenReturn(testProductDto);

        ResultActions result = mockMvc.perform(get("/api/v1/products/{id}", 1)
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isOk());

        verify(productService, times(1)).getProductById(1L);
    }

    @Test
    void shouldReturn404WhenIdNotExists() throws Exception {
        when(productService.getProductById(999L))
            .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

        ResultActions result = mockMvc.perform(get("/api/v1/products/{id}", 999)
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isNotFound());

        verify(productService, times(1)).getProductById(999L);
    }

    @Test
    void shouldCreateProduct() throws Exception {
        ProductDto newProductDto = TestDataFactory.createProductDto(null, "New Product",
            BigDecimal.valueOf(499.99), 20);
        ProductDto savedProductDto = TestDataFactory.createProductDto(4L, "New Product",
            BigDecimal.valueOf(499.99), 20);

        when(productService.createProduct(any(ProductDto.class))).thenReturn(savedProductDto);

        ResultActions result = mockMvc.perform(post("/api/v1/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(newProductDto)));

        result.andDo(print())
            .andExpect(status().isCreated());

        verify(productService, times(1)).createProduct(any(ProductDto.class));
    }

    @Test
    void shouldReturn400WhenNameExists() throws Exception {
        ProductDto newProductDto = TestDataFactory.createProductDto(null, "Laptop",
            BigDecimal.valueOf(999.99), 10);

        when(productService.createProduct(any(ProductDto.class)))
            .thenThrow(new IllegalArgumentException("Product with name 'Laptop' already exists"));

        ResultActions result = mockMvc.perform(post("/api/v1/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(newProductDto)));

        result.andDo(print())
            .andExpect(status().isBadRequest());

        verify(productService, times(1)).createProduct(any(ProductDto.class));
    }

    @Test
    void shouldReturn400WhenInvalidData() throws Exception {
        ProductDto invalidProductDto = new ProductDto();
        invalidProductDto.setName("");
        invalidProductDto.setPrice(null);
        invalidProductDto.setQuantityInStock(-5);

        ResultActions result = mockMvc.perform(post("/api/v1/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidProductDto)));

        result.andDo(print())
            .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any());
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        ProductDto updateDto = TestDataFactory.createProductDto(1L, "Updated Laptop",
            BigDecimal.valueOf(899.99), 15);

        when(productService.updateProduct(eq(1L), any(ProductDto.class))).thenReturn(updateDto);

        ResultActions result = mockMvc.perform(put("/api/v1/products/{id}", 1)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)));

        result.andDo(print())
            .andExpect(status().isOk());

        verify(productService, times(1)).updateProduct(eq(1L), any(ProductDto.class));
    }

    @Test
    void shouldReturn404WhenProductNotFound() throws Exception {
        ProductDto updateDto = TestDataFactory.createProductDto(999L, "Updated",
            BigDecimal.valueOf(100), 5);

        when(productService.updateProduct(eq(999L), any(ProductDto.class)))
            .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

        ResultActions result = mockMvc.perform(put("/api/v1/products/{id}", 999)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateDto)));

        result.andDo(print())
            .andExpect(status().isNotFound());

        verify(productService, times(1)).updateProduct(eq(999L), any(ProductDto.class));
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        ResultActions result = mockMvc.perform(delete("/api/v1/products/{id}", 1)
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isNoContent());

        verify(productService, times(1)).deleteProduct(1L);
    }

    @Test
    void shouldReturnProductAsJson() throws Exception {
        Product product = TestDataFactory.createProduct(1L, "Laptop", BigDecimal.valueOf(999.99),
            10);
        String expectedJson = "{\"productId\":1,\"name\":\"Laptop\",\"price\":999.99}";

        when(productService.getProductById(1L)).thenReturn(testProductDto);
        when(productService.convertToEntity(testProductDto)).thenReturn(product);
        when(productService.convertToJson(product)).thenReturn(expectedJson);

        ResultActions result = mockMvc.perform(get("/api/v1/products/{id}/json", 1)
            .contentType(MediaType.APPLICATION_JSON));

        result.andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string(expectedJson));

        verify(productService, times(1)).getProductById(1L);
        verify(productService, times(1)).convertToEntity(testProductDto);
        verify(productService, times(1)).convertToJson(product);
    }
}