package kurotoon.ui.shop

import dev.icerock.moko.resources.StringResource
import kurotoon.shop.PackRepository.ImportResult
import kurotoon.shop.Rarity
import kurotoon.shop.ShopKind
import tachiyomi.i18n.MR

internal val ShopKind.label: StringResource
    get() = when (this) {
        ShopKind.AVATAR -> MR.strings.kurotoon_rewards_kind_avatar
        ShopKind.FRAME -> MR.strings.kurotoon_rewards_kind_frame
        ShopKind.THEME -> MR.strings.kurotoon_rewards_kind_theme
        ShopKind.PARTICLES -> MR.strings.kurotoon_rewards_kind_particles
        ShopKind.STICKER -> MR.strings.kurotoon_rewards_kind_sticker
        ShopKind.BADGE -> MR.strings.kurotoon_rewards_kind_badge
        ShopKind.BOOST -> MR.strings.kurotoon_rewards_kind_boost
    }

internal val Rarity.label: StringResource
    get() = when (this) {
        Rarity.COMMON -> MR.strings.kurotoon_rewards_rarity_common
        Rarity.RARE -> MR.strings.kurotoon_rewards_rarity_rare
        Rarity.EPIC -> MR.strings.kurotoon_rewards_rarity_epic
        Rarity.LEGENDARY -> MR.strings.kurotoon_rewards_rarity_legendary
        Rarity.MYTHIC -> MR.strings.kurotoon_rewards_rarity_mythic
    }

internal val ImportResult.Reason.label: StringResource
    get() = when (this) {
        ImportResult.Reason.UNREADABLE -> MR.strings.kurotoon_packs_error_unreadable
        ImportResult.Reason.NO_MANIFEST -> MR.strings.kurotoon_packs_error_noManifest
        ImportResult.Reason.INVALID_MANIFEST -> MR.strings.kurotoon_packs_error_invalid
        ImportResult.Reason.TOO_LARGE -> MR.strings.kurotoon_packs_error_tooLarge
        ImportResult.Reason.RESERVED_ID -> MR.strings.kurotoon_packs_error_reserved
    }
