package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.AuthUser

interface UserRepository { suspend fun synchronize(user: AuthUser) }
