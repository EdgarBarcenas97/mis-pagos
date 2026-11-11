package dev.base.app.feature.home.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import dev.base.app.feature.home.domain.Tip

@Entity(tableName = "tips")
data class TipEntity(@PrimaryKey val id: Int, val title: String)

@Dao
interface TipDao {
    @Query("SELECT * FROM tips ORDER BY id")
    suspend fun getAll(): List<TipEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tips: List<TipEntity>)
}

fun TipEntity.toDomain() = Tip(id = id, title = title)

fun Tip.toEntity() = TipEntity(id = id, title = title)
