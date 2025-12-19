package com.example.speedfinder.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// 1. TABLE (Hamara Data yahan save hoga)
@Entity(tableName = "daily_usage")
data class DailyUsage(
    @PrimaryKey val date: String, // Format: "2023-12-19" (Aaj ki tareekh)
    val wifiBytes: Long = 0,      // WiFi Data
    val mobileBytes: Long = 0     // Mobile Data
)

// 2. DAO (Data nikalne aur dalne ke tools)
@Dao
interface UsageDao {
    // Aaj ka data mangwane ke liye
    @Query("SELECT * FROM daily_usage WHERE date = :date")
    fun getUsageForDate(date: String): Flow<DailyUsage?>

    // Data update ya insert karne ke liye
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(usage: DailyUsage)

    // Sirf Mobile Data barhane ke liye
    @Query("UPDATE daily_usage SET mobileBytes = :bytes WHERE date = :date")
    suspend fun updateMobileUsage(date: String, bytes: Long)

    // Sirf WiFi Data barhane ke liye
    @Query("UPDATE daily_usage SET wifiBytes = :bytes WHERE date = :date")
    suspend fun updateWifiUsage(date: String, bytes: Long)
}

// 3. DATABASE SETUP (Main Connection)
@Database(entities = [DailyUsage::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao

    // Singleton Pattern (Taake puri app mein ek hi database khule)
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "speedfinder_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}