package ar.edu.uade.salastecnicas

import android.app.Application
import ar.edu.uade.salastecnicas.data.RobotRepository
import ar.edu.uade.salastecnicas.data.SimulatedRobotRepository
import ar.edu.uade.salastecnicas.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Acá se arma el objeto grande de la app, una sola vez.
 *
 * Esto reemplaza a un framework de inyección de dependencias: son pocas
 * líneas y se explican sin anotaciones de por medio.
 */
class SalasApp : Application() {

    private val scope = CoroutineScope(SupervisorJob())

    lateinit var repositorio: RobotRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val base = AppDatabase.crear(this)
        repositorio = SimulatedRobotRepository(scope, base.eventoDao())
        repositorio.arrancar()
    }
}
