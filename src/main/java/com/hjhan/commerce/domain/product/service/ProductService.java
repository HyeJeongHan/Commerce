package com.hjhan.commerce.domain.product.service;

import com.hjhan.commerce.domain.category.entity.Category;
import com.hjhan.commerce.domain.category.service.CategoryService;
import com.hjhan.commerce.domain.product.dto.ProductCreateRequest;
import com.hjhan.commerce.domain.product.dto.ProductResponse;
import com.hjhan.commerce.domain.product.dto.ProductUpdateRequest;
import com.hjhan.commerce.domain.product.entity.Product;
import com.hjhan.commerce.domain.product.entity.ProductStatus;
import com.hjhan.commerce.domain.product.entity.Stock;
import com.hjhan.commerce.domain.product.repository.ProductRepository;
import com.hjhan.commerce.domain.product.repository.StockRepository;
import com.hjhan.commerce.global.exception.BusinessException;
import com.hjhan.commerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final CategoryService categoryService;

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        Category category = categoryService.getCategory(request.categoryId());
        Product product = Product.create(request.name(), request.description(), request.price(), category);
        productRepository.save(product);

        Stock stock = Stock.create(product, request.stockQuantity());
        stockRepository.save(stock);

        return ProductResponse.from(productRepository.findByIdWithDetails(product.getId()).orElseThrow());
    }

    public Page<ProductResponse> search(String keyword, Long categoryId,
                                        BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.search(keyword, categoryId, minPrice, maxPrice, pageable)
                .map(ProductResponse::from);
    }

    public ProductResponse findById(Long id) {
        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = getProduct(id);
        Category category = categoryService.getCategory(request.categoryId());
        product.update(request.name(), request.description(), request.price(), category);
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = getProduct(id);
        product.changeStatus(ProductStatus.INACTIVE);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
