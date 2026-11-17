package ar.edu.uade.salastecnicas.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(eventos: List<EventoEntity>)

    @Query("SELECT * FROM eventos ORDER BY tsMs DESC")
    fun observarTodos(): Flow<List<EventoEntity>>

    @Query("DELETE FROM eventos")
    suspend fun borrarTodo()
}
