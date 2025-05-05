package com.alex.testapp.data.local


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.alex.testapp.data.local.dao.UserDao
import com.alex.testapp.data.local.dao.UserVideoLikeDao
import com.alex.testapp.data.local.dao.VideoDao
import com.alex.testapp.util.AppInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [UserEntity::class, VideoEntity::class, UserVideoLikeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun videoDao(): VideoDao
    abstract fun userVideoLikeDao(): UserVideoLikeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }

        fun populateInitialData(context: Context, scope: CoroutineScope) {
            val db = getDatabase(context)
            scope.launch(Dispatchers.IO) {
                if (db.userDao().getAllUsers().isEmpty()) {
                    db.userDao().insertUsers(
                        listOf(
                            UserEntity(name = "کاربر اول", watchedVideosCount = 0),
                            UserEntity(name = "کاربر دوم", watchedVideosCount = 0)
                        )
                    )
                }
                AppInitializer.isDataReady.complete(true)
            }
        }
    }
}