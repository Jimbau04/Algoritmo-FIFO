package servidor;

import java.util.Map;

/**
 * Interfaz que define los métodos remotos disponibles para los clientes.
 * Esta interfaz representa el "contrato" entre cliente y servidor
 * * IMPORTANTE: Todos los métodos deben usar tipos compatibles con XML-RPC:
 *  * - Tipos primitivos (int, boolean, double, String)
 *  * - Map (se convierte a struct)
 *  * - Object[] (se convierte a array)
 */
public interface IPlanificadorServicio {

    /**
     * Conecta un cliente al servidor
     * @param clienteId Identificador único del cliente
     * @return true si la conexión fue exitosa
     */
    boolean conectarCliente(String clienteId);

    /**
     * Desconecta un cliente del servidor
     * @param clienteId Identificador del cliente
     * @return true si la desconexión fue exitosa
     */
    boolean desconectarCliente(String clienteId);

    /**
     * Agrega un nuevo proceso a la cola FIFO
     * @param clienteId ID del cliente propietario
     * @param nombreProceso Nombre/ID del proceso
     * @param tiempoCPU Tiempo de ejecución requerido
     * @return true si se agregó correctamente
     */
    boolean agregarProceso(String clienteId, String nombreProceso, int tiempoCPU);

    /**
     * Elimina un proceso de la cola (si aún no está en ejecución)
     * @param clienteId ID del cliente propietario
     * @param nombreProceso Nombre del proceso a eliminar
     * @return true si se eliminó correctamente
     */
    boolean eliminarProceso(String clienteId, String nombreProceso);

    /**
     * Obtiene el estado actual de un proceso específico
     * @param clienteId ID del cliente propietario
     * @param nombreProceso Nombre del proceso
     * @return Map con los datos del proceso (estado, tiempos, etc.)
     */
    Map<String, Object> obtenerEstadoProceso(String clienteId, String nombreProceso);

    /**
     * Obtiene todos los procesos de un cliente
     * @param clienteId ID del cliente
     * @return Lista de Maps con los datos de cada proceso
     */
    Object[] obtenerProcesosPorCliente(String clienteId);
}