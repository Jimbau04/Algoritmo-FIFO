package modelo;
/* ================= CLASE QUE AYUDA CON LOS 5 POSIBLES ESTADOS DE UN PROCESO =================
    Enumeración que representa los estados posibles de un proceso en el planificador FIFO
    enum es la abreviatura de enumeration y es un tipo especial de clase que representa un conjunto fijo de constantes,
    en este caso los tipos de estado de un proceso.

    Lo utilizamos para eviatr errores de escritura, facilita la lectura y mantenimiento.
 */
public enum EstadoProceso {
    NUEVO,
    LISTO,
    EJECUTANDO,
    COMPLETADO,
    ELIMINADO,
}
