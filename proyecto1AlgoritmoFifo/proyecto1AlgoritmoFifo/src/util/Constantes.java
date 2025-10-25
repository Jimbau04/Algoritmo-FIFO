package util;

public class Constantes {
    public static final int PUERTO_SERVIDOR = 8080;
    public static final int RANGO_PLANIFICADOR = 40; // Capacidad del planificador (40 ticks)

    // Configuración de simulación
    public static final int SIMULACION_TICK_MS = 5000; // 1 tick = 5 segundo (cambiar a 5000 para producción)
    public static final int TIMEOUT_INACTIVIDAD_CLIENTE_S = 30; // 30 segundos

    // Namespace del Servidor
    public static final String NAMESPACE = "Planificador";

    // Métodos del Servidor
    public static final String METODO_CONECTAR = "Planificador.conectarCliente";
    public static final String METODO_DESCONECTAR = "Planificador.desconectarCliente";
    public static final String METODO_AGREGAR_PROCESO = "Planificador.agregarProceso";
    public static final String METODO_ELIMINAR_PROCESO = "Planificador.eliminarProceso";
    public static final String METODO_OBTENER_PROCESOS = "Planificador.obtenerProcesosPorCliente";
}