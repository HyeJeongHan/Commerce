package com.hjhan.commerce.domain.category.service;

import com.hjhan.commerce.domain.category.dto.CategoryRequest;
import com.hjhan.commerce.domain.category.dto.CategoryResponse;
import com.hjhan.commerce.domain.category.entity.Category;
import com.hjhan.commerce.domain.category.repository.CategoryRepository;
import com.hjhan.commerce.global.exception.BusinessException;
import com.hjhan.commerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Category category = Category.create(request.name(), request.description());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getCategory(id));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        category.update(request.name(), request.description());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.delete(getCategory(id));
    }

    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
