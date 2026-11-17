package ar.edu.uade.salastecnicas.sim

import ar.edu.uade.salastecnicas.model.Alerta
import ar.edu.uade.salastecnicas.model.ClaseEvento
import ar.edu.uade.salastecnicas.model.DecisionAnomalia
import ar.edu.uade.salastecnicas.model.EstadoEnlace
import ar.edu.uade.salastecnicas.model.EstadoOperativo
import ar.edu.uade.salastecnicas.model.EstadoOrden
import ar.edu.uade.salastecnicas.model.Evento
import ar.edu.uade.salastecnicas.model.Habilitacion
import ar.edu.uade.salastecnicas.model.Mision
import ar.edu.uade.salastecnicas.model.Orden
import ar.edu.uade.salastecnicas.model.OrdenEnviada
import ar.edu.uade.salastecnicas.model.Parametros
import ar.edu.uade.salastecnicas.model.PuntoVerificacion
import ar.edu.uade.salastecnicas.model.ResultadoPunto
import ar.edu.uade.salastecnicas.model.ResultadoVerificacion
import ar.edu.uade.salastecnicas.model.Severidad
import ar.edu.uade.salastecnicas.model.Telemetria
import kotlin.math.roundToInt

/**
 * Simulador del robot.
 *
 * No usa Random ni el reloj del sistema: todo avanza por llamadas a [avanzar],
 * de modo que los tests son deterministas y la demo siempre se comporta igual.
 */
class RobotSimulator(
    private val robotId: String = "G1-01",
    private val puntos: List<PuntoVerificacion> = Rondas.PUNTOS
) {

    // ---- Reloj ----
    private var relojMs = 0L

    // ---- Estado del robot ----
    private var estado = EstadoOperativo.EN_ESPERA
    private var enlace = EstadoEnlace.OK
    private var bateria = 100.0
    private var sala = Rondas.SALA_PUNTO_SEGURO
    private var velocidad = 0.0

    // ---- Progreso de la ronda ----
    private var mision: Mision? = null
    private var minutosEnEtapa = 0.0

    // ---- Variables del servicio ----
    private var temperaturaMaxC = 24.0
    private var coberturaPct = 72
    private var presencias = 0
    private var distanciaPresenciaM: Double? = null
    private var margenEstabilidadPct = 88

    // ---- Estado vinculado a los comandos ----
    private var localizacionValida = true
    private var msAccionamientoRestante = 0L
    private var autorizacionVigenteHastaMs: Long? = null
    private var detencionIgnorada = false
    private var msVelocidadCero = 0L
    private var msSinEnlace = 0L
    private var regresoAutonomoDecidido = false
    private var puntoConTemperaturaAlta: String? = null
    private var temperaturaForzadaC = 0.0
    private var advertenciaBateriaEmitida = false

    // ---- Colas ----
    private val alertas = mutableListOf<Alerta>()
    private val ordenes = mutableListOf<OrdenEnviada>()
    private val eventos = mutableListOf<Evento>()
    private var contadorIds = 0

    /** Última telemetría publicada mientras el enlace estuvo disponible. */
    private var ultimaTelemetria: Telemetria? = null

    // =====================================================================
    // Lectura
    // =====================================================================

    fun relojMs(): Long = relojMs

    fun mision(): Mision? = mision

    fun alertas(): List<Alerta> = alertas.toList()

    fun ordenes(): List<OrdenEnviada> = ordenes.toList()

    /** Devuelve los eventos ya recibidos y los saca de la cola. */
    fun drenarEventos(): List<Evento> {
        val recibidos = eventos.filter { it.recibidoTsMs >= 0 }
        eventos.removeAll(recibidos)
        return recibidos
    }

    /**
     * Si el enlace está perdido devuelve la última telemetría conocida, con su
     * timestamp viejo. Eso es lo que hace que la app muestre datos antiguos.
     */
    fun telemetria(): Telemetria {
        val ultima = ultimaTelemetria
        return if (enlace == EstadoEnlace.PERDIDO && ultima != null) {
            // Los datos del robot quedan congelados, pero el estado del enlace
            // se informa al día: es lo único que la app mide por sí misma, y sin
            // eso el operador no podría saber que está mirando datos viejos.
            ultima.copy(enlace = enlace)
        } else {
            construirTelemetria()
        }
    }

    /**
     * Cuánto hace que la telemetría no se actualiza. Con el enlace disponible es 0.
     * Es el número que la app muestra como "hace X s".
     */
    fun antiguedadMs(): Long = relojMs - telemetria().tsMs

    fun datoAntiguo(): Boolean = antiguedadMs() > Parametros.DATO_ANTIGUO_MS

    /**
     * Estado verdadero del robot, incluso si la app no lo está viendo.
     * La UI nunca lo usa: es sólo para los tests y para el Modo demo.
     */
    fun estadoReal(): EstadoOperativo = estado

    fun esperandoDecision(): Boolean = estado == EstadoOperativo.ESPERANDO_DECISION

    /** Verdadero si hay una autorización de supervisor vigente en este momento. */
    fun autorizacionVigente(): Boolean =
        autorizacionVigenteHastaMs?.let { relojMs <= it } ?: false

    private fun construirTelemetria() = Telemetria(
        robotId = robotId,
        tsMs = relojMs,
        enlace = enlace,
        latenciaMs = if (enlace == EstadoEnlace.DEGRADADO) 410 else 84,
        bateriaPct = bateriaPct(),
        autonomiaMin = autonomiaMin(),
        estadoOperativo = estado,
        misionId = mision?.id,
        puntoActualId = mision?.puntoActual?.id,
        puntosCompletados = mision?.completados ?: 0,
        puntosTotales = puntos.size,
        sala = sala,
        velocidadMs = velocidad,
        caminando = velocidad > 0.0,
        temperaturaMaxC = temperaturaMaxC,
        coberturaPct = coberturaPct,
        presencias = presencias,
        distanciaPresenciaM = distanciaPresenciaM,
        margenEstabilidadPct = margenEstabilidadPct,
        alertasActivas = alertas.count { !it.reconocida }
    )

    // =====================================================================
    // Cálculos del servicio
    // =====================================================================

    /** Se redondea para que el consumo acumulado no arrastre error de punto flotante. */
    fun bateriaPct(): Int = bateria.roundToInt()

    fun autonomiaMin(): Int =
        (bateriaPct() / 100.0 * Parametros.AUTONOMIA_TOTAL_MIN).toInt()

    /** Minutos que dura la ronda completa, con traslados. */
    fun minutosRonda(): Int =
        puntos.sumOf { it.minutosVerificacion } + puntos.size * Parametros.TRASLADO_MIN

    /** Minutos que faltan de la ronda en curso, con traslados. */
    fun minutosRestantes(): Int {
        val m = mision ?: return minutosRonda()
        val faltan = m.puntosSinVerificar
        return faltan.sumOf { it.minutosVerificacion } + faltan.size * Parametros.TRASLADO_MIN
    }

    /** Minutos exigidos para habilitar el inicio: ronda + regreso + margen. */
    fun minutosNecesarios(): Int =
        ((minutosRonda() + Parametros.REGRESO_MIN) * (1 + Parametros.MARGEN_BATERIA))
            .roundToInt()

    fun presenciaBloquea(): Boolean {
        val d = distanciaPresenciaM ?: return false
        return presencias > 0 && d < Parametros.DISTANCIA_PRESENCIA_BLOQUEO_M
    }

    // =====================================================================
    // Avance del tiempo
    // =====================================================================

    fun avanzar(dtMs: Long) {
        relojMs += dtMs
        val minutos = dtMs * Parametros.FACTOR_DEMO / 60_000.0

        consumirBateria(minutos)
        avanzarEtapa(minutos)
        avanzarAccionamiento(dtMs)
        verificarEjecuciones(dtMs)
        vencerOrdenes()
        controlarEnlacePerdido(dtMs)
        controlarBateriaBaja()

        if (enlace != EstadoEnlace.PERDIDO) {
            ultimaTelemetria = construirTelemetria()
        }
    }

    private fun consumirBateria(minutos: Double) {
        val trabajando = estado in listOf(
            EstadoOperativo.EN_TRASLADO,
            EstadoOperativo.VERIFICANDO_PUNTO,
            EstadoOperativo.RETORNANDO
        )
        val factor = if (trabajando) 1.0 else 0.2
        val consumo = minutos / Parametros.AUTONOMIA_TOTAL_MIN * 100.0 * factor
        bateria = (bateria - consumo).coerceAtLeast(0.0)
    }

    private fun avanzarEtapa(minutos: Double) {
        when (estado) {
            EstadoOperativo.EN_TRASLADO -> {
                minutosEnEtapa += minutos
                if (minutosEnEtapa >= Parametros.TRASLADO_MIN) {
                    minutosEnEtapa = 0.0
                    empezarVerificacion()
                }
            }

            EstadoOperativo.VERIFICANDO_PUNTO -> {
                val punto = mision?.puntoActual ?: return
                minutosEnEtapa += minutos
                if (minutosEnEtapa >= punto.minutosVerificacion) {
                    minutosEnEtapa = 0.0
                    terminarVerificacion(punto)
                }
            }

            EstadoOperativo.RETORNANDO -> {
                minutosEnEtapa += minutos
                if (minutosEnEtapa >= Parametros.REGRESO_MIN) {
                    minutosEnEtapa = 0.0
                    llegarAPuntoSeguro()
                }
            }

            else -> Unit
        }
    }

    private fun avanzarAccionamiento(dtMs: Long) {
        if (msAccionamientoRestante > 0) {
            msAccionamientoRestante = (msAccionamientoRestante - dtMs).coerceAtLeast(0L)
        }
    }

    private fun empezarVerificacion() {
        val punto = mision?.puntoActual ?: return
        estado = EstadoOperativo.VERIFICANDO_PUNTO
        velocidad = 0.0
        sala = punto.sala
        temperaturaMaxC = if (punto.id == puntoConTemperaturaAlta) temperaturaForzadaC
        else punto.temperaturaNormalC
    }

    private fun terminarVerificacion(punto: PuntoVerificacion) {
        val enFalla = temperaturaMaxC >= Parametros.TEMPERATURA_FALLA_C
        val resultado = ResultadoVerificacion(
            puntoId = punto.id,
            resultado = if (enFalla) ResultadoPunto.FALLA else ResultadoPunto.OK,
            temperaturaMaxC = temperaturaMaxC,
            ausenciaTensionConfirmada = !enFalla
        )
        val m = mision ?: return
        mision = m.copy(resultados = m.resultados + resultado)
        registrarEvento(
            ClaseEvento.PUNTO_COMPLETADO,
            "${resultado.puntoId}: ${resultado.resultado} a " +
                formatearGrados(resultado.temperaturaMaxC)
        )

        if (enFalla) {
            // El robot se detiene y espera: no decide solo.
            estado = EstadoOperativo.ESPERANDO_DECISION
            velocidad = 0.0
            emitirAlerta(
                Severidad.CRITICA,
                "Temperatura fuera de rango en ${punto.sala}",
                "Lectura de ${formatearGrados(temperaturaMaxC)}, umbral " +
                    "${formatearGrados(Parametros.TEMPERATURA_FALLA_C)}. " +
                    "Requiere decisión del operador."
            )
            return
        }

        if (mision!!.completa) empezarRegreso() else empezarTraslado()
    }

    private fun empezarTraslado() {
        estado = EstadoOperativo.EN_TRASLADO
        velocidad = Parametros.VELOCIDAD_MARCHA_MS
        minutosEnEtapa = 0.0
    }

    private fun empezarRegreso() {
        estado = EstadoOperativo.RETORNANDO
        velocidad = Parametros.VELOCIDAD_MARCHA_MS
        minutosEnEtapa = 0.0
    }

    private fun llegarAPuntoSeguro() {
        estado = EstadoOperativo.EN_ESPERA
        velocidad = 0.0
        sala = Rondas.SALA_PUNTO_SEGURO
        temperaturaMaxC = 24.0
        val m = mision
        if (m != null && !m.cerrada) {
            mision = m.copy(cerrada = true)
            registrarEvento(
                ClaseEvento.RONDA_CERRADA,
                if (m.completa) "Ronda completa: ${m.completados} de ${m.total} puntos"
                else "Ronda incompleta: faltaron ${m.puntosSinVerificar.size} puntos"
            )
        }
    }

    // =====================================================================
    // Inicio de ronda
    // =====================================================================

    /**
     * Arranca una ronda sin pasar por la evaluación de habilitación.
     * Lo usa el simulador internamente; la app siempre entra por [enviar].
     */
    fun iniciarRonda() {
        advertenciaBateriaEmitida = false
        contadorIds++
        mision = Mision(
            id = "RND-%03d".format(contadorIds),
            nombre = Rondas.NOMBRE_RONDA,
            puntos = puntos
        )
        empezarTraslado()
        registrarEvento(ClaseEvento.RONDA_INICIADA, Rondas.NOMBRE_RONDA)
    }

    // =====================================================================
    // Habilitación de comandos
    // =====================================================================

    private fun no(motivo: String, detalle: String) = Habilitacion(false, motivo, detalle)

    /**
     * Única fuente de verdad sobre si una orden se puede enviar.
     * La UI la usa para deshabilitar botones y el simulador para rechazar.
     */
    fun habilitacion(orden: Orden, codigoSupervisor: String? = null): Habilitacion =
        when (orden) {

            // La parada no tiene condiciones: sería inaceptable rechazarla.
            Orden.DETENCION_OPERATIVA -> Habilitacion(true)

            Orden.INICIAR_MISION -> when {
                estado != EstadoOperativo.EN_ESPERA ->
                    no("ESTADO_INVALIDO", "El robot no está en espera")
                enlace != EstadoEnlace.OK ->
                    no("ENLACE_NO_OK", "El enlace no está en OK")
                alertas.any { it.severidad == Severidad.CRITICA && !it.reconocida } ->
                    no("ALERTA_CRITICA_PENDIENTE", "Hay una alerta crítica sin reconocer")
                autonomiaMin() < minutosNecesarios() -> no(
                    "BATERIA_INSUFICIENTE",
                    "Ronda ${minutosRonda()} min + regreso ${Parametros.REGRESO_MIN} min; " +
                        "autonomía ${autonomiaMin()} min"
                )
                else -> Habilitacion(true)
            }

            Orden.PAUSAR -> when {
                // Se revisa antes que el estado: cortar un accionamiento a medio
                // camino es peor que no poder pausar.
                msAccionamientoRestante > 0 -> no(
                    "ACCIONAMIENTO_EN_CURSO",
                    "No se puede pausar durante el accionamiento del seccionador"
                )
                estado !in listOf(
                    EstadoOperativo.EN_TRASLADO,
                    EstadoOperativo.VERIFICANDO_PUNTO,
                    EstadoOperativo.RETORNANDO
                ) -> no("ESTADO_INVALIDO", "No hay un avance en curso que pausar")
                else -> Habilitacion(true)
            }

            Orden.REANUDAR -> when {
                estado != EstadoOperativo.PAUSADO ->
                    no("ESTADO_INVALIDO", "El robot no está pausado")
                enlace != EstadoEnlace.OK ->
                    no("ENLACE_NO_OK", "El enlace no está en OK")
                presenciaBloquea() -> no(
                    "PRESENCIA_DETECTADA",
                    "Presencia detectada a ${formatearMetros(distanciaPresenciaM)} " +
                        "en sector no cubierto"
                )
                autonomiaMin() < minutosRestantes() + Parametros.REGRESO_MIN -> no(
                    "BATERIA_INSUFICIENTE",
                    "Faltan ${minutosRestantes()} min + regreso " +
                        "${Parametros.REGRESO_MIN} min; autonomía ${autonomiaMin()} min"
                )
                else -> Habilitacion(true)
            }

            Orden.CANCELAR -> {
                val m = mision
                if (m == null || m.cerrada) no("SIN_MISION_ACTIVA", "No hay una ronda en curso")
                else Habilitacion(true)
            }

            Orden.REGRESO_PUNTO_SEGURO -> when {
                estado == EstadoOperativo.EN_FALLA ->
                    no("ROBOT_EN_FALLA", "El robot no está en condiciones de caminar")
                !localizacionValida ->
                    no("SIN_LOCALIZACION", "Sin localización válida desde la posición actual")
                estado == EstadoOperativo.EN_ESPERA ->
                    no("YA_EN_PUNTO_SEGURO", "El robot ya está en el punto seguro")
                else -> Habilitacion(true)
            }

            Orden.RECONOCER_ALERTAS ->
                if (alertas.none { !it.reconocida })
                    no("SIN_ALERTAS", "No hay alertas sin reconocer")
                else Habilitacion(true)

            Orden.ACCIONAR_SECCIONADOR -> when {
                // El enlace se revisa primero: sin enlace confiable no se acciona
                // nada sobre infraestructura eléctrica.
                enlace != EstadoEnlace.OK -> no("ENLACE_DEGRADADO", "Enlace degradado")
                estado != EstadoOperativo.ESPERANDO_DECISION -> no(
                    "FUERA_DE_SUBPROCESO",
                    "Sólo disponible durante el subproceso de anomalía"
                )
                codigoSupervisor != Parametros.CODIGO_SUPERVISOR ->
                    no("CODIGO_INVALIDO", "Código de supervisor inválido")
                !autorizacionVigente() -> no("AUTORIZACION_VENCIDA", "Autorización vencida")
                else -> Habilitacion(true)
            }
        }

    // =====================================================================
    // Envío de órdenes
    // =====================================================================

    /**
     * Evalúa la habilitación, responde y aplica el efecto.
     * CONFIRMADA significa que el robot recibió la orden, no que la ejecutó.
     */
    fun enviar(orden: Orden, codigoSupervisor: String? = null): OrdenEnviada {
        contadorIds++
        val comandoId = "CMD-%04d".format(contadorIds)
        val habilitacion = habilitacion(orden, codigoSupervisor)

        val enviada = when {
            // Sin enlace la orden sale pero nadie la contesta.
            enlace == EstadoEnlace.PERDIDO -> OrdenEnviada(
                comandoId = comandoId,
                orden = orden,
                enviadaTsMs = relojMs,
                estado = EstadoOrden.PENDIENTE,
                criterioEjecucion = criterioDe(orden)
            )

            !habilitacion.habilitada -> OrdenEnviada(
                comandoId = comandoId,
                orden = orden,
                enviadaTsMs = relojMs,
                estado = EstadoOrden.RECHAZADA,
                motivoRechazo = habilitacion.motivo,
                detalleRechazo = habilitacion.detalle
            )

            else -> {
                aplicarEfecto(orden)
                OrdenEnviada(
                    comandoId = comandoId,
                    orden = orden,
                    enviadaTsMs = relojMs,
                    estado = EstadoOrden.CONFIRMADA,
                    criterioEjecucion = criterioDe(orden)
                )
            }
        }

        ordenes.add(enviada)
        registrarEvento(ClaseEvento.ORDEN_ENVIADA, "$orden: ${enviada.estado}")
        return enviada
    }

    private fun aplicarEfecto(orden: Orden) {
        when (orden) {
            Orden.INICIAR_MISION -> iniciarRonda()

            Orden.PAUSAR -> {
                estado = EstadoOperativo.PAUSADO
                velocidad = 0.0
            }

            Orden.REANUDAR -> {
                val puntoActual = mision?.puntoActual
                estado = if (puntoActual != null && sala == puntoActual.sala)
                    EstadoOperativo.VERIFICANDO_PUNTO else EstadoOperativo.EN_TRASLADO
                velocidad = if (estado == EstadoOperativo.EN_TRASLADO)
                    Parametros.VELOCIDAD_MARCHA_MS else 0.0
            }

            Orden.CANCELAR -> {
                val m = mision ?: return
                mision = m.copy(cerrada = true)
                registrarEvento(
                    ClaseEvento.RONDA_CERRADA,
                    "Ronda cancelada: faltaron ${m.puntosSinVerificar.size} puntos. " +
                        "La cuadrilla no puede ingresar"
                )
                empezarRegreso()
            }

            Orden.REGRESO_PUNTO_SEGURO -> empezarRegreso()

            Orden.DETENCION_OPERATIVA -> {
                // Si la falla está forzada, el robot responde pero no se detiene.
                if (!detencionIgnorada) {
                    estado = EstadoOperativo.DETENIDO
                    velocidad = 0.0
                }
            }

            Orden.RECONOCER_ALERTAS -> {
                for (i in alertas.indices) {
                    if (!alertas[i].reconocida) {
                        alertas[i] = alertas[i].copy(reconocida = true)
                    }
                }
            }

            Orden.ACCIONAR_SECCIONADOR -> {
                msAccionamientoRestante = Parametros.ACCIONAMIENTO_MS
            }
        }
    }

    private fun criterioDe(orden: Orden): String = when (orden) {
        Orden.DETENCION_OPERATIVA ->
            "estado DETENIDO con velocidad 0 sostenida " +
                "${Parametros.VERIFICACION_DETENCION_MS / 1000} s"
        Orden.PAUSAR -> "estado operativo igual a PAUSADO"
        Orden.REGRESO_PUNTO_SEGURO -> "estado operativo igual a RETORNANDO"
        Orden.ACCIONAR_SECCIONADOR -> "lectura de ausencia de tensión en el punto"
        Orden.INICIAR_MISION -> "ronda creada y robot en traslado"
        Orden.REANUDAR -> "estado operativo distinto de PAUSADO"
        Orden.CANCELAR -> "ronda marcada como cerrada"
        Orden.RECONOCER_ALERTAS -> "ninguna alerta sin reconocer"
    }

    /**
     * Revisa, orden por orden, si su criterio de ejecución se cumplió.
     * Una orden confirmada que no produce su efecto en el plazo queda marcada
     * como ejecución fallida. Esta es la diferencia entre que el robot acuse
     * recibo y que el robot obedezca.
     */
    private fun verificarEjecuciones(dtMs: Long) {
        msVelocidadCero = if (velocidad == 0.0) msVelocidadCero + dtMs else 0L

        for (i in ordenes.indices) {
            val orden = ordenes[i]
            if (orden.estado != EstadoOrden.CONFIRMADA) continue
            if (orden.ejecucionVerificada || orden.ejecucionFallida) continue

            if (criterioCumplido(orden.orden)) {
                ordenes[i] = orden.copy(ejecucionVerificada = true)
                continue
            }

            if (relojMs - orden.enviadaTsMs > Parametros.PLAZO_VERIFICACION_MS) {
                ordenes[i] = orden.copy(ejecucionFallida = true)
                alDetectarEjecucionFallida(orden)
            }
        }
    }

    private fun criterioCumplido(orden: Orden): Boolean = when (orden) {
        // Exige el estado DETENIDO y no sólo velocidad 0: durante una
        // verificación el robot también está quieto, y eso no es obedecer.
        Orden.DETENCION_OPERATIVA ->
            estado == EstadoOperativo.DETENIDO &&
                msVelocidadCero >= Parametros.VERIFICACION_DETENCION_MS
        Orden.PAUSAR -> estado == EstadoOperativo.PAUSADO
        Orden.REGRESO_PUNTO_SEGURO ->
            estado == EstadoOperativo.RETORNANDO || estado == EstadoOperativo.EN_ESPERA
        Orden.ACCIONAR_SECCIONADOR -> msAccionamientoRestante == 0L
        Orden.INICIAR_MISION -> mision != null
        Orden.REANUDAR -> estado != EstadoOperativo.PAUSADO
        Orden.CANCELAR -> mision?.cerrada == true
        Orden.RECONOCER_ALERTAS -> alertas.none { !it.reconocida }
    }

    private fun alDetectarEjecucionFallida(orden: OrdenEnviada) {
        if (orden.orden != Orden.DETENCION_OPERATIVA) return
        registrarEvento(
            ClaseEvento.DETENCION_NO_EJECUTADA,
            "La orden ${orden.comandoId} fue confirmada pero el robot sigue en movimiento"
        )
        emitirAlerta(
            Severidad.CRITICA,
            "La detención no se ejecutó",
            "El robot confirmó la orden y sigue en movimiento. La detención por " +
                "software no reemplaza una parada física: recurrir al acompañante presencial."
        )
    }

    /** Una orden sin respuesta no se reintenta: queda registrada como tal. */
    private fun vencerOrdenes() {
        for (i in ordenes.indices) {
            val orden = ordenes[i]
            if (orden.estado != EstadoOrden.PENDIENTE) continue
            if (relojMs - orden.enviadaTsMs > Parametros.VENCIMIENTO_ORDEN_MS) {
                ordenes[i] = orden.copy(estado = EstadoOrden.SIN_RESPUESTA)
                registrarEvento(
                    ClaseEvento.ORDEN_RESUELTA,
                    "${orden.orden}: sin respuesta tras " +
                        "${Parametros.VENCIMIENTO_ORDEN_MS / 1000} s"
                )
            }
        }
    }

    /**
     * Medida de procedimiento del riesgo 4: pasado el plazo sin enlace, el robot
     * no se queda esperando órdenes que no llegan, vuelve al punto seguro.
     */
    private fun controlarEnlacePerdido(dtMs: Long) {
        if (enlace != EstadoEnlace.PERDIDO) {
            msSinEnlace = 0L
            return
        }
        msSinEnlace += dtMs
        if (msSinEnlace < Parametros.REGRESO_AUTONOMO_MS) return
        if (regresoAutonomoDecidido) return

        regresoAutonomoDecidido = true
        val puedeVolver = estado !in listOf(
            EstadoOperativo.EN_ESPERA,
            EstadoOperativo.RETORNANDO,
            EstadoOperativo.EN_FALLA
        )
        if (puedeVolver) empezarRegreso()
        registrarEvento(
            ClaseEvento.ORDEN_RESUELTA,
            "Regreso autónomo al punto seguro por pérdida de enlace de " +
                "${Parametros.REGRESO_AUTONOMO_MS / 1000} s"
        )
    }

    /**
     * Escenario 2: si la autonomía ya no cubre lo que falta más el regreso con
     * margen, el robot se pausa y avisa. La decisión de continuar o volver es
     * del operador, y continuar se deshabilita solo cuando ya no alcanza.
     */
    private fun controlarBateriaBaja() {
        if (advertenciaBateriaEmitida) return
        val enRonda = estado in listOf(
            EstadoOperativo.EN_TRASLADO,
            EstadoOperativo.VERIFICANDO_PUNTO
        )
        if (!enRonda) return

        val necesarios = ((minutosRestantes() + Parametros.REGRESO_MIN) *
            (1 + Parametros.MARGEN_BATERIA)).roundToInt()
        if (autonomiaMin() >= necesarios) return

        advertenciaBateriaEmitida = true
        estado = EstadoOperativo.PAUSADO
        velocidad = 0.0
        emitirAlerta(
            Severidad.ADVERTENCIA,
            "Batería baja: ronda pausada",
            "Autonomía ${autonomiaMin()} min. Faltan ${minutosRestantes()} min de ronda " +
                "más ${Parametros.REGRESO_MIN} min de regreso."
        )
    }

    // =====================================================================
    // Subproceso de anomalía
    // =====================================================================

    /** Habilita la ventana de autorización del supervisor si el código es correcto. */
    fun ingresarAutorizacion(codigo: String): Boolean {
        if (codigo != Parametros.CODIGO_SUPERVISOR) return false
        autorizacionVigenteHastaMs = relojMs + Parametros.VIGENCIA_AUTORIZACION_MS
        return true
    }

    /**
     * Resuelve el subproceso con la decisión del operador.
     * Sin esta llamada la ronda queda detenida: el robot no decide solo.
     */
    fun decidir(decision: DecisionAnomalia) {
        if (estado != EstadoOperativo.ESPERANDO_DECISION) return
        when (decision) {
            DecisionAnomalia.MARCAR_Y_CONTINUAR -> {
                registrarEvento(ClaseEvento.ORDEN_RESUELTA, "Decisión: marcar y continuar")
                continuarDespuesDeLaFalla()
            }

            DecisionAnomalia.AISLAR -> {
                confirmarAusenciaDeTension()
                registrarEvento(ClaseEvento.ORDEN_RESUELTA, "Decisión: aislar y continuar")
                continuarDespuesDeLaFalla()
            }

            DecisionAnomalia.ABORTAR -> {
                val m = mision ?: return
                mision = m.copy(cerrada = true)
                registrarEvento(
                    ClaseEvento.RONDA_CERRADA,
                    "Ronda abortada por anomalía: faltaron " +
                        "${m.puntosSinVerificar.size} puntos. La cuadrilla no puede ingresar"
                )
                empezarRegreso()
            }
        }
    }

    private fun continuarDespuesDeLaFalla() {
        if (mision?.completa == true) empezarRegreso() else empezarTraslado()
    }

    /** Tras accionar el seccionador, el punto pasa a tener ausencia de tensión. */
    private fun confirmarAusenciaDeTension() {
        val m = mision ?: return
        val ultimo = m.resultados.lastOrNull() ?: return
        mision = m.copy(
            resultados = m.resultados.dropLast(1) +
                ultimo.copy(ausenciaTensionConfirmada = true)
        )
    }

    // =====================================================================
    // Forzadores de estado, usados por el Modo demo y por los tests
    // =====================================================================

    fun forzarBateria(pct: Double) {
        bateria = pct.coerceIn(0.0, 100.0)
        refrescarTelemetriaSiHayEnlace()
    }

    fun forzarPresencia(distanciaM: Double?) {
        distanciaPresenciaM = distanciaM
        presencias = if (distanciaM == null) 0 else 1
        coberturaPct = if (distanciaM == null) 72 else 54
        refrescarTelemetriaSiHayEnlace()
    }

    fun forzarPerdidaLocalizacion(perdida: Boolean) {
        localizacionValida = !perdida
    }

    /** Escenario 5: el robot confirma la detención pero no la ejecuta. */
    fun forzarDetencionIgnorada(ignorar: Boolean) {
        detencionIgnorada = ignorar
    }

    /** Escenario 4: el punto indicado va a medir una temperatura de falla. */
    fun forzarTemperaturaAlta(puntoId: String, temperaturaC: Double) {
        puntoConTemperaturaAlta = puntoId
        temperaturaForzadaC = temperaturaC
    }

    fun forzarEnlace(nuevo: EstadoEnlace) {
        val anterior = enlace

        // Al cortarse el enlace hay que congelar lo último que la app llegó a
        // ver. Sin esto, si el corte ocurre antes del primer tick la telemetría
        // cae al valor actual y la antigüedad se queda en cero para siempre.
        if (anterior != EstadoEnlace.PERDIDO && nuevo == EstadoEnlace.PERDIDO) {
            ultimaTelemetria = construirTelemetria()
        }

        enlace = nuevo
        if (anterior != EstadoEnlace.PERDIDO && nuevo == EstadoEnlace.PERDIDO) {
            registrarEvento(ClaseEvento.ENLACE_PERDIDO, "Enlace perdido")
        }
        if (anterior == EstadoEnlace.PERDIDO && nuevo != EstadoEnlace.PERDIDO) {
            liberarEventosPendientes()
            msSinEnlace = 0L
            regresoAutonomoDecidido = false
            registrarEvento(ClaseEvento.ENLACE_RECUPERADO, "Enlace recuperado")
            ultimaTelemetria = construirTelemetria()
        }
    }

    /**
     * Si el enlace está perdido no se refresca nada: la app tiene que seguir
     * viendo el dato viejo, que es el punto del escenario 3.
     */
    private fun refrescarTelemetriaSiHayEnlace() {
        if (enlace != EstadoEnlace.PERDIDO) ultimaTelemetria = construirTelemetria()
    }

    // =====================================================================
    // Alertas y eventos
    // =====================================================================

    fun emitirAlerta(severidad: Severidad, titulo: String, detalle: String) {
        contadorIds++
        alertas.add(
            Alerta(
                id = "ALT-%04d".format(contadorIds),
                severidad = severidad,
                titulo = titulo,
                detalle = detalle,
                tsMs = relojMs
            )
        )
        registrarEvento(ClaseEvento.ALERTA_EMITIDA, "$severidad: $titulo")
    }

    private fun registrarEvento(clase: ClaseEvento, detalle: String) {
        contadorIds++
        eventos.add(
            Evento(
                id = "EVT-%04d".format(contadorIds),
                misionId = mision?.id,
                clase = clase,
                tsMs = relojMs,
                // -1 marca "generado durante la desconexión, todavía no recibido"
                recibidoTsMs = if (enlace == EstadoEnlace.PERDIDO) -1L else relojMs,
                detalle = detalle
            )
        )
    }

    /** Al volver el enlace, los eventos generados a ciegas pasan a recibidos. */
    private fun liberarEventosPendientes() {
        for (i in eventos.indices) {
            if (eventos[i].recibidoTsMs < 0) {
                eventos[i] = eventos[i].copy(recibidoTsMs = relojMs)
            }
        }
    }

    // =====================================================================
    // Formato
    // =====================================================================

    private fun formatearGrados(valor: Double): String =
        "%.1f °C".format(valor).replace('.', ',')

    private fun formatearMetros(valor: Double?): String =
        if (valor == null) "—" else "%.1f m".format(valor).replace('.', ',')
}
