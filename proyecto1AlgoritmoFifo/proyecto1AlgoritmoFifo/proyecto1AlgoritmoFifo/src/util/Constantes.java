package util;

/**
 * ARCHIVO: util/Constantes.java
 *
 * Constantes utilizadas en todo el sistema
 */
public class Constantes {

    // ========== CONFIGURACIÓN DE RED ==========
    public static final String SERVIDOR_HOST = "localhost";
    public static final int SERVIDOR_PUERTO = 8080;
    public static final String SERVIDOR_URL = "http://" + SERVIDOR_HOST + ":" + SERVIDOR_PUERTO + "/xmlrpc";

    // ========== NAMESPACE XML-RPC ==========
    public static final String NAMESPACE_PLANIFICADOR = "Planificador";

    // ========== NOMBRES DE MÉTODOS REMOTOS ==========
    public static final String METODO_CONECTAR = NAMESPACE_PLANIFICADOR + ".conectarCliente";
    public static final String METODO_DESCONECTAR = NAMESPACE_PLANIFICADOR + ".desconectarCliente";
    public static final String METODO_AGREGAR_PROCESO = NAMESPACE_PLANIFICADOR + ".agregarProceso";
    public static final String METODO_ELIMINAR_PROCESO = NAMESPACE_PLANIFICADOR + ".eliminarProceso";
    public static final String METODO_OBTENER_ESTADO = NAMESPACE_PLANIFICADOR + ".obtenerEstadoProceso";
    public static final String METODO_OBTENER_PROCESOS = NAMESPACE_PLANIFICADOR + ".obtenerProcesosPorCliente";

    // ========== CLAVES PARA MAPS (XML-RPC) ==========
    public static final String KEY_NOMBRE = "nombre";
    public static final String KEY_ESTADO = "estado";
    public static final String KEY_TIEMPO_CPU = "tiempoCPU";
    public static final String KEY_TIEMPO_ESPERA = "tiempoEspera";
    public static final String KEY_TIEMPO_FINALIZACION = "tiempoFinalizacion";
    public static final String KEY_PENALIZACION = "penalizacion";
    public static final String KEY_CLIENTE_ID = "clienteId";

    // ========== VALORES POR DEFECTO ==========
    public static final int TIMEOUT_CONEXION = 5000; // 5 segundos
    public static final int INTERVALO_ACTUALIZACION = 1000; // 1 segundo

    // Constructor privado para evitar instanciación
    private Constantes() {}
}