# Evidencias de los cinco escenarios

Cada escenario se dispara desde la pantalla **Modo demo** y se documenta con
capturas en orden. Nombre de archivo: `N-escenario-M-descripcion.png`.

## 1. Misión normal

- `1-mision-normal-1-inicio.png` — Control con "Iniciar ronda" habilitado.
- `1-mision-normal-2-progreso.png` — Misión con 3 de 5 puntos en OK.
- `1-mision-normal-3-completa.png` — Historial con la ronda completa.

## 2. Batería baja

- `2-bateria-1-advertencia.png` — Panel con la alerta de advertencia y el robot
  pausado.
- `2-bateria-2-continuar-habilitado.png` — Control con "Reanudar" habilitado.
- `2-bateria-3-continuar-deshabilitado.png` — "Reanudar" deshabilitado con el
  motivo visible: la autonomía ya no cubre lo que falta más el regreso.

## 3. Desconexión

- `3-desconexion-1-dato-antiguo.png` — Panel con el enlace PERDIDO y la
  antigüedad del dato en segundos.
- `3-desconexion-2-sin-respuesta.png` — Control con una orden en SIN_RESPUESTA.
- `3-desconexion-3-reconciliacion.png` — Historial con eventos que muestran el
  momento de origen y el de recepción.

## 4. Incidente

- `4-incidente-1-modal.png` — Modal de anomalía con la lectura de 78,4 °C y los
  tres caminos.
- `4-incidente-2-autorizacion.png` — Modal de autorización de supervisor.
- `4-incidente-3-resultado.png` — Misión con el punto en FALLA y la ausencia de
  tensión confirmada tras aislar.

## 5. Orden confirmada que no se ejecuta

- `5-no-ejecutada-1-confirmada.png` — Orden en CONFIRMADA con el efecto todavía
  sin verificar.
- `5-no-ejecutada-2-no-verificada.png` — La misma orden marcada como efecto NO
  verificado, con el aviso de recurrir al acompañante presencial.
- `5-no-ejecutada-3-alerta.png` — Alerta crítica "La detención no se ejecutó".

## Cómo capturar

En el emulador, `Ctrl + S` guarda una captura. Desde la línea de comandos:

```bash
adb exec-out screencap -p > docs/evidencias/1-mision-normal-1-inicio.png
```

Para grabar el video de demostración:

```bash
adb shell screenrecord /sdcard/demo.mp4
```

## Qué conviene decir en el video

- En el escenario 3, que el robot **sigue trabajando** aunque la app no lo vea.
- En el escenario 5, que la orden dice confirmada y el robot no se detuvo, y que
  por eso existe el acompañante presencial: el G1 no tiene corte físico.
- Que ningún botón se deshabilita sin decir por qué.
