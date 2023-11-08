package ru.practicum.android.diploma.filter.domain.useCase

import ru.practicum.android.diploma.common.domain.model.filter_models.CountryFilter

interface SetCountryFilterUseCase {
    fun execute(country: CountryFilter)
}