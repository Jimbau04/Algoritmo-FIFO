package cliente;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import util.Constantes;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ClienteFIFO {

    private XmlRpcClient cliente;
    private String clienteId;
    private String servidorURL;
    private boolean conectado;

    public ClienteFIFO(String servidorURL, String clienteId) {
        this.servidorURL = servidorURL;
        this.clienteId = clienteId;
        this.conectado = false;
    }

    /**
     * Conecta con el servidor XML-RPC
     */
    public boolean conectar() {
        try {
            System.out.println("═══════════════════════════════════════");
            System.out.println("  CONECTANDO AL SERVIDOR");
            System.out.println("═══════════════════════════════════════");
            System.out.println("  URL: " + servidorURL);
            System.out.println("  Cliente ID: " + clienteId);

            // 1. Crear la configuración del cliente XML-RPC
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(servidorURL));
            config.setEnabledForExtensions(true);
            config.setConnectionTimeout(Constantes.TIMEOUT_CONEXION);
            config.setReplyTimeout(Constantes.TIMEOUT_CONEXION);

            // 2. Crear el cliente XML-RPC
            cliente = new XmlRpcClient();
            cliente.setConfig(config);

            // 3. Intentar conectar llamando al método remoto
            Object[] params = new Object[]{clienteId};
            Boolean resultado = (Boolean) cliente.execute(Constantes.METODO_CONECTAR, params);

            if (resultado != null && resultado) {
                conectado = true;
                System.out.println("✓ Conexión exitosa");
                System.out.println("═══════════════════════════════════════\n");
                return true;
            } else {
                System.out.println("✗ El servidor rechazó la conexión");
                System.out.println("═══════════════════════════════════════\n");
                return false;
            }

        } catch (Exception e) {
            System.err.println("✗ Error al conectar con el servidor:");
            System.err.println("  " + e.getMessage());
            System.out.println("═══════════════════════════════════════\n");
            conectado = false;
            return false;
        }
    }

    /**
     * Desconecta del servidor
     */
    public boolean desconectar() {
        try {
            if (!conectado) {
                System.out.println("⚠ No hay conexión activa");
                return false;
            }

            System.out.println("Desconectando del servidor...");

            // Llamar al método remoto de desconexión
            Object[] params = new Object[]{clienteId};
            Boolean resultado = (Boolean) cliente.execute(Constantes.METODO_DESCONECTAR, params);

            if (resultado != null && resultado) {
                conectado = false;
                cliente = null;
                System.out.println("✓ Desconectado exitosamente");
                return true;
            } else {
                System.out.println("⚠ Error al desconectar");
                return false;
            }

        } catch (Exception e) {
            System.err.println("✗ Error al desconectar: " + e.getMessage());
            conectado = false;
            cliente = null;
            return false;
        }
    }

    /**
     * Agrega un nuevo proceso
     */
    public boolean agregarProceso(String nombreProceso, int tiempoCPU) {
        try {
            if (!conectado) {
                System.out.println("✗ Error: No hay conexión con el servidor");
                return false;
            }

            System.out.println("Agregando proceso '" + nombreProceso + "' con tiempo CPU: " + tiempoCPU);

            // Llamar al método remoto
            Object[] params = new Object[]{clienteId, nombreProceso, tiempoCPU};
            Boolean resultado = (Boolean) cliente.execute(Constantes.METODO_AGREGAR_PROCESO, params);

            if (resultado != null && resultado) {
                System.out.println("✓ Proceso agregado exitosamente");
                return true;
            } else {
                System.out.println("✗ Error al agregar proceso");
                return false;
            }

        } catch (Exception e) {
            System.err.println("✗ Error al agregar proceso: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina un proceso
     */
    public boolean eliminarProceso(String nombreProceso) {
        try {
            if (!conectado) {
                System.out.println("✗ Error: No hay conexión con el servidor");
                return false;
            }

            System.out.println("Eliminando proceso: " + nombreProceso);

            // Llamar al método remoto
            Object[] params = new Object[]{clienteId, nombreProceso};
            Boolean resultado = (Boolean) cliente.execute(Constantes.METODO_ELIMINAR_PROCESO, params);

            if (resultado != null && resultado) {
                System.out.println("✓ Proceso eliminado exitosamente");
                return true;
            } else {
                System.out.println("✗ Error al eliminar proceso (puede estar en ejecución)");
                return false;
            }

        } catch (Exception e) {
            System.err.println("✗ Error al eliminar proceso: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el estado de un proceso específico
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerEstadoProceso(String nombreProceso) {
        try {
            if (!conectado) {
                System.out.println("✗ Error: No hay conexión con el servidor");
                return new HashMap<>();
            }

            // Llamar al método remoto
            Object[] params = new Object[]{clienteId, nombreProceso};
            Object resultado = cliente.execute(Constantes.METODO_OBTENER_ESTADO, params);

            if (resultado instanceof Map) {
                return (Map<String, Object>) resultado;
            } else {
                System.out.println("⚠ Proceso no encontrado");
                return new HashMap<>();
            }

        } catch (Exception e) {
            System.err.println("✗ Error al obtener estado: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Obtiene todos los procesos del cliente
     */
    public Object[] obtenerTodosLosProcesos() {
        try {
            if (!conectado) {
                System.out.println("✗ Error: No hay conexión con el servidor");
                return new Object[0];
            }

            // Llamar al método remoto
            Object[] params = new Object[]{clienteId};
            Object resultado = cliente.execute(Constantes.METODO_OBTENER_PROCESOS, params);

            if (resultado instanceof Object[]) {
                return (Object[]) resultado;
            } else {
                return new Object[0];
            }

        } catch (Exception e) {
            System.err.println("✗ Error al obtener procesos: " + e.getMessage());
            return new Object[0];
        }
    }

    // Getters
    public boolean isConectado() {
        return conectado;
    }

    public String getClienteId() {
        return clienteId;
    }
}