package servidor;

public class PlanificadorHandler {

    private static ServidorFIFO servidorPrincipal;

    public static void setServidor(ServidorFIFO servidor) {
        servidorPrincipal = servidor;
    }

    // --- Métodos Públicos (Redireccionan a la instancia principal) ---

    public boolean conectarCliente(String clienteId) {
        if (servidorPrincipal == null) return false;
        return servidorPrincipal.conectarCliente(clienteId);
    }

    public boolean desconectarCliente(String clienteId) {
        if (servidorPrincipal == null) return false;
        return servidorPrincipal.desconectarCliente(clienteId);
    }

    public boolean agregarProceso(String clienteId, String nombreProceso, int tiempoCPU, int tiempoPeticion) {
        if (servidorPrincipal == null) return false;
        return servidorPrincipal.agregarProceso(clienteId, nombreProceso, tiempoCPU,tiempoPeticion);
    }

    public boolean eliminarProceso(String clienteId, String nombreProceso) {
        if (servidorPrincipal == null) return false;
        return servidorPrincipal.eliminarProceso(clienteId, nombreProceso);
    }

    public Object[] obtenerProcesosPorCliente(String clienteId) {
        if (servidorPrincipal == null) return new Object[0];
        return servidorPrincipal.obtenerProcesosPorCliente(clienteId);
    }
}
