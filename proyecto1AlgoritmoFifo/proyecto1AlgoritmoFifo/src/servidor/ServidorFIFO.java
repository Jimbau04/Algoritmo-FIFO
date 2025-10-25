package servidor;

import modelo.ColaProceso;
import modelo.Proceso;
import modelo.EstadoProceso;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;
import servidor.interfaz.ColaVentana;
import servidor.interfaz.procesos;
import servidor.interfaz.tirmpos;
import util.Constantes;
import util.ConvertidorProceso;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorFIFO {

    // --- Atributos de Red y Lógica ---
    private WebServer webServer;
    private int puerto;
    private final int rangoPlanificador;
    private boolean ejecutando;
    private Thread hiloEjecucion;
    private int tiempoSimulacionGlobal = 1; // El reloj empieza en el tick 1

    // --- Gestión de Estado ---
    private ColaProceso colaProceso;
    private Map<String, List<String>> clientesProcesos;
    private Map<String, Timer> timersInactividad;

    // --- Referencias a las GUIs del Servidor ---
    private procesos guiProcesos;
    private tirmpos guiTiempos;
    private ColaVentana guiColas;

    public ServidorFIFO(int puerto, int rangoPlanificador) {
        this.puerto = puerto;
        this.rangoPlanificador = rangoPlanificador;
        this.colaProceso = new ColaProceso();
        this.clientesProcesos = new ConcurrentHashMap<>();
        this.timersInactividad = new ConcurrentHashMap<>();
        this.ejecutando = false;
        System.out.println("✓ Servidor FIFO inicializado en puerto " + puerto + " con rango " + rangoPlanificador);
    }

    public void setInterfaces(procesos guiProcesos, tirmpos guiTiempos, ColaVentana guiColas) {
        this.guiProcesos = guiProcesos;
        this.guiTiempos = guiTiempos;
        this.guiColas = guiColas;
        System.out.println("✓ GUIs del servidor conectadas.");
    }

    // ============ MÉTODOS DE INICIO/PARADA ============

    public void iniciar() throws Exception {
        webServer = new WebServer(puerto);
        XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

        PropertyHandlerMapping phm = new PropertyHandlerMapping();
        phm.addHandler(Constantes.NAMESPACE, PlanificadorHandler.class);
        xmlRpcServer.setHandlerMapping(phm);

        PlanificadorHandler.setServidor(this);

        XmlRpcServerConfigImpl config = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
        config.setEnabledForExtensions(true);
        config.setContentLengthOptional(false);

        webServer.start();
        System.out.println("✓ Servidor XML-RPC iniciado en http://localhost:" + puerto);

        iniciarHiloEjecucion();
    }

    public void detener() {
        ejecutando = false;
        if (hiloEjecucion != null) {
            hiloEjecucion.interrupt();
        }
        if (webServer != null) {
            webServer.shutdown();
        }
        for (String clienteId : new ArrayList<>(clientesProcesos.keySet())) {
            desconectarCliente(clienteId);
        }
        System.out.println("✓ Servidor detenido");
    }

    // ============ HILO DE EJECUCIÓN (PLANIFICADOR) ============

    private void iniciarHiloEjecucion() {
        ejecutando = true;
        hiloEjecucion = new Thread(() -> {
            System.out.println("✓ Hilo de ejecución (Planificador) iniciado.");
            while (ejecutando) {
                try {

                    Proceso pEnEjecucion = colaProceso.getProcesoEnEjecucion();

                    if (pEnEjecucion != null) {
                        // 1. EJECUTAR UN TICK
                        pEnEjecucion.incrementarTiempoEjecutado();
                        notificarGUIs_TickEjecucion(pEnEjecucion, tiempoSimulacionGlobal);

                        // 2. COMPPROBAR SI TERMINÓ
                        if (pEnEjecucion.getTiempoEjecutado() >= pEnEjecucion.getTiempoCPU()) {
                            colaProceso.marcarProcesoTerminado(tiempoSimulacionGlobal);
                            notificarGUIs_Terminado(pEnEjecucion);
                        }
                    }
                    else if (!colaProceso.estaVacia()) {
                        // 3. INICIAR NUEVO PROCESO
                        Proceso pNuevo = colaProceso.obtenerSiguienteProceso();
                        if (pNuevo == null) continue;

                        pNuevo.setTiempoInicio(tiempoSimulacionGlobal);
                        pNuevo.incrementarTiempoEjecutado();

                        notificarGUIs_Ejecucion(pNuevo);
                        notificarGUIs_TickEjecucion(pNuevo, tiempoSimulacionGlobal); // Ejecutar primer tick
                    }
                    else if (colaProceso.hayProcesosEnEspera()) {
                        // 4. CHEQUEAR COLA DE ESPERA (solo si CPU está libre y cola de listos vacía)
                        chequearColaEspera();
                    }

                    // 5. AVANZAR EL RELOJ
                    Thread.sleep(Constantes.SIMULACION_TICK_MS);
                    tiempoSimulacionGlobal++;

                } catch (InterruptedException e) {
                    System.out.println("  Hilo de ejecución interrumpido.");
                    ejecutando = false;
                } catch (Exception e) {
                    System.err.println("✗ Error crítico en el hilo del planificador: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            System.out.println("✓ Hilo de ejecución (Planificador) detenido.");
        });
        hiloEjecucion.start();
    }

    private synchronized void chequearColaEspera() {
        Proceso p = colaProceso.obtenerSiguienteDe_Espera();
        if (p == null) return;

        System.out.println("  Reintentando proceso en espera: " + p.getNombre());

        int finPlanActual = colaProceso.getTiempoFinPlanificado();
        int tiempoLlegada = Math.max(1, finPlanActual + 1); // Su nueva llegada planificada
        int tiempoFinEstimado = tiempoLlegada + p.getTiempoCPU() - 1;

        // Actualizar el proceso con sus nuevos tiempos planificados
        p.setTiempoLlegada(tiempoLlegada);
        p.setTiempoFin(tiempoFinEstimado);

        if (tiempoFinEstimado <= this.rangoPlanificador) {
            System.out.println("  ✓ Proceso " + p.getNombre() + " aceptado en reintento.");
            colaProceso.agregarProceso(p); // Mover a cola de Listos
            notificarGUIs_MovidoA_Listo(p);
        } else {
            colaProceso.incrementarIntentos(p.getNombre());

            if (colaProceso.getIntentos(p.getNombre()) >= 2) {
                System.out.println("  ✗ Proceso " + p.getNombre() + " falló reintento. MATANDO.");
                colaProceso.matarProceso(p, EstadoProceso.RECHAZADO);
                notificarGUIs_Eliminado(p, EstadoProceso.RECHAZADO);
            } else {
                System.out.println("  ! Proceso " + p.getNombre() + " sigue sin caber. Re-encolando.");
                colaProceso.agregarA_EsperaCapacidad(p); // Ponerlo al final
            }
        }
    }

    // ============ MÉTODOS REMOTOS (LLAMADOS POR EL CLIENTE) ============

    public boolean conectarCliente(String clienteId) {
        if (clienteId == null || clienteId.trim().isEmpty()) return false;
        clientesProcesos.putIfAbsent(clienteId, new ArrayList<>());
        System.out.println("✓ Cliente conectado: " + clienteId);
        iniciarTimerInactividad(clienteId);
        return true;
    }

    public boolean desconectarCliente(String clienteId) {
        if (clienteId == null || !clientesProcesos.containsKey(clienteId)) return false;

        Timer timer = timersInactividad.remove(clienteId);
        if (timer != null) timer.cancel();

        List<String> procesosCliente = clientesProcesos.get(clienteId);
        if (procesosCliente != null) {
            for (String nombreProceso : new ArrayList<>(procesosCliente)) {
                Proceso p = colaProceso.getProceso(nombreProceso);
                if (p != null && (p.getEstado() == EstadoProceso.LISTO || p.getEstado() == EstadoProceso.RECHAZADO)) {
                    colaProceso.eliminarProceso(nombreProceso);
                    notificarGUIs_Eliminado(p, EstadoProceso.ELIMINADO);
                }
            }
        }

        clientesProcesos.remove(clienteId);
        System.out.println("✓ Cliente desconectado: " + clienteId);
        return true;
    }

    public synchronized boolean agregarProceso(String clienteId, String nombreProceso, int tiempoCPU, int tiempoPeticion) {
        if (!clientesProcesos.containsKey(clienteId)) return false;
        if (colaProceso.getProceso(nombreProceso) != null) return false;

        resetTimerInactividad(clienteId);

        int finPlanActual = colaProceso.getTiempoFinPlanificado();
        int tiempoLlegada = Math.max(1, finPlanActual + 1);
        int tiempoFinEstimado = tiempoLlegada + tiempoCPU - 1;


        Proceso p = new Proceso(clienteId, nombreProceso, tiempoCPU, tiempoLlegada, tiempoPeticion);
        p.setTiempoFin(tiempoFinEstimado);

        if (tiempoFinEstimado > this.rangoPlanificador) {
            System.out.println("✗ Proceso " + nombreProceso + " no cabe (Plan Lleno: " + tiempoFinEstimado + " > " + this.rangoPlanificador + "). Puesto en cola de espera.");
            colaProceso.agregarA_EsperaCapacidad(p);
            notificarGUIs_NuevoEnEspera(p);
        } else {
            colaProceso.agregarProceso(p);
            System.out.println("✓ Proceso encolado (Listo): " + nombreProceso + " (Plan: " + tiempoLlegada + " a " + tiempoFinEstimado + ")");
            notificarGUIs_Nuevo(p);
        }

        clientesProcesos.get(clienteId).add(nombreProceso);
        return true;
    }

    public synchronized boolean eliminarProceso(String clienteId, String nombreProceso) {
        if (!clientesProcesos.containsKey(clienteId)) return false;
        Proceso p = colaProceso.getProceso(nombreProceso);
        if (p == null) return false;
        if (!p.getClienteId().equals(clienteId)) return false;

        resetTimerInactividad(clienteId);

        if (colaProceso.eliminarProceso(nombreProceso)) {
            System.out.println("✓ Cliente eliminó proceso: " + nombreProceso);
            notificarGUIs_Eliminado(p, EstadoProceso.ELIMINADO);
            return true;
        } else {
            System.out.println("✗ Cliente intentó eliminar proceso en ejecución: " + nombreProceso);
            return false;
        }
    }

    public Object[] obtenerProcesosPorCliente(String clienteId) {
        if (!clientesProcesos.containsKey(clienteId)) return new Object[0];
        resetTimerInactividad(clienteId);

        List<Map<String, Object>> listaMapas = new ArrayList<>();
        List<String> nombresProcesos = clientesProcesos.get(clienteId);
        if (nombresProcesos == null) return new Object[0];

        for (String nombreProceso : nombresProcesos) {
            Proceso p = colaProceso.getProceso(nombreProceso);
            if (p != null) {
                listaMapas.add(ConvertidorProceso.procesoAMap(p));
            }
        }
        return listaMapas.toArray();
    }

    // ============ LÓGICA DE TIMEOUT DE INACTIVIDAD ============

    private void iniciarTimerInactividad(String clienteId) {
        Timer timer = new Timer(true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (colaProceso.getProcesosActivosCliente(clienteId).isEmpty()) {
                    System.out.println("! Timeout de 15s para cliente: " + clienteId + ". Desconectando.");
                    desconectarCliente(clienteId);
                } else {
                    System.out.println("  Timer de inactividad para " + clienteId + " chequeado, pero sigue con procesos activos. Reiniciando timer.");
                    resetTimerInactividad(clienteId);
                }
            }
        };
        timer.schedule(task, Constantes.TIMEOUT_INACTIVIDAD_CLIENTE_S * 1000L);
        timersInactividad.put(clienteId, timer);
    }

    private void resetTimerInactividad(String clienteId) {
        Timer timerAnterior = timersInactividad.remove(clienteId);
        if (timerAnterior != null) {
            timerAnterior.cancel();
        }
        iniciarTimerInactividad(clienteId);
    }

    // ============ MÉTODOS DE NOTIFICACIÓN A GUIs ============

    private void notificarGUIs_Nuevo(Proceso p) {
        if (guiColas != null) {
            guiColas.agregarNuevo(p.getNombre());
            guiColas.moverA_Listo(p.getNombre());
        }
        if (guiProcesos != null) {
            guiProcesos.registrarTareaCompleta(p.getNombre(), p.getTiempoLlegada(), -1, p.getTiempoFin());
        }
        if (guiTiempos != null) {
            guiTiempos.agregarProceso(p.getNombre());
        }
    }

    private void notificarGUIs_NuevoEnEspera(Proceso p) {
        if (guiColas != null) {
            guiColas.agregarNuevo(p.getNombre() + " (En Espera)");
        }
        if (guiProcesos != null) {
            guiProcesos.registrarTareaCompleta(p.getNombre(), p.getTiempoLlegada(), -1, p.getTiempoFin());
        }
        if (guiTiempos != null) {
            guiTiempos.agregarProceso(p.getNombre());
            guiTiempos.actualizarTiempoEspera(p.getNombre(), "ESPERA");
        }
    }

    private void notificarGUIs_MovidoA_Listo(Proceso p) {
        if (guiColas != null) {
            guiColas.moverA_Listo(p.getNombre() + " (En Espera)");
        }
        if (guiTiempos != null) {
            guiTiempos.actualizarTiempoEspera(p.getNombre(), 0);
        }
    }

    private void notificarGUIs_Ejecucion(Proceso p) {
        if (guiColas != null) guiColas.moverA_Ejecucion(p.getNombre());
        if (guiProcesos != null) {
            guiProcesos.registrarTareaCompleta(
                    p.getNombre(), p.getTiempoLlegada(), p.getTiempoInicio(), p.getTiempoFin());
        }
        if (guiTiempos != null) {
            guiTiempos.actualizarTiempoEspera(p.getNombre(), p.getTiempoEspera());
            guiTiempos.calcularYActualizarMedias();
        }
    }

    private void notificarGUIs_TickEjecucion(Proceso p, int tickActual) {
        if (guiProcesos != null) {
            guiProcesos.pintarTickEjecucion(p.getNombre(), tickActual);
        }
    }

    private void notificarGUIs_Terminado(Proceso p) {
        if (guiColas != null) guiColas.moverA_Terminado(p.getNombre());
        if (guiTiempos != null) {
            guiTiempos.actualizarTiempoFinalizacion(p.getNombre(), p.getTiempoFinalizacion());
            guiTiempos.actualizarPenalizacion(p.getNombre(), String.format("%.2f", p.getPenalizacion()));
            guiTiempos.calcularYActualizarMedias();
        }
    }

    private void notificarGUIs_Eliminado(Proceso p, EstadoProceso estado) {
        if (guiColas != null) {
            guiColas.moverA_Terminado(p.getNombre() + " (" + estado.name() + ")");
        }
        if (guiTiempos != null) {
            guiTiempos.eliminarProceso(p.getNombre());
            guiTiempos.calcularYActualizarMedias();
        }
    }
}