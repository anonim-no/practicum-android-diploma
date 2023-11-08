package ru.practicum.android.diploma.filter.data.dataSource

import ru.practicum.android.diploma.common.domain.model.filter_models.AreaFilter
import ru.practicum.android.diploma.common.domain.model.filter_models.CountryFilter
import ru.practicum.android.diploma.common.domain.model.filter_models.Filter
import ru.practicum.android.diploma.common.domain.model.filter_models.IndustryFilter

interface FiltersLocalDataSource {
    fun getFilterOptions(): Filter?
    fun setCountry(country: CountryFilter)
    fun setArea(area: AreaFilter?)
    fun setIndustry(industry: IndustryFilter)
    fun setSalary(salary: Int)
    fun setOnlyWithSalary(option: Boolean)
    fun clearFilterOptions()
    fun clearArea()
    fun clearIndustry()
    fun clearSalary()
    fun clearTempFilterOptions()
    fun isTempFilterOptionsEmpty(): Boolean
    fun addFilterToCache(filter: Filter?)
    fun isTempFilterOptionsExists(): Boolean
}