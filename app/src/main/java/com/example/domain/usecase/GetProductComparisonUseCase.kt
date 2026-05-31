package com.example.domain.usecase

import com.example.domain.model.ComparisonResult
import com.example.domain.model.Product
import com.example.domain.repository.ComparisonRepository
import kotlinx.coroutines.flow.Flow

class GetProductComparisonUseCase(
    private val comparisonRepository: ComparisonRepository
) {
    operator fun invoke(product: Product): Flow<Result<ComparisonResult?>> {
        return comparisonRepository.getProductComparison(product)
    }
}
