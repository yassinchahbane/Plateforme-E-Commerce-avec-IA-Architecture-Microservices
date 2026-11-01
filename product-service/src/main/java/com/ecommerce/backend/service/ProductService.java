package com.ecommerce.backend.service;





import com.ecommerce.backend.dto.ProductDTO;
import com.ecommerce.backend.model.Product;
import com.ecommerce.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ProductDTO getProductById(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(()->new RuntimeException("Product not found: " + id));
        return toDTO(product);
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO dto){
        Product product = toEntity(dto);
        Product saved = productRepository.save(product);
        return toDTO(saved);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO dto){
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
                existing.setName(dto.getName());
                existing.setDescription(dto.getDescription());
                existing.setPrice(dto.getPrice());
                existing.setStock(dto.getStock());
                existing.setCategory(dto.getCategory());
                return toDTO(productRepository.save(existing));
    }


    @Transactional
    public void deleteProduct(Long id){
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }



    private ProductDTO toDTO(Product p){
        return ProductDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .stock(p.getStock())
                .category(p.getCategory())
                .build();
    }

    private Product toEntity(ProductDTO dto) {
        return Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .category(dto.getCategory())
                .build();
    }

    public List<ProductDTO> getProductByIds(List<Long> productIds) {
       List <Product> products = productRepository.findAllById(productIds);

      return products.stream()
               .map(this::toDTO)
               .collect(Collectors.toList());
    }
}