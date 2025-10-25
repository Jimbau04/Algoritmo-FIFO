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

    private WebServer webServer;
    private int puerto;
    private final int rangoPlanificador;
    private boolean ejecutando;
    private Thread hiloEjecucion;
    private int tiempoSimulacionGlobal = 0;

    private ColaProceso colaProceso;
    private Map<String, List<String>> clientesProcesos;
    private Map<String, Timer> timersInactividad;

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

    private void iniciarHiloEjecucion() {
        ejecutando = true;
        hiloEjecucion = new Thread(() -> {
            System.out.println("✓ Hilo de ejecución (Planificador) iniciado.");
            while (ejecutando) {
                try {
                    System.out.println("\n═══════════════════════════════════════");
                    System.out.println("  TICK: " + tiempoSimulacionGlobal);
                    System.out.println("═══════════════════════════════════════");

                    // 1. PROCESAR PROCESOS PENDIENTES (por tiempo de petición)
                    List<Proceso> procesosIntentados = colaProceso.procesarPendientes(
                            tiempoSimulacionGlobal, rangoPlanificador);

                    for (Proceso p : procesosIntentados) {
                        if (p.getEstado() == EstadoProceso.EN_COLA) {
                            notificarGUIs_Nuevo(p);
                        } else if (p.getEstado() == EstadoProceso.EN_FILA_ESPERA) {
                            notificarGUIs_NuevoEnEspera(p);
                        }
                    }

                    // 2. INCREMENTAR TICKS EN ESPERA
                    colaProceso.incrementarTicksEspera();

                    Proceso pEnEjecucion = colaProceso.getProcesoEnEjecucion();

                    if (pEnEjecucion != null) {
                        // 3. EJECUTAR UN TICK
                        pEnEjecucion.incrementarTiempoEjecutado();
                        notificarGUIs_TickEjecucion(pEnEjecucion, tiempoSimulacionGlobal);

                        System.out.println("  Ejecutando: " + pEnEjecucion.getNombre() +
                                " (" + pEnEjecucion.getTiempoEjecutado() +
                                "/" + pEnEjecucion.getTiempoCPU() + ")");

                        // 4. COMPROBAR SI TERMINÓ
                        if (pEnEjecucion.getTiempoEjecutado() >= pEnEjecucion.getTiempoCPU()) {
                            System.out.println("  ✓ Proceso terminado: " + pEnEjecucion.getNombre());
                            colaProceso.marcarProcesoTerminado(tiempoSimulacionGlobal);
                            notificarGUIs_Terminado(pEnEjecucion);
                        }
                    }
                    else if (!colaProceso.estaVacia()) {
                        // 5. INICIAR NUEVO PROCESO
                        Proceso pNuevo = colaProceso.obtenerSiguienteProceso();
                        if (pNuevo != null) {
                            pNuevo.setTiempoInicio(tiempoSimulacionGlobal);
                            pNuevo.incrementarTiempoEjecutado();

                            System.out.println("  Iniciando: " + pNuevo.getNombre() +
                                    " (Espera: " + pNuevo.getTiempoEspera() + ")");

                            notificarGUIs_Ejecucion(pNuevo);
                            notificarGUIs_TickEjecucion(pNuevo, tiempoSimulacionGlobal);
                        }
                    } else {
                        System.out.println("  CPU libre (sin procesos)");
                    }

                    // 6. CHEQUEAR REINTENTOS DE PROCESOS EN ESPERA
                    chequearProcesosEnEspera();

                    // Mostrar estado de las colas
                    if (colaProceso.hayProcesosPendientes()) {
                        System.out.println("  Pendientes: " + colaProceso.getProcesosPendientes().size());
                    }
                    if (colaProceso.hayProcesosEnEspera()) {
                        System.out.println("  En Espera: " + colaProceso.getProcesosEnEspera().size());
                    }

                    // 7. AVANZAR EL RELOJ
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

    private synchronized void chequearProcesosEnEspera() {
        List<Proceso> procesosListos = colaProceso.getProcesosListosParaReintento();

        for (Proceso p : procesosListos) {
            int espacioUsado = colaProceso.calcularEspacioUsado(tiempoSimulacionGlobal);
            int espacioDisponible = rangoPlanificador - espacioUsado;

            System.out.println("  Reintentando: " + p.getNombre() +
                    " (Ticks espera: " + colaProceso.getTicksEnEspera(p.getNombre()) +
                    ", Intento: " + (colaProceso.getIntentos(p.getNombre()) + 1) + ")");
            System.out.println("    Espacio usado: " + espacioUsado + "/" + rangoPlanificador +
                    ", Disponible: " + espacioDisponible + ", Necesita: " + p.getTiempoCPU());

            if (espacioDisponible >= p.getTiempoCPU()) {
                // CABE - Mover a cola de listos
                colaProceso.removerDeEspera(p);

                int finPlanActual = colaProceso.getTiempoFinPlanificado();
                int tiempoLlegada = Math.max(tiempoSimulacionGlobal, finPlanActual + 1);
                int tiempoFinEstimado = tiempoLlegada + p.getTiempoCPU() - 1;

                p.setTiempoLlegada(tiempoLlegada);
                p.setTiempoFin(tiempoFinEstimado);

                colaProceso.agregarProceso(p);
                System.out.println("    ✓ ACEPTADO en reintento (Plan: " + tiempoLlegada +
                        " a " + tiempoFinEstimado + ")");
                notificarGUIs_MovidoA_Listo(p);

            } else {
                // NO CABE
                colaProceso.incrementarIntentos(p.getNombre());
                int intentos = colaProceso.getIntentos(p.getNombre());

                if (intentos >= 2) {
                    // MATAR después del segundo intento fallido
                    colaProceso.removerDeEspera(p);
                    colaProceso.matarProceso(p, EstadoProceso.RECHAZADO);
                    System.out.println("    ✗ RECHAZADO definitivamente (2 intentos fallidos)");
                    notificarGUIs_Rechazado(p);
                } else {
                    // Resetear ticks para esperar otros 5
                    System.out.println("    ! Sigue sin caber, esperará otros 5 ticks");
                    notificarGUIs_ActualizarEspera(p);
                }
            }
        }
    }

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
                if (p != null && (p.getEstado() == EstadoProceso.NUEVO ||
                        p.getEstado() == EstadoProceso.EN_COLA ||
                        p.getEstado() == EstadoProceso.EN_FILA_ESPERA)) {
                    colaProceso.eliminarProceso(nombreProceso);
                    notificarGUIs_Eliminado(p);
                }
            }
        }

        clientesProcesos.remove(clienteId);
        System.out.println("✓ Cliente desconectado: " + clienteId);
        return true;
    }

    public synchronized boolean agregarProceso(String clienteId, String nombreProceso,
                                               int tiempoCPU, int tiempoPeticion) {
        if (!clientesProcesos.containsKey(clienteId)) return false;
        if (colaProceso.getProceso(nombreProceso) != null) return false;

        resetTimerInactividad(clienteId);

        System.out.println("\n[AGREGAR] " + nombreProceso + " - CPU: " + tiempoCPU +
                ", Petición (t): " + tiempoPeticion);

        // Crear el proceso con su tiempo de petición
        Proceso p = new Proceso(clienteId, nombreProceso, tiempoCPU, tiempoPeticion, tiempoPeticion);

        // Agregarlo a la cola de PENDIENTES (se procesará cuando llegue su tick)
        colaProceso.agregarProcesoPendiente(p);

        // Notificar a la GUI que el proceso está pendiente
        notificarGUIs_Pendiente(p);

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
            notificarGUIs_Eliminado(p);
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

    private void iniciarTimerInactividad(String clienteId) {
        Timer timer = new Timer(true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (colaProceso.getProcesosActivosCliente(clienteId).isEmpty()) {
                    System.out.println("! Timeout de inactividad para: " + clienteId);
                    desconectarCliente(clienteId);
                } else {
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

    // ============ NOTIFICACIONES A GUIs ============

    private void notificarGUIs_Pendiente(Proceso p) {
        if (guiColas != null) {
            guiColas.agregarNuevo(p.getNombre() + " (t=" + p.getTiempoPeticion() + ")");
        }
        if (guiProcesos != null) {
            // Solo registrar el tick de petición (P)
            guiProcesos.registrarTareaCompleta(p.getNombre(), -1, -1, -1, p.getTiempoPeticion());
        }
        if (guiTiempos != null) {
            guiTiempos.agregarProceso(p.getNombre());
            guiTiempos.actualizarTiempoEspera(p.getNombre(), "PEND(" + p.getTiempoPeticion() + ")");
        }
    }

    private void notificarGUIs_Nuevo(Proceso p) {
        if (guiColas != null) {
            guiColas.moverA_Listo(p.getNombre());
        }
        if (guiProcesos != null) {
            guiProcesos.registrarTareaCompleta(p.getNombre(), p.getTiempoLlegada(),
                    -1, p.getTiempoFin(), p.getTiempoPeticion());
        }
        if (guiTiempos != null) {
            guiTiempos.agregarProceso(p.getNombre());
        }
    }

    private void notificarGUIs_NuevoEnEspera(Proceso p) {
        if (guiColas != null) {
            guiColas.moverA_Bloqueado(p.getNombre() + " (Espera)");
        }
        if (guiProcesos != null) {
            guiProcesos.registrarTareaCompleta(p.getNombre(), -1, -1, -1, p.getTiempoPeticion());
        }
        if (guiTiempos != null) {
            guiTiempos.agregarProceso(p.getNombre());
            guiTiempos.actualizarTiempoEspera(p.getNombre(), "ESPERA");
        }
    }

    private void notificarGUIs_MovidoA_Listo(Proceso p) {
        if (guiColas != null) {
            guiColas.moverA_Listo(p.getNombre());
        }
        if (guiProcesos != null) {
            guiProcesos.registrarTareaCompleta(p.getNombre(), p.getTiempoLlegada(),
                    -1, p.getTiempoFin(), p.getTiempoPeticion());
        }
        if (guiTiempos != null) {
            guiTiempos.actualizarTiempoEspera(p.getNombre(), 0);
        }
    }

    private void notificarGUIs_ActualizarEspera(Proceso p) {
        if (guiTiempos != null) {
            int ticks = colaProceso.getTicksEnEspera(p.getNombre());
            guiTiempos.actualizarTiempoEspera(p.getNombre(),
                    "ESPERA(" + ticks + "/5)");
        }
    }

    private void notificarGUIs_Ejecucion(Proceso p) {
        if (guiColas != null) guiColas.moverA_Ejecucion(p.getNombre());
        if (guiProcesos != null) {
            guiProcesos.registrarTareaCompleta(p.getNombre(), p.getTiempoLlegada(),
                    p.getTiempoInicio(), p.getTiempoFin(), p.getTiempoPeticion());
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
            guiTiempos.actualizarTiempoFinalizacion(p.getNombre(),
                    p.getTiempoFinalizacion());
            guiTiempos.actualizarPenalizacion(p.getNombre(),
                    String.format("%.2f", p.getPenalizacion()));
            guiTiempos.calcularYActualizarMedias();
        }
    }

    private void notificarGUIs_Rechazado(Proceso p) {
        if (guiColas != null) {
            guiColas.moverA_Terminado(p.getNombre() + " (RECHAZADO)");
        }
        if (guiTiempos != null) {
            guiTiempos.eliminarProceso(p.getNombre());
            guiTiempos.calcularYActualizarMedias();
        }
    }

    private void notificarGUIs_Eliminado(Proceso p) {
        if (guiColas != null) {
            guiColas.moverA_Terminado(p.getNombre() + " (ELIMINADO)");
        }
        if (guiTiempos != null) {
            guiTiempos.eliminarProceso(p.getNombre());
            guiTiempos.calcularYActualizarMedias();
        }
    }

    public int getTiempoSimulacionGlobal() {
        return tiempoSimulacionGlobal;
    }
}