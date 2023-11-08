package ru.practicum.android.diploma.filter.data.db

import ru.practicum.android.diploma.common.domain.model.filter_models.AreaFilter
import ru.practicum.android.diploma.common.domain.model.filter_models.CountryFilter
import ru.practicum.android.diploma.common.domain.model.filter_models.Filter
import ru.practicum.android.diploma.common.domain.model.filter_models.IndustryFilter

interface FilterLocalCache {
    fun getFilterCache(): Filter?
    fun addFilterToCache(filter: Filter?)
    fun addCountry(country: CountryFilter)
    fun addArea(area: AreaFilter?)
    fun addIndustry(industry: IndustryFilter)
    fun addSalary(salary: Int)
    fun addOnlyWithSalary(isOnlyWithSalary: Boolean)
    fun clearAll()
    fun clearArea()
    fun clearIndustry()
    fun clearSalary()
}