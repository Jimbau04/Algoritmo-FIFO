package modelo;

public enum EstadoProceso {
    NUEVO,         // Creado por el cliente, aún no enviado
    EN_COLA,         // Llegó al servidor, en cola de espera
    EJECUCION,     // En la CPU del servidor
    TERMINADO,     // Completado exitosamente
    ELIMINADO,     // Matado por el cliente o el servidor
    RECHAZADO,
    EN_FILA_ESPERA// El servidor no lo aceptó (ej. fuera de rango)
}