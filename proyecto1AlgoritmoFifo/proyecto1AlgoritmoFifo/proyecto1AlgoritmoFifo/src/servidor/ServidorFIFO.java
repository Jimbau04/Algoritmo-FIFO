package servidor;

import modelo.ColaProceso;
import modelo.Proceso;
import modelo.EstadoProceso;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

import java.util.*;

public class ServidorFIFO implements IPlanificadorServicio {

    // ============ ATRIBUTOS ============
    private ColaProceso colaProceso;
    private WebServer webServer;
    private int puerto;
    private Map<String, List<String>> clientesProcesos;
    private boolean ejecutando;
    private Thread hiloEjecucion;

    // ============ CONSTRUCTOR ============
    public ServidorFIFO(int puerto) {
        this.puerto = puerto;
        this.colaProceso = new ColaProceso();
        this.clientesProcesos = new HashMap<>();
        this.ejecutando = false;

        System.out.println("✓ Servidor FIFO inicializado en puerto " + puerto);
    }

    // ============ MÉTODOS DE INICIO/PARADA ============

    public void iniciar() throws Exception {
        // 1. Crear el servidor web
        webServer = new WebServer(puerto);

        // 2. Obtener el servidor XML-RPC interno
        XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

        // 3. Configurar el servidor
        XmlRpcServerConfigImpl config = new XmlRpcServerConfigImpl();
        config.setEnabledForExtensions(true);
        config.setContentLengthOptional(false);
        xmlRpcServer.setConfig(config);

        // 4. ⭐ CAMBIO IMPORTANTE: Usar PlanificadorHandler en lugar de ServidorFIFO
        PropertyHandlerMapping phm = new PropertyHandlerMapping();
        phm.addHandler("Planificador", PlanificadorHandler.class); // ⬅️ CAMBIO AQUÍ
        xmlRpcServer.setHandlerMapping(phm);

        // 5. ⭐ NUEVO: Establecer la referencia al servidor en el handler
        PlanificadorHandler.setServidor(this);

        // 6. Iniciar el servidor web
        webServer.start();

        System.out.println("✓ Servidor XML-RPC iniciado en http://localhost:" + puerto);
        System.out.println("✓ Métodos disponibles bajo el namespace 'Planificador'");

        // 7. Iniciar el hilo de ejecución de procesos
        iniciarHiloEjecucion();
    }

    private void iniciarHiloEjecucion() {
        ejecutando = true;

        hiloEjecucion = new Thread(() -> {
            System.out.println("✓ Hilo de ejecución iniciado");

            while (ejecutando) {
                try {
                    // 1. ¿Hay un proceso ejecutándose?
                    if (!colaProceso.hayProcesoEnEjecucion() && !colaProceso.estaVacia()) {
                        // 2. Obtener el siguiente proceso (FIFO)
                        Proceso siguiente = colaProceso.obtenerSiguienteProceso();

                        if (siguiente != null) {
                            System.out.println(">>> Ejecutando: " + siguiente.getNombre());
                        }
                    }

                    // 3. Si hay un proceso en ejecución, completarlo
                    if (colaProceso.hayProcesoEnEjecucion()) {
                        colaProceso.completaProcesoActual();

                        Proceso completado = colaProceso.getTodosLosProcesos()
                                .stream()
                                .filter(p -> p.getEstado() == EstadoProceso.COMPLETADO)
                                .reduce((first, second) -> second)
                                .orElse(null);

                        if (completado != null) {
                            System.out.println(">>> Completado: " + completado.getNombre() +
                                    " | E=" + completado.getTiempoEspera() +
                                    " | F=" + completado.getTiempoFinalizacion() +
                                    " | P=" + String.format("%.2f", completado.getPenalizacion()));
                        }
                    }

                    // 4. Esperar un poco antes de la siguiente iteración
                    Thread.sleep(100);

                } catch (InterruptedException e) {
                    System.out.println("✗ Hilo de ejecución interrumpido");
                    break;
                }
            }

            System.out.println("✓ Hilo de ejecución terminado");
        });

        hiloEjecucion.start();
    }

    public void detener() {
        ejecutando = false;
        if (webServer != null) {
            webServer.shutdown();
        }
        System.out.println("✓ Servidor detenido");
    }

    // ============ MÉTODOS REMOTOS (IPlanificadorServicio) ============

    @Override
    public synchronized boolean conectarCliente(String clienteId) {
        try {
            // Validar que el clienteId no sea nulo o vacío
            if (clienteId == null || clienteId.trim().isEmpty()) {
                System.out.println("✗ Error: ClienteId inválido");
                return false;
            }

            // Verificar si el cliente ya está conectado
            if (clientesProcesos.containsKey(clienteId)) {
                System.out.println("⚠ Cliente ya conectado: " + clienteId);
                return true; // Ya está conectado, retornamos true
            }

            // Registrar el nuevo cliente con una lista vacía de procesos
            clientesProcesos.put(clienteId, new ArrayList<>());

            System.out.println("✓ Cliente conectado exitosamente: " + clienteId);
            System.out.println("  Total de clientes conectados: " + clientesProcesos.size());

            return true;

        } catch (Exception e) {
            System.err.println("✗ Error al conectar cliente: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public synchronized boolean desconectarCliente(String clienteId) {
        try {
            // Verificar si el cliente existe
            if (!clientesProcesos.containsKey(clienteId)) {
                System.out.println("⚠ Cliente no encontrado: " + clienteId);
                return false;
            }

            // Obtener la lista de procesos del cliente
            List<String> procesosCliente = clientesProcesos.get(clienteId);

            // Eliminar todos los procesos del cliente que aún estén en la cola
            if (procesosCliente != null && !procesosCliente.isEmpty()) {
                System.out.println("  Eliminando " + procesosCliente.size() + " procesos del cliente...");

                for (String nombreProceso : new ArrayList<>(procesosCliente)) {
                    colaProceso.eliminarProceso(nombreProceso);
                }
            }

            // Remover el cliente del registro
            clientesProcesos.remove(clienteId);

            System.out.println("✓ Cliente desconectado exitosamente: " + clienteId);
            System.out.println("  Total de clientes conectados: " + clientesProcesos.size());

            return true;

        } catch (Exception e) {
            System.err.println("✗ Error al desconectar cliente: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public synchronized boolean agregarProceso(String clienteId, String nombreProceso, int tiempoCPU) {
        try {
            // 1. Validar parámetros
            if (clienteId == null || clienteId.trim().isEmpty()) {
                System.out.println("✗ Error: ClienteId inválido");
                return false;
            }

            if (nombreProceso == null || nombreProceso.trim().isEmpty()) {
                System.out.println("✗ Error: Nombre de proceso inválido");
                return false;
            }

            if (tiempoCPU <= 0) {
                System.out.println("✗ Error: Tiempo de CPU debe ser mayor a 0");
                return false;
            }

            // 2. Verificar que el cliente esté conectado
            if (!clientesProcesos.containsKey(clienteId)) {
                System.out.println("✗ Error: Cliente no conectado: " + clienteId);
                return false;
            }

            // 3. Verificar que no exista un proceso con el mismo nombre
            for (Proceso p : colaProceso.getTodosLosProcesos()) {
                if (p.getNombre().equals(nombreProceso)) {
                    System.out.println("✗ Error: Ya existe un proceso con el nombre: " + nombreProceso);
                    return false;
                }
            }

            // 4. Crear el nuevo proceso
            Proceso nuevoProceso = new Proceso(nombreProceso, tiempoCPU, 0);
            nuevoProceso.setClienteId(clienteId);

            // 5. Agregar el proceso a la cola
            colaProceso.agregarProceso(nuevoProceso);

            // 6. Registrar el proceso en la lista del cliente
            clientesProcesos.get(clienteId).add(nombreProceso);

            System.out.println("✓ Proceso agregado exitosamente:");
            System.out.println("  - Nombre: " + nombreProceso);
            System.out.println("  - Cliente: " + clienteId);
            System.out.println("  - Tiempo CPU: " + tiempoCPU);
            System.out.println("  - Posición en cola: " + colaProceso.tamanioCola());

            return true;

        } catch (Exception e) {
            System.err.println("✗ Error al agregar proceso: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public synchronized boolean eliminarProceso(String clienteId, String nombreProceso) {
        try {
            // 1. Validar parámetros
            if (clienteId == null || nombreProceso == null) {
                System.out.println("✗ Error: Parámetros inválidos");
                return false;
            }

            // 2. Verificar que el cliente esté conectado
            if (!clientesProcesos.containsKey(clienteId)) {
                System.out.println("✗ Error: Cliente no conectado: " + clienteId);
                return false;
            }

            // 3. Verificar que el proceso pertenezca al cliente
            List<String> procesosCliente = clientesProcesos.get(clienteId);
            if (!procesosCliente.contains(nombreProceso)) {
                System.out.println("✗ Error: El proceso no pertenece al cliente");
                return false;
            }

            // 4. Verificar que el proceso no esté en ejecución
            Proceso procesoEnEjecucion = colaProceso.getProcesoEnEjecucion();
            if (procesoEnEjecucion != null && procesoEnEjecucion.getNombre().equals(nombreProceso)) {
                System.out.println("✗ Error: No se puede eliminar un proceso en ejecución");
                return false;
            }

            // 5. Eliminar el proceso de la cola
            boolean eliminado = colaProceso.eliminarProceso(nombreProceso);

            if (eliminado) {
                // 6. Remover de la lista del cliente
                procesosCliente.remove(nombreProceso);

                System.out.println("✓ Proceso eliminado exitosamente:");
                System.out.println("  - Nombre: " + nombreProceso);
                System.out.println("  - Cliente: " + clienteId);

                return true;
            } else {
                System.out.println("⚠ El proceso ya no está en la cola (posiblemente ya completado)");
                // Aún así lo removemos de la lista del cliente
                procesosCliente.remove(nombreProceso);
                return false;
            }

        } catch (Exception e) {
            System.err.println("✗ Error al eliminar proceso: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public synchronized Map<String, Object> obtenerEstadoProceso(String clienteId, String nombreProceso) {
        try {
            // 1. Validar parámetros
            if (clienteId == null || nombreProceso == null) {
                System.out.println("✗ Error: Parámetros inválidos");
                return new HashMap<>();
            }

            // 2. Verificar que el cliente esté conectado
            if (!clientesProcesos.containsKey(clienteId)) {
                System.out.println("✗ Error: Cliente no conectado");
                return new HashMap<>();
            }

            // 3. Buscar el proceso en todos los procesos
            for (Proceso proceso : colaProceso.getTodosLosProcesos()) {
                if (proceso.getNombre().equals(nombreProceso) &&
                        proceso.getClienteId().equals(clienteId)) {

                    // 4. Convertir el proceso a Map usando la utilidad
                    return util.ConvertidorProceso.procesoAMap(proceso);
                }
            }

            System.out.println("⚠ Proceso no encontrado: " + nombreProceso);
            return new HashMap<>();

        } catch (Exception e) {
            System.err.println("✗ Error al obtener estado del proceso: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    @Override
    public synchronized Object[] obtenerProcesosPorCliente(String clienteId) {
        try {
            // 1. Validar parámetro
            if (clienteId == null) {
                System.out.println("✗ Error: ClienteId inválido");
                return new Object[0];
            }

            // 2. Verificar que el cliente esté conectado
            if (!clientesProcesos.containsKey(clienteId)) {
                System.out.println("✗ Error: Cliente no conectado");
                return new Object[0];
            }

            // 3. Obtener todos los procesos del cliente
            List<Map<String, Object>> procesosMap = new ArrayList<>();

            for (Proceso proceso : colaProceso.getTodosLosProcesos()) {
                if (proceso.getClienteId().equals(clienteId)) {
                    procesosMap.add(util.ConvertidorProceso.procesoAMap(proceso));
                }
            }

            System.out.println("✓ Obteniendo " + procesosMap.size() + " procesos del cliente: " + clienteId);

            // 4. Convertir a array de Objects
            return procesosMap.toArray();

        } catch (Exception e) {
            System.err.println("✗ Error al obtener procesos del cliente: " + e.getMessage());
            e.printStackTrace();
            return new Object[0];
        }
    }
}