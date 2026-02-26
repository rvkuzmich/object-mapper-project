package ru.kuzmich.objectmapperproject.service;

import java.util.List;
import ru.kuzmich.objectmapperproject.dto.ProductDto;
import ru.kuzmich.objectmapperproject.model.Product;

public interface ProductService {

    List<ProductDto> getAllProducts();

    ProductDto getProductById(Long id);

    ProductDto createProduct(ProductDto productDTO);

    ProductDto updateProduct(Long id, ProductDto productDTO);

    void deleteProduct(Long id);

    ProductDto convertToDto(Product product);

    Product convertToEntity(ProductDto productDto);

    String convertToJson(Product product);
}
