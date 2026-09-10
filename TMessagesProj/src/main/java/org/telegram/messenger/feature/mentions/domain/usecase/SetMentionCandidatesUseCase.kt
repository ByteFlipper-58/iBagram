package org.telegram.messenger.feature.mentions.domain.usecase

import org.telegram.messenger.feature.mentions.domain.model.MentionCandidate
import org.telegram.messenger.feature.mentions.domain.repository.MentionsRepository

class SetMentionCandidatesUseCase(
    private val repository: MentionsRepository,
    private val filterMentionsUseCase: FilterMentionsUseCase = FilterMentionsUseCase()
) {
    operator fun invoke(candidates: List<MentionCandidate>, query: String = "", limit: Int = 20) {
        val filtered = filterMentionsUseCase(candidates, query, limit)
        repository.setCandidates(filtered)
    }
}
