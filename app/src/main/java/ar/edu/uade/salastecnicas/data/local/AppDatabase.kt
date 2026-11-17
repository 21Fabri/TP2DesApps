package ar.edu.uade.salastecnicas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EventoEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventoDao(): EventoDao

    companion object {
        fun crear(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "salas-tecnicas.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
