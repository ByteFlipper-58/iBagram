package org.telegram.messenger.feature.system.localization.data.mapper

import org.telegram.messenger.feature.system.localization.domain.model.PluralQuantity
import java.util.Locale

object LocalizationMapper {

    fun formatString(template: String, vararg args: Any): String {
        return try {
            String.format(Locale.US, template, *args)
        } catch (e: Exception) {
            template
        }
    }

    fun formatPlural(
        quantity: PluralQuantity,
        count: Int,
        one: String,
        few: String? = null,
        many: String? = null,
        other: String,
        zero: String? = null,
        two: String? = null
    ): String {
        val template = when (quantity) {
            PluralQuantity.ZERO -> zero ?: other
            PluralQuantity.ONE -> one
            PluralQuantity.TWO -> two ?: few ?: other
            PluralQuantity.FEW -> few ?: other
            PluralQuantity.MANY -> many ?: other
            PluralQuantity.OTHER -> other
        }
        return formatString(template, count)
    }
}
