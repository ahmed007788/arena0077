package com.arena0077.app.di

import com.arena0077.app.data.auth.AuthManager
import com.arena0077.app.data.auth.AuthStorage
import com.arena0077.app.data.chat.ArenaRepository
import com.arena0077.app.data.chat.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideArenaRepository(
        authManager: AuthManager,
        chatRepository: ChatRepository
    ): ArenaRepository = ArenaRepository(authManager, chatRepository)
}
