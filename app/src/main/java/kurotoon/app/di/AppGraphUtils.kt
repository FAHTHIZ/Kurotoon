package kurotoon.app.di

import android.content.Context
import kurotoon.core.metro.metroGraph

val Context.appGraph get() = metroGraph<AppGraph>()
