package com.example.recipevault

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Database(
    entities = [Recipe::class, Step::class, Ingredient::class, IngredientStepCrossRef::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun stepDao(): StepDao
    abstract fun ingredientDao(): IngredientDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app_db")
            .fallbackToDestructiveMigration(false)
            //TODO add migration
            .build()

    @Provides
    fun provideRecipeDao(database: AppDatabase): RecipeDao = database.recipeDao()

    @Provides
    fun provideStepDao(database: AppDatabase): StepDao = database.stepDao()

    @Provides
    fun provideIngredientDao(database: AppDatabase): IngredientDao = database.ingredientDao()
}


@Entity
data class Recipe(
    @PrimaryKey val recipeId: Int,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
)

@Dao
interface RecipeDao {
    @Query("SELECT * FROM Recipe")
    fun getAllRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM Recipe WHERE recipeId = :recipeId")
    fun getRecipeById(recipeId: Int): Recipe?

    @Transaction
    @Query("SELECT * FROM Recipe WHERE recipeId = :recipeId")
    fun getRecipeWithStepsById(recipeId: Int): RecipeWithSteps?

    @Insert
    fun insertAll(vararg recipes: Recipe)

    @Delete
    fun delete(recipe: Recipe)

    @Update
    fun updateAll(vararg recipes: Recipe)
}


@Entity(indices = [Index(value = ["recipe_id", "step_number"], unique = true)])
data class Step(
    @PrimaryKey val stepId: Int,
    @ColumnInfo(name = "step_number") val stepNumber: Int,
    @ColumnInfo(name = "recipe_id") val recipeId: Int,
    @ColumnInfo(name = "instructions") val description: String?,

    )

@Dao
interface StepDao {
    @Query("SELECT * FROM Step WHERE recipe_id = :recipeId ORDER BY step_number ASC")
    fun getStepsByRecipeId(recipeId: Int): List<Step>

    @Query("SELECT * FROM Step WHERE stepId = :stepId")
    fun getStepById(stepId: Int): Step?

    @Transaction
    @Query("SELECT * FROM Step WHERE stepId = :stepId")
    fun getStepWithIngredientsById(stepId: Int): StepWithIngredients?

    @Insert
    fun insertAll(vararg steps: Step)

    @Delete
    fun delete(step: Step)

    @Update
    fun updateAll(vararg steps: Step)
}

data class RecipeWithSteps(
    @Embedded val recipe: Recipe,
    @Relation(
        parentColumn = "recipeId",
        entityColumn = "recipe_id"
    )
    val steps: List<Step>
)


@Entity
data class Ingredient(
    @PrimaryKey val ingredientId: Int,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "quantity") val quantity: String?,
)

@Dao
interface IngredientDao {
    @Query("SELECT * FROM Ingredient")
    fun getAllIngredients(): List<Ingredient>

    @Query("SELECT * FROM Ingredient WHERE ingredientId = :ingredientId")
    fun getIngredientById(ingredientId: Int): Ingredient?

    @Insert
    fun insertAll(vararg ingredients: Ingredient)

    @Delete
    fun delete(ingredient: Ingredient)

    @Update
    fun updateAll(vararg ingredients: Ingredient)

}


@Entity(
    primaryKeys = ["ingredientId", "stepId"],
    indices = [Index(value = ["ingredientId"]), Index(value = ["stepId"])]
)
data class IngredientStepCrossRef(
    val ingredientId: Int,
    val stepId: Int

)

data class StepWithIngredients(
    @Embedded val step: Step,
    @Relation(
        parentColumn = "stepId",
        entityColumn = "ingredientId",
        associateBy = Junction(IngredientStepCrossRef::class)
    )
    val ingredients: List<Ingredient>
)


