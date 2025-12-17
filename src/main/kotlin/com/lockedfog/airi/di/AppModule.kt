package com.lockedfog.airi.di

import com.lockedfog.airi.data.config.SettingsRepository
import com.lockedfog.airi.domain.AgentKernel
import com.lockedfog.airi.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module
val appModule = module {
    single { SettingsRepository() }

    single(createdAtStart = true) { AgentKernel(get()) }

    factory { (scope: CoroutineScope) ->
        SettingsViewModel(
            scope = scope,
            repository = get()
        )
    }
}
