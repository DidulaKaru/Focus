package com.focusflow.app.di

import android.content.Context
import com.focusflow.app.repository.AuthRepository
import com.focusflow.app.repository.AuthRepositoryImpl
import com.focusflow.app.repository.ModuleRepository
import com.focusflow.app.repository.ModuleRepositoryImpl
import com.focusflow.app.repository.PomodoroRepository
import com.focusflow.app.repository.PomodoroRepositoryImpl
import com.focusflow.app.repository.TaskRepository
import com.focusflow.app.repository.TaskRepositoryImpl
import com.focusflow.app.repository.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(@ApplicationContext context: Context): AuthRepository {
        return AuthRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideTaskRepository(): TaskRepository {
        return TaskRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideModuleRepository(): ModuleRepository {
        return ModuleRepositoryImpl()
    }

    @Provides
    @Singleton
    fun providePomodoroRepository(): PomodoroRepository {
        return PomodoroRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        @ApplicationContext context: Context
    ): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }
}
