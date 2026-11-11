package kurotoon.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kurotoon.schedule.ScheduleLoader
import kurotoon.schedule.WeeklySchedule

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class ScheduleViewModel(loader: ScheduleLoader) : ViewModel() {

    /** Null until the first schedule is built. */
    val schedule: StateFlow<WeeklySchedule?> = loader.subscribe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
