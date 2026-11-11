package dev.base.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.base.app.feature.home.data.TipDao
import dev.base.app.feature.home.data.TipEntity

/**
 * Base de datos local. Cada feature que guarde datos agrega aquí su entidad
 * y su DAO, y sube [version] con una migración si la app ya se publicó.
 */
@Database(entities = [TipEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tipDao(): TipDao
}
