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

    public boolean conectar() {
        try {
            System.out.println("═══════════════════════════════════════");
            System.out.println("  CONECTANDO AL SERVIDOR");
            System.out.println("═══════════════════════════════════════");
            System.out.println("  URL: " + servidorURL);
            System.out.println("  Cliente ID: " + clienteId);

            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(servidorURL));
            config.setEnabledForExtensions(true);
            config.setConnectionTimeout(5000);
            config.setReplyTimeout(5000);

            cliente = new XmlRpcClient();
            cliente.setConfig(config);

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

    public boolean desconectar() {
        try {
            if (!conectado) {
                System.out.println("⚠ No hay conexión activa");
                return false;
            }

            System.out.println("Desconectando del servidor...");
            Object[] params = new Object[]{clienteId};
            Boolean resultado = (Boolean) cliente.execute(Constantes.METODO_DESCONECTAR, params);

            conectado = false;
            cliente = null;

            if (resultado != null && resultado) {
                System.out.println("✓ Desconectado exitosamente");
                return true;
            } else {
                System.out.println("⚠ Error al desconectar (servidor no respondió)");
                return false;
            }

        } catch (Exception e) {
            System.err.println("✗ Error al desconectar: " + e.getMessage());
            conectado = false;
            cliente = null;
            return false;
        }
    }

    public void desconectarLocalmente() {
        this.conectado = false;
        this.cliente = null;
        System.out.println("! Conexión perdida o cerrada por el servidor.");
    }

    public boolean agregarProceso(String nombreProceso, int tiempoCPU, int tiempoPeticion) {
        if (!conectado) {
            System.out.println("✗ Error: No hay conexión con el servidor");
            return false;
        }

        try {
            System.out.println("Agregando proceso '" + nombreProceso + "' con tiempo CPU: " + tiempoCPU +" y tiempo de peticion: "+tiempoPeticion);

            Object[] params = new Object[]{clienteId, nombreProceso, tiempoCPU, tiempoPeticion};
            Boolean resultado = (Boolean) cliente.execute(Constantes.METODO_AGREGAR_PROCESO, params);

            if (resultado != null && resultado) {
                System.out.println("✓ Proceso agregado exitosamente");
                return true;
            } else {
                System.out.println("✗ Error al agregar proceso (rechazado por el servidor)");
                return false;
            }

        } catch (Exception e) {
            System.err.println("✗ Error al agregar proceso: " + e.getMessage());
            desconectarLocalmente();
            return false;
        }
    }

    public boolean eliminarProceso(String nombreProceso) {
        if (!conectado) {
            System.out.println("✗ Error: No hay conexión con el servidor");
            return false;
        }

        try {
            System.out.println("Eliminando proceso: " + nombreProceso);
            Object[] params = new Object[]{clienteId, nombreProceso};
            Boolean resultado = (Boolean) cliente.execute(Constantes.METODO_ELIMINAR_PROCESO, params);

            if (resultado != null && resultado) {
                System.out.println("✓ Solicitud de eliminación enviada");
                return true;
            } else {
                System.out.println("✗ Error al eliminar proceso (servidor lo rechazó)");
                return false;
            }

        } catch (Exception e) {
            System.err.println("✗ Error al eliminar proceso: " + e.getMessage());
            desconectarLocalmente();
            return false;
        }
    }

    public Object[] obtenerTodosLosProcesos() {
        if (!conectado) {
            System.out.println("✗ Error: No hay conexión con el servidor");
            return null;
        }

        try {
            Object[] params = new Object[]{clienteId};
            Object resultado = cliente.execute(Constantes.METODO_OBTENER_PROCESOS, params);

            if (resultado instanceof Object[]) {
                return (Object[]) resultado;
            } else {
                return new Object[0];
            }

        } catch (Exception e) {
            System.err.println("✗ Error al obtener procesos (sondeo): " + e.getMessage());
            desconectarLocalmente();
            return null;
        }
    }

    public boolean isConectado() { return conectado; }
    public String getClienteId() { return clienteId; }
}