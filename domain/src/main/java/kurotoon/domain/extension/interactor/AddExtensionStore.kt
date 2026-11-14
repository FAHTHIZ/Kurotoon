package kurotoon.domain.extension.interactor

import dev.zacsweers.metro.Inject
import kurotoon.domain.extension.StoreLink
import kurotoon.domain.extension.repository.ExtensionStoreRepository

@Inject
class AddExtensionStore(
    private val repository: ExtensionStoreRepository,
) {
    suspend operator fun invoke(indexUrl: String): Result<Unit> {
        return repository.insert(StoreLink.normalize(indexUrl))
    }
}
