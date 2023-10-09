package ru.practicum.android.diploma.vacancy.di

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ru.practicum.android.diploma.vacancy.data.repositoryImpl.VacancyRepositoryImpl
import ru.practicum.android.diploma.vacancy.domain.repository.VacancyRepository
import ru.practicum.android.diploma.vacancy.domain.useCase.CallPhoneUseCase
import ru.practicum.android.diploma.vacancy.domain.useCase.CallPhoneUseCaseImpl
import ru.practicum.android.diploma.vacancy.domain.useCase.FindVacancyByIdUseCase
import ru.practicum.android.diploma.vacancy.domain.useCase.FindVacancyByIdUseCaseImpl
import ru.practicum.android.diploma.vacancy.domain.useCase.OpenMailUseCase
import ru.practicum.android.diploma.vacancy.domain.useCase.OpenMailUseCaseImpl
import ru.practicum.android.diploma.vacancy.domain.useCase.ShareVacancyByIdUseCase
import ru.practicum.android.diploma.vacancy.domain.useCase.ShareVacancyByIdUseCaseImpl
import ru.practicum.android.diploma.vacancy.ui.navigator.ExternalNavigator
import ru.practicum.android.diploma.vacancy.ui.navigator.ExternalNavigatorImpl
import ru.practicum.android.diploma.vacancy.ui.viewModel.VacancyViewModel

val vacancyModule = module {

    single<ExternalNavigator> { ExternalNavigatorImpl(androidContext()) }
    single<VacancyRepository> { VacancyRepositoryImpl(get(), get(), get(), get()) }
    single<FindVacancyByIdUseCase> { FindVacancyByIdUseCaseImpl(get(), get()) }
    single<OpenMailUseCase> { OpenMailUseCaseImpl(get()) }
    single<ShareVacancyByIdUseCase> { ShareVacancyByIdUseCaseImpl(get()) }
    single<CallPhoneUseCase> { CallPhoneUseCaseImpl(get()) }

    viewModel { (vacancyId: Int) -> VacancyViewModel(vacancyId, get(), get(), get(), get(), get()) }


}