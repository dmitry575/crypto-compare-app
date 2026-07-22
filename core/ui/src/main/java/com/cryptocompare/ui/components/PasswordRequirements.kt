package com.cryptocompare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cryptocompare.ui.R
import com.cryptocompare.ui.theme.Dimensions

/** Чек-лист требований к паролю: общий для регистрации и смены пароля. */
@Composable
fun PasswordRequirements(
    lengthMet: Boolean,
    letterMet: Boolean,
    numberMet: Boolean,
) {
    Column(
        modifier = Modifier.padding(top = Dimensions.Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.xs),
    ) {
        RequirementItem(text = stringResource(R.string.password_requirement_length), met = lengthMet)
        RequirementItem(text = stringResource(R.string.password_requirement_letter), met = letterMet)
        RequirementItem(text = stringResource(R.string.password_requirement_number), met = numberMet)
    }
}
