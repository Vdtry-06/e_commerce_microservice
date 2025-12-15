package com.vdtry06.ecommerce.product;

import com.vdtry06.ecommerce.exception.ProductPurchaseException;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    
    ProductRepository repository;
    ProductMapper mapper;

    @Transactional
    public Integer createProduct(ProductRequest request) {
        try {
            logger.info("Creating product: {}", request.name());
            var product = mapper.toProduct(request);
            logger.info("Mapped product: id={}, category={}", product.getId(), product.getCategory().getId());
            var savedProduct = repository.save(product);
            logger.info("Product saved successfully with id: {}", savedProduct.getId());
            return savedProduct.getId();
        } catch (Exception e) {
            logger.error("Error creating product: ", e);
            throw new RuntimeException("Failed to create product: " + e.getMessage(), e);
        }
    }

    @Transactional(rollbackFor = ProductPurchaseException.class) // giao dịch hoàn tác nếu có ngoại lệ xảy ra
    public List<ProductPurchaseResponse> purchaseProducts(List<ProductPurchaseRequest> request) {
        // 1. nhận danh sách yêu cầu mua sản phẩm
        var productIds = request
                .stream()
                .map(ProductPurchaseRequest::productId)
                .toList();
        var storedProducts = repository.findAllByIdInOrderById(productIds);

        // 2. kiểm tra tính hợp lệ của danh sách sản phẩm
        if(productIds.size() != storedProducts.size()) {
            throw new ProductPurchaseException("One or more products does not exist");
        }
        // sắp xếp list theo yêu cầu để khớp với danh sách storedRequest
        var sortedRequest = request
                .stream()
                .sorted(Comparator.comparing(ProductPurchaseRequest::productId))
                .toList();
        var purchasedProducts = new ArrayList<ProductPurchaseResponse>();

        for(int i = 0; i < storedProducts.size(); i++) {
            var product = storedProducts.get(i);
            var productRequest = sortedRequest.get(i);

            // 3. cập nhật số lượng hàng tồn kho cho từng sản phẩm
            if(product.getAvailableQuantity() < productRequest.quantity()) {
                throw new ProductPurchaseException("Insufficient stock quantity for product with ID:: " + productRequest.productId());
            }
            var newAvailableQuantity = product.getAvailableQuantity() - productRequest.quantity();
            product.setAvailableQuantity(newAvailableQuantity);
            repository.save(product);

            // 4. Tạo danh sách phản hồi hoặc hoàn tác giao dịch nếu có lỗi xảy ra
            purchasedProducts.add(mapper.toproductPurchaseResponse(product, productRequest.quantity()));
        }
        return purchasedProducts;
    }

    public ProductResponse findById(Integer productId) {
        return repository.findById(productId)
                .map(mapper::toProductResponse)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID:: " + productId));
    }

    public List<ProductResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toProductResponse)
                .collect(Collectors.toList());
    }
}
