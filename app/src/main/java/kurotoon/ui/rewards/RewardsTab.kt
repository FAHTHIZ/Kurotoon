package kurotoon.ui.rewards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.util.system.toast
import kurotoon.rewards.PurchaseResult
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Redeem
import mihon.icons.materialsymbols.roundedfilled.Redeem
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object RewardsTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            return TabOptions(
                index = 2u,
                title = stringResource(MR.strings.kurotoon_tab_rewards),
                icon = rememberVectorPainter(
                    if (isSelected) MaterialSymbols.RoundedFilled.Redeem else MaterialSymbols.Rounded.Redeem,
                ),
            )
        }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val store = remember { context.appGraph.rewardsStore }
        val state by store.state.collectAsStateWithLifecycle(initialValue = store.current)

        RewardsScreen(
            state = state,
            nowMillis = System.currentTimeMillis(),
            onClaimMission = { store.claimMission(it) },
            onBuy = { item ->
                when (store.buy(item.code)) {
                    PurchaseResult.Success -> context.toast(
                        context.stringResource(MR.strings.kurotoon_rewards_unlocked, item.name),
                    )
                    PurchaseResult.NotEnoughPoints -> context.toast(MR.strings.kurotoon_rewards_notEnoughPoints)
                    PurchaseResult.AlreadyOwned, PurchaseResult.UnknownItem -> Unit
                }
            },
        )
    }
}
