package modelo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ColaProceso {

    // Cola principal de procesos listos para ejecutarse
    private final Queue<Proceso> colaListos = new LinkedList<>();

    // Cola de procesos que no caben en el rango (esperando espacio)
    private final Queue<Proceso> colaEspera = new LinkedList<>();

    // Cola de procesos pendientes (esperando que llegue su tiempo de petición)
    private final Queue<Proceso> colaPendientes = new LinkedList<>();

    // Contador de intentos de reingreso para cada proceso
    private final Map<String, Integer> intentosProceso = new ConcurrentHashMap<>();

    // Contador de ticks que cada proceso lleva en espera
    private final Map<String, Integer> ticksEnEspera = new ConcurrentHashMap<>();

    // Proceso actualmente en ejecución en la CPU
    private Proceso procesoEnEjecucion = null;

    // Registro completo de todos los procesos (histórico)
    private final Map<String, Proceso> registroTotalProcesos = new ConcurrentHashMap<>();

    /**
     * Agrega un proceso a la cola de PENDIENTES (aún no llega su tiempo de petición)
     */
    public synchronized void agregarProcesoPendiente(Proceso p) {
        registroTotalProcesos.put(p.getNombre(), p);
        p.setEstado(EstadoProceso.NUEVO);
        colaPendientes.add(p);
        System.out.println("  → Proceso " + p.getNombre() + " pendiente hasta tick " + p.getTiempoPeticion());
    }

    /**
     * Chequea procesos pendientes y los intenta agregar si ya llegó su tiempo de petición
     * @param tiempoActual El tick actual del planificador
     * @return Lista de procesos que intentaron ingresar
     */
    public synchronized List<Proceso> procesarPendientes(int tiempoActual, int rangoPlanificador) {
        List<Proceso> procesosIntentados = new ArrayList<>();
        Iterator<Proceso> it = colaPendientes.iterator();

        while (it.hasNext()) {
            Proceso p = it.next();

            // Si ya llegó su tiempo de petición
            if (tiempoActual >= p.getTiempoPeticion()) {
                it.remove(); // Sacar de pendientes
                procesosIntentados.add(p);

                // Intentar agregarlo según disponibilidad de espacio
                int espacioUsado = calcularEspacioUsado(tiempoActual);
                int espacioDisponible = rangoPlanificador - espacioUsado;

                System.out.println("  [TICK " + tiempoActual + "] Procesando petición de: " + p.getNombre());
                System.out.println("    Espacio: " + espacioUsado + "/" + rangoPlanificador +
                        " usado, " + espacioDisponible + " disponible, necesita: " + p.getTiempoCPU());

                if (espacioDisponible >= p.getTiempoCPU()) {
                    // CABE - Agregar a cola de listos
                    agregarProcesoInterno(p, tiempoActual);
                } else {
                    // NO CABE - A cola de espera
                    agregarA_EsperaInterno(p);
                }
            }
        }

        return procesosIntentados;
    }

    /**
     * Agrega un proceso a la cola de listos (uso interno)
     */
    private synchronized void agregarProcesoInterno(Proceso p, int tiempoActual) {
        int finPlanActual = getTiempoFinPlanificado();
        int tiempoLlegada = Math.max(tiempoActual, finPlanActual + 1);
        int tiempoFinEstimado = tiempoLlegada + p.getTiempoCPU() - 1;

        p.setTiempoLlegada(tiempoLlegada);
        p.setTiempoFin(tiempoFinEstimado);
        p.setEstado(EstadoProceso.EN_COLA);
        colaListos.add(p);

        System.out.println("    ✓ LISTO (Plan: " + tiempoLlegada + " a " + tiempoFinEstimado + ")");
    }

    /**
     * Agrega un proceso a la cola de espera (uso interno)
     */
    private synchronized void agregarA_EsperaInterno(Proceso p) {
        p.setEstado(EstadoProceso.EN_FILA_ESPERA);
        colaEspera.add(p);
        intentosProceso.putIfAbsent(p.getNombre(), 0);
        ticksEnEspera.put(p.getNombre(), 0);

        System.out.println("    ! EN_ESPERA (no cabe, esperará 5 ticks)");
    }

    /**
     * Agrega un proceso a la cola de listos (para reintentos desde espera)
     */
    public synchronized void agregarProceso(Proceso p) {
        p.setEstado(EstadoProceso.EN_COLA);
        colaListos.add(p);
    }

    /**
     * Agrega un proceso a la cola de espera (para uso externo)
     */
    public synchronized void agregarA_Espera(Proceso p) {
        if (!registroTotalProcesos.containsKey(p.getNombre())) {
            registroTotalProcesos.put(p.getNombre(), p);
        }
        agregarA_EsperaInterno(p);
    }

    /**
     * Obtiene la lista de procesos en espera
     */
    public synchronized List<Proceso> getProcesosEnEspera() {
        return new ArrayList<>(colaEspera);
    }

    /**
     * Obtiene la lista de procesos pendientes
     */
    public synchronized List<Proceso> getProcesosPendientes() {
        return new ArrayList<>(colaPendientes);
    }

    /**
     * Incrementa el contador de ticks para TODOS los procesos en espera
     */
    public synchronized void incrementarTicksEspera() {
        for (Proceso p : colaEspera) {
            int ticksActuales = ticksEnEspera.getOrDefault(p.getNombre(), 0);
            ticksEnEspera.put(p.getNombre(), ticksActuales + 1);
        }
    }

    /**
     * Devuelve la lista de procesos que ya llevan 5 ticks en espera
     */
    public synchronized List<Proceso> getProcesosListosParaReintento() {
        List<Proceso> listos = new ArrayList<>();
        for (Proceso p : colaEspera) {
            if (ticksEnEspera.getOrDefault(p.getNombre(), 0) >= 5) {
                listos.add(p);
            }
        }
        return listos;
    }

    /**
     * Remueve un proceso de la cola de espera
     */
    public synchronized boolean removerDeEspera(Proceso p) {
        ticksEnEspera.remove(p.getNombre());
        return colaEspera.remove(p);
    }

    /**
     * Obtiene el número de intentos de reingreso de un proceso
     */
    public int getIntentos(String nombreProceso) {
        return intentosProceso.getOrDefault(nombreProceso, 0);
    }

    /**
     * Incrementa el contador de intentos para un proceso
     */
    public void incrementarIntentos(String nombreProceso) {
        intentosProceso.put(nombreProceso, getIntentos(nombreProceso) + 1);
    }

    /**
     * Verifica si hay procesos en la cola de espera
     */
    public synchronized boolean hayProcesosEnEspera() {
        return !colaEspera.isEmpty();
    }

    /**
     * Verifica si hay procesos pendientes
     */
    public synchronized boolean hayProcesosPendientes() {
        return !colaPendientes.isEmpty();
    }

    /**
     * Obtiene el siguiente proceso de la cola de listos y lo pone en ejecución
     */
    public synchronized Proceso obtenerSiguienteProceso() {
        if (procesoEnEjecucion != null || colaListos.isEmpty()) {
            return null;
        }
        procesoEnEjecucion = colaListos.poll();
        if (procesoEnEjecucion == null) return null;
        procesoEnEjecucion.setEstado(EstadoProceso.EJECUCION);
        return procesoEnEjecucion;
    }

    /**
     * Marca el proceso en ejecución como terminado
     */
    public synchronized void marcarProcesoTerminado(int tiempoGlobal) {
        if (procesoEnEjecucion != null) {
            procesoEnEjecucion.setEstado(EstadoProceso.TERMINADO);
            procesoEnEjecucion.setTiempoFin(tiempoGlobal);
            procesoEnEjecucion = null;
        }
    }

    /**
     * Elimina un proceso de las colas (solo si NO está en ejecución)
     */
    public synchronized boolean eliminarProceso(String nombreProceso) {
        Proceso p = registroTotalProcesos.get(nombreProceso);
        if (p == null) return false;

        if (procesoEnEjecucion != null && procesoEnEjecucion.getNombre().equals(nombreProceso)) {
            return false;
        }

        colaListos.removeIf(proc -> proc.getNombre().equals(nombreProceso));
        colaEspera.removeIf(proc -> proc.getNombre().equals(nombreProceso));
        colaPendientes.removeIf(proc -> proc.getNombre().equals(nombreProceso));
        ticksEnEspera.remove(nombreProceso);
        intentosProceso.remove(nombreProceso);
        p.setEstado(EstadoProceso.ELIMINADO);
        return true;
    }

    /**
     * Mata un proceso forzosamente
     */
    public synchronized void matarProceso(Proceso p, EstadoProceso razon) {
        p.setEstado(razon);
        colaListos.remove(p);
        colaEspera.remove(p);
        colaPendientes.remove(p);
        ticksEnEspera.remove(p.getNombre());
        intentosProceso.remove(p.getNombre());
        if(procesoEnEjecucion == p) {
            procesoEnEjecucion = null;
        }
    }

    public Proceso getProcesoEnEjecucion() {
        return procesoEnEjecucion;
    }

    public synchronized boolean hayProcesoEnEjecucion() {
        return procesoEnEjecucion != null;
    }

    public synchronized boolean estaVacia() {
        return colaListos.isEmpty();
    }

    /**
     * Calcula cuántos ticks del planificador están ocupados actualmente
     */
    public synchronized int calcularEspacioUsado(int tiempoActual) {
        int espacioUsado = 0;

        if (procesoEnEjecucion != null) {
            int ticksRestantes = procesoEnEjecucion.getTiempoCPU() - procesoEnEjecucion.getTiempoEjecutado();
            espacioUsado = ticksRestantes;
        }

        for (Proceso p : colaListos) {
            espacioUsado += p.getTiempoCPU();
        }

        return espacioUsado;
    }

    /**
     * Obtiene el tiempo de finalización planificado más lejano
     */
    public synchronized int getTiempoFinPlanificado() {
        int maxFin = 0;

        if (procesoEnEjecucion != null) {
            maxFin = procesoEnEjecucion.getTiempoFin();
        }

        for (Proceso p : colaListos) {
            if (p.getTiempoFin() > maxFin) {
                maxFin = p.getTiempoFin();
            }
        }

        return maxFin;
    }

    public synchronized List<Proceso> getTodosLosProcesos() {
        return List.copyOf(registroTotalProcesos.values());
    }

    public synchronized List<Proceso> getProcesosActivosCliente(String clienteId) {
        return registroTotalProcesos.values().stream()
                .filter(p -> p.getClienteId().equals(clienteId))
                .filter(p -> p.getEstado() == EstadoProceso.NUEVO ||
                        p.getEstado() == EstadoProceso.EN_COLA ||
                        p.getEstado() == EstadoProceso.EJECUCION ||
                        p.getEstado() == EstadoProceso.EN_FILA_ESPERA)
                .collect(Collectors.toList());
    }

    public Proceso getProceso(String nombre) {
        return registroTotalProcesos.get(nombre);
    }

    public int getTicksEnEspera(String nombreProceso) {
        return ticksEnEspera.getOrDefault(nombreProceso, 0);
    }
}