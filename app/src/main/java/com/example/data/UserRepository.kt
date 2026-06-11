package com.example.data

import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    fun getUserById(id: Long): Flow<User?> = userDao.getUserById(id)
    suspend fun getUserByIdDirect(id: Long): User? = userDao.getUserByIdDirect(id)
    suspend fun registerUser(user: User): Long = userDao.insertUser(user)
    suspend fun updateUser(user: User) = userDao.updateUser(user)
}
