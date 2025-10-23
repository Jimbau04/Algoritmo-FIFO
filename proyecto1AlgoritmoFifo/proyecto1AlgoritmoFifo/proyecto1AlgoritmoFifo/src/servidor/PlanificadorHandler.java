package servidor;

import modelo.Proceso;
import modelo.ColaProceso;
import java.util.*;

/**
 * ARCHIVO: servidor/PlanificadorHandler.java
 *
 * Handler que procesa las peticiones XML-RPC.
 * Esta clase es instanciada por XML-RPC y delega las operaciones
 * al ServidorFIFO singleton.
 *
 * IMPORTANTE: Esta clase DEBE tener un constructor sin parámetros
 */
public class PlanificadorHandler implements IPlanificadorServicio {

    // Referencia al servidor singleton
    private static ServidorFIFO servidorFIFO;

    /**
     * Constructor sin parámetros (requerido por XML-RPC)
     */
    public PlanificadorHandler() {
        // Constructor vacío - XML-RPC lo necesita
    }

    /**
     * Método estático para establecer la referencia al servidor
     * Este método será llamado por ServidorFIFO al iniciar
     */
    public static void setServidor(ServidorFIFO servidor) {
        servidorFIFO = servidor;
    }

    // ============ DELEGACIÓN DE MÉTODOS ============
    // Todos los métodos delegan al servidor singleton

    @Override
    public boolean conectarCliente(String clienteId) {
        if (servidorFIFO == null) {
            System.err.println("✗ Error: ServidorFIFO no inicializado");
            return false;
        }
        return servidorFIFO.conectarCliente(clienteId);
    }

    @Override
    public boolean desconectarCliente(String clienteId) {
        if (servidorFIFO == null) {
            System.err.println("✗ Error: ServidorFIFO no inicializado");
            return false;
        }
        return servidorFIFO.desconectarCliente(clienteId);
    }

    @Override
    public boolean agregarProceso(String clienteId, String nombreProceso, int tiempoCPU) {
        if (servidorFIFO == null) {
            System.err.println("✗ Error: ServidorFIFO no inicializado");
            return false;
        }
        return servidorFIFO.agregarProceso(clienteId, nombreProceso, tiempoCPU);
    }

    @Override
    public boolean eliminarProceso(String clienteId, String nombreProceso) {
        if (servidorFIFO == null) {
            System.err.println("✗ Error: ServidorFIFO no inicializado");
            return false;
        }
        return servidorFIFO.eliminarProceso(clienteId, nombreProceso);
    }

    @Override
    public Map<String, Object> obtenerEstadoProceso(String clienteId, String nombreProceso) {
        if (servidorFIFO == null) {
            System.err.println("✗ Error: ServidorFIFO no inicializado");
            return new HashMap<>();
        }
        return servidorFIFO.obtenerEstadoProceso(clienteId, nombreProceso);
    }

    @Override
    public Object[] obtenerProcesosPorCliente(String clienteId) {
        if (servidorFIFO == null) {
            System.err.println("✗ Error: ServidorFIFO no inicializado");
            return new Object[0];
        }
        return servidorFIFO.obtenerProcesosPorCliente(clienteId);
    }
}