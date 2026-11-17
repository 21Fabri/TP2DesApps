package ar.edu.uade.salastecnicas.sim

import ar.edu.uade.salastecnicas.model.PuntoVerificacion
import ar.edu.uade.salastecnicas.model.Robot

object Rondas {

    const val NOMBRE_RONDA = "Verificación previa de salas técnicas"

    /**
     * Los cinco puntos del recorrido. Los minutos suman 38 y, con 2 minutos de
     * traslado por punto, dan una ronda de 48 minutos simulados.
     */
    val PUNTOS = listOf(
        PuntoVerificacion(
            id = "P1_TABLERO_GENERAL",
            sala = "Sala de tablero general",
            queVerifica = "Ausencia de tensión, temperatura de barras, olor a quemado",
            minutosVerificacion = 8,
            temperaturaNormalC = 38.5
        ),
        PuntoVerificacion(
            id = "P2_BOMBAS",
            sala = "Sala de bombas y presurización",
            queVerifica = "Estado de bombas, temperatura, pérdidas",
            minutosVerificacion = 8,
            temperaturaNormalC = 32.0
        ),
        PuntoVerificacion(
            id = "P3_UPS",
            sala = "Sala de UPS y racks",
            queVerifica = "Temperatura, estado de baterías, ventilación",
            minutosVerificacion = 7,
            temperaturaNormalC = 41.2
        ),
        PuntoVerificacion(
            id = "P4_ASCENSORES",
            sala = "Sala de máquinas de ascensores",
            queVerifica = "Temperatura, estado de maniobra",
            minutosVerificacion = 8,
            temperaturaNormalC = 29.8
        ),
        PuntoVerificacion(
            id = "P5_GRUPO_ELECTROGENO",
            sala = "Grupo electrógeno",
            queVerifica = "Nivel de combustible, temperatura, estado de arranque",
            minutosVerificacion = 7,
            temperaturaNormalC = 27.4
        )
    )

    const val SALA_PUNTO_SEGURO = "Antesala del pasillo técnico"

    /** Tres equipos, dos no disponibles, para poder mostrar el estado deshabilitado. */
    val ROBOTS = listOf(
        Robot(
            id = "G1-01",
            modelo = "Unitree G1 EDU",
            disponible = true
        ),
        Robot(
            id = "G1-02",
            modelo = "Unitree G1 EDU",
            disponible = false,
            motivoNoDisponible = "En carga: 38 %, faltan 55 min"
        ),
        Robot(
            id = "GO2-01",
            modelo = "Unitree Go2 EDU",
            disponible = false,
            motivoNoDisponible = "Fuera de servicio: sin capacidad de accionamiento"
        )
    )
}
