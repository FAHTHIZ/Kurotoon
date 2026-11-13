package kurotoon.core.migration.migrations

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.library.LibraryUpdateWorker
import kurotoon.core.migration.Migration
import kurotoon.core.migration.MigrationContext

@Inject
@ContributesIntoSet(AppScope::class)
class SetupLibraryUpdateMigration(
    private val context: Context,
) : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        LibraryUpdateWorker.setupTask(context)
        return true
    }
}
