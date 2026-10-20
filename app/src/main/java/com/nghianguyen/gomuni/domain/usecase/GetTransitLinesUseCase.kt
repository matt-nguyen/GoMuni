package com.nghianguyen.gomuni.domain.usecase

import com.nghianguyen.gomuni.domain.model.TransitLine
import com.nghianguyen.gomuni.domain.repository.TransitRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetTransitLinesUseCase
@Inject
constructor(
    private val repository: TransitRepository,
) {
    operator fun invoke(): Flow<List<TransitLine>> {
        return repository.getTransitLines()
    }

    suspend fun ensureLoaded(operatorId: String) {
        repository.ensureTransitLinesLoaded(operatorId)
    }
}
