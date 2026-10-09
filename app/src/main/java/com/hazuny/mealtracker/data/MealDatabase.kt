package com.hazuny.mealtracker.data

import android.content.Context
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.Update
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "meal_entries",
    indices = [Index(value = ["dateKey"]), Index(value = ["eatenAtEpochMillis"])],
)
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val eatenAtEpochMillis: Long,
    val mealTag: String,
    val tagSource: String,
    val foodNameSnapshot: String,
    val quantity: Double,
    val unitSnapshot: String,
    val caloriesPerUnitKcalSnapshot: Double,
    val proteinPerUnitGSnapshot: Double,
    val caloriesKcalSnapshot: Double,
    val proteinGSnapshot: Double,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "food_templates", indices = [Index(value = ["name"])])
data class FoodTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String,
    val caloriesPerUnitKcal: Double,
    val proteinPerUnitG: Double,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caloriesKcal: Double,
    val proteinG: Double,
    val effectiveFromDate: String,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
)

@Dao
interface MealDao {
    @Query("SELECT * FROM meal_entries ORDER BY dateKey DESC, eatenAtEpochMillis ASC, id ASC")
    fun observeAllEntries(): Flow<List<MealEntryEntity>>

    @Query(
        "SELECT * FROM meal_entries WHERE dateKey = :dateKey " +
            "ORDER BY eatenAtEpochMillis ASC, id ASC",
    )
    fun observeEntriesForDate(dateKey: String): Flow<List<MealEntryEntity>>

    @Insert
    suspend fun insertEntry(entry: MealEntryEntity)

    @Update
    suspend fun updateEntry(entry: MealEntryEntity)

    @Query("DELETE FROM meal_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("SELECT * FROM food_templates ORDER BY name COLLATE NOCASE ASC")
    fun observeFoodTemplates(): Flow<List<FoodTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFoodTemplate(food: FoodTemplateEntity)

    @Query("DELETE FROM food_templates WHERE id = :foodId")
    suspend fun deleteFoodTemplate(foodId: Long)

    @Query("SELECT * FROM daily_goals ORDER BY effectiveFromDate DESC, id DESC LIMIT 1")
    fun observeGoal(): Flow<DailyGoalEntity?>

    @Insert
    suspend fun saveGoal(goal: DailyGoalEntity)
}

@Database(
    entities = [MealEntryEntity::class, FoodTemplateEntity::class, DailyGoalEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MealDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao

    companion object {
        fun create(context: Context): MealDatabase =
            Room.databaseBuilder<MealDatabase>(context, "meal-tracker.db")
                .setDriver(AndroidSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
