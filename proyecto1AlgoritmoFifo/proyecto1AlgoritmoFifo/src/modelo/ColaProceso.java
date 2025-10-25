package modelo;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ColaProceso {

    private final Queue<Proceso> colaListos = new LinkedList<>();
    private final Queue<Proceso> colaEsperaCapacidad = new LinkedList<>();
    private final Map<String, Integer> intentosProceso = new ConcurrentHashMap<>();

    private Proceso procesoEnEjecucion = null;
    private final Map<String, Proceso> registroTotalProcesos = new ConcurrentHashMap<>();

    public synchronized void agregarProceso(Proceso p) {
        registroTotalProcesos.put(p.getNombre(), p);
        p.setEstado(EstadoProceso.LISTO);
        colaListos.add(p);
    }

    public synchronized void agregarA_EsperaCapacidad(Proceso p) {
        if (!registroTotalProcesos.containsKey(p.getNombre())) {
            registroTotalProcesos.put(p.getNombre(), p);
        }
        p.setEstado(EstadoProceso.RECHAZADO);
        colaEsperaCapacidad.add(p);
        intentosProceso.putIfAbsent(p.getNombre(), 0);
    }

    public synchronized Proceso obtenerSiguienteDe_Espera() {
        return colaEsperaCapacidad.poll();
    }

    public int getIntentos(String nombreProceso) {
        return intentosProceso.getOrDefault(nombreProceso, 0);
    }

    public void incrementarIntentos(String nombreProceso) {
        intentosProceso.put(nombreProceso, getIntentos(nombreProceso) + 1);
    }

    public synchronized boolean hayProcesosEnEspera() {
        return !colaEsperaCapacidad.isEmpty();
    }

    public synchronized Proceso obtenerSiguienteProceso() {
        if (procesoEnEjecucion != null || colaListos.isEmpty()) {
            return null;
        }
        procesoEnEjecucion = colaListos.poll();
        if (procesoEnEjecucion == null) return null;
        procesoEnEjecucion.setEstado(EstadoProceso.EJECUCION);
        return procesoEnEjecucion;
    }

    public synchronized void marcarProcesoTerminado(int tiempoGlobal) {
        if (procesoEnEjecucion != null) {
            procesoEnEjecucion.setEstado(EstadoProceso.TERMINADO);
            procesoEnEjecucion.setTiempoFin(tiempoGlobal);
            procesoEnEjecucion = null;
        }
    }

    public synchronized boolean eliminarProceso(String nombreProceso) {
        Proceso p = registroTotalProcesos.get(nombreProceso);
        if (p == null) return false;
        if (procesoEnEjecucion != null && procesoEnEjecucion.getNombre().equals(nombreProceso)) {
            return false;
        }
        colaListos.removeIf(proc -> proc.getNombre().equals(nombreProceso));
        colaEsperaCapacidad.removeIf(proc -> proc.getNombre().equals(nombreProceso));
        p.setEstado(EstadoProceso.ELIMINADO);
        return true;
    }

    public synchronized void matarProceso(Proceso p, EstadoProceso razon) {
        p.setEstado(razon);
        colaListos.remove(p);
        colaEsperaCapacidad.remove(p);
        if(procesoEnEjecucion == p) {
            procesoEnEjecucion = null;
        }
    }

    public Proceso getProcesoEnEjecucion() { return procesoEnEjecucion; }
    public synchronized boolean hayProcesoEnEjecucion() { return procesoEnEjecucion != null; }
    public synchronized boolean estaVacia() { return colaListos.isEmpty(); }

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

        for (Proceso p : colaEsperaCapacidad) {
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
                .filter(p -> p.getEstado() == EstadoProceso.LISTO || p.getEstado() == EstadoProceso.EJECUCION)
                .collect(Collectors.toList());
    }

    public Proceso getProceso(String nombre) {
        return registroTotalProcesos.get(nombre);
    }
}

