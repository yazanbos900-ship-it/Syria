package com.example.domain.repository

import com.example.domain.model.ComparisonResult
import com.example.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ComparisonRepository {
    fun getProductComparison(product: Product): Flow<Result<ComparisonResult?>>
}
