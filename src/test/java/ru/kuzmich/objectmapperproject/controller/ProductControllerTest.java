package ru.kuzmich.objectmapperproject.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.kuzmich.objectmapperproject.dto.ProductDto;
import ru.kuzmich.objectmapperproject.exception.ResourceNotFoundException;
import ru.kuzmich.objectmapperproject.service.ProductService;
import ru.kuzmich.objectmapperproject.util.TestDataFactory;

@SpringBootTest
@ActiveProfiles("test")
class ProductControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private List<ProductDto> productDtoList;
    private ProductDto testProductDto;

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .alwaysDo(print())
            .build();

        productDtoList = TestDataFactory.createProductDtoList();
        testProductDto = TestDataFactory.createProductDto(1L, "Laptop",
            BigDecimal.valueOf(999.99), 10);
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnAllProducts() throws Exception {
        when(productService.getAllProducts()).thenReturn(productDtoList);

        mockMvc.perform(get("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].name", is("Laptop")));

        verify(productService, times(1)).getAllProducts();
    }

    @Test
    void shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());

        verify(productService, never()).getAllProducts();
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnEmptyList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(productService, times(1)).getAllProducts();
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnProductById() throws Exception {
        when(productService.getProductById(1L)).thenReturn(testProductDto);

        mockMvc.perform(get("/api/v1/products/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId", is(1)))
            .andExpect(jsonPath("$.name", is("Laptop")));

        verify(productService, times(1)).getProductById(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn404WhenProductNotFound() throws Exception {
        when(productService.getProductById(999L))
            .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(get("/api/v1/products/{id}", 999)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        verify(productService, times(1)).getProductById(999L);
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void shouldCreateProductWithAdminRole() throws Exception {
        ProductDto newProductDto = TestDataFactory.createProductDto(null, "New Product",
            BigDecimal.valueOf(499.99), 20);

        when(productService.createProduct(any(ProductDto.class))).thenReturn(testProductDto);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProductDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.productId", is(1)));

        verify(productService, times(1)).createProduct(any(ProductDto.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403WhenUserTriesToCreate() throws Exception {
        ProductDto newProductDto = TestDataFactory.createProductDto(null, "New Product",
            BigDecimal.valueOf(499.99), 20);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProductDto)))
            .andExpect(status().isForbidden());

        verify(productService, never()).createProduct(any());
    }
}