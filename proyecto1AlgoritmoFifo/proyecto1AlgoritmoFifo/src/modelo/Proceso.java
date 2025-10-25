package modelo;

import java.io.Serializable;

public class Proceso implements Serializable {
    private String clienteId;
    private String nombre;
    private int tiempoCPU; // 'C'
    private EstadoProceso estado;
    private int tiempoLlegada; // El tick en el que se planifica su inicio
    private int tiempoInicio;  // El tick real en el que se empezó a ejecutar
    private int tiempoPeticion;
    private int tiempoFin;
    private int tiempoEjecutado = 0; // Contador de ticks

    public Proceso(String clienteId, String nombre, int tiempoCPU,int tiempoPeticion, int tiempoLlegada) {
        this.clienteId = clienteId;
        this.nombre = nombre;
        this.tiempoCPU = tiempoCPU;
        this.tiempoPeticion = tiempoPeticion;
        this.estado = EstadoProceso.LISTO;
        this.tiempoLlegada = tiempoLlegada; // Este es el tiempo *planificado*
        this.tiempoInicio = -1;
        this.tiempoFin = -1;
    }

    public int getTiempoEjecutado() {
        return tiempoEjecutado;
    }

    public void incrementarTiempoEjecutado() {
        this.tiempoEjecutado++;
    }

    // Getters
    public String getClienteId() { return clienteId; }
    public String getNombre() { return nombre; }
    public int getTiempoCPU() { return tiempoCPU; }
    public EstadoProceso getEstado() { return estado; }
    public int getTiempoPeticion() { return tiempoPeticion; }
    public int getTiempoLlegada() { return tiempoLlegada; }
    public int getTiempoInicio() { return tiempoInicio; }
    public int getTiempoFin() { return tiempoFin; }

    // Setters
    public void setEstado(EstadoProceso estado) { this.estado = estado; }
    public void setTiempoInicio(int tiempoInicio) { this.tiempoInicio = tiempoInicio; }
    public void setTiempoFin(int tiempoFin) { this.tiempoFin = tiempoFin; }
    public void setTiempoLlegada(int tiempoLlegada) { this.tiempoLlegada = tiempoLlegada; }

    // Métricas calculadas
    public int getTiempoFinalizacion() { // 'F'
        return tiempoFin;
    }
    public int getTiempoEspera() { // 'E'
        if (tiempoInicio == -1) return 0; // Aún no ha empezado
        return tiempoInicio - tiempoLlegada;
    }
    public double getPenalizacion() { // 'P'
        if (tiempoFin == -1 || tiempoCPU <= 0) return 0.0;
        double tiempoRetorno = (double) tiempoFin - tiempoLlegada + 1; // +1 porque los ticks son inclusivos
        if (tiempoRetorno <= 0) return 1.0;
        return tiempoRetorno / tiempoCPU;
    }

    @Override
    public String toString() {
        return String.format(
                "Proceso[%s, Cliente=%s, Estado=%s, CPU=%d, " +
                        "Petición=%d, Llegada=%d, Inicio=%d, Fin=%d, " +
                        "E=%d, F=%d, P=%.2f]",
                nombre, clienteId, estado, tiempoCPU,
                tiempoPeticion, tiempoLlegada, tiempoInicio, tiempoFin,
                getTiempoEspera(), getTiempoFinalizacion(),
                getPenalizacion()
        );
    }
}
