package modelo;
/* ================= CLASE PROCESO =================
    Ayuda a representar un proceso individual en el sistema de planificación FIFO.
    En esta clase encontrarás:
     >> Todos los atributos necesarios para calcular las métricas del algoritmo
 */

public class Proceso {
    // Atributos básicos del proceso
    private String nombre; //Identificador único del proceso (utilizaremos UUID para generarlo)
    private int tiempoCPU; // t - Tiempo de ejecución requerido
    private int tiempoCreacion; // C - Tiempo que tarda en pasar de nuevo a listo
    private EstadoProceso estado;

    // Atributos de tiempo (estos se calcularán durante la ejecución)
    private int tiempoLlegada; // Momento en que el proceso llega al sistema
    private int tiempoInicio; // Momento en que comienza a ejecutarse el programa
    private int tiempoFinalizacion; // F - Momento en que termina el proceso
    private int tiempoEspera; // E - Tiempo en estado LISTO
    private double penalizacion; // P - Proporción F/t

    // Utilizaremos la IP como identificar al cliente propietario del proceso
    private String clienteId;

    /*
        ======= Constructor principal =======
        @param nombre Identificador del proceso
        @param tiempoCPU Tiempo de ejecución del proceso
        @param tiempoCreacion Tiempo de creación (C)
        @param tiempoLlegada Momento en que el proceso llega al sistema

     */
    public Proceso(String nombre, int tiempoCPU, int tiempoCreacion, int tiempoLlegada) {
        this.nombre = nombre;
        this.tiempoCPU = tiempoCPU;
        this.tiempoCreacion = tiempoCreacion;
        this.tiempoLlegada = tiempoLlegada;
        this.estado = EstadoProceso.NUEVO;
        this.tiempoInicio = -1;
        this.tiempoFinalizacion = -1;
        this.tiempoEspera = 0;
        this.penalizacion = 0.0;
        this.clienteId = "";
    }

    // Constructor alternativo para procesos que llegan inmediatamente
    public Proceso(String nombre, int tiempoCPU, int tiempoCreacion) {
        this(nombre, tiempoCPU, tiempoCreacion, 0);
    }

    // ================= MÉTODOS DE CÁLCULO ================= //
    // Calcula el tiempo de espera según la formula: E= F-t
    public void calcularTiempoEspera() {
        if (tiempoFinalizacion > 0){ // Aseguramos que el proceso ya haya terminado o al menos tenga un tiempo de finalización válido
           this.tiempoEspera = tiempoFinalizacion - tiempoCPU;
           //Si el tiempo espera es negativo lo pasamos a 0, este caso puede pasar por algún error que debugiaremos después
            if (this.tiempoEspera < 0){
                System.out.println("El tiempo de espera salio negativo");
                this.tiempoEspera = 0;
            }
        }
    }

    //Calcular la penalización según la formula: P = F/t
    public void calcularPenalizacion() {
        //Aseguramos que tenga un tiempo de finalización y que haya tenido tiempo en CPU para evitar división entre cero
        if(tiempoFinalizacion >= 0 && tiempoCPU > 0){
            this.penalizacion = (double) tiempoFinalizacion / (double) tiempoCPU;
        }
    }

    //Calcula todas las metricas del proceso
    //Este debe ser llamado cuando el proceso está COMPLETADO
    public void calcularMetricas(){
        calcularTiempoEspera();
        calcularPenalizacion();
    }

    // ================= GETTER Y SETTERS ================= //
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getTiempoCPU() { return tiempoCPU; }
    public void setTiempoCPU(int tiempoCPU) { this.tiempoCPU = tiempoCPU; }

    public int getTiempoCreacion() { return tiempoCreacion; }
    public void setTiempoCreacion(int tiempoCreacion) { this.tiempoCreacion = tiempoCreacion; }

    public EstadoProceso getEstado() { return estado; }
    public void setEstado(EstadoProceso estado) { this.estado = estado; }

    public int getTiempoLlegada() { return tiempoLlegada; }
    public void setTiempoLlegada(int tiempoLlegada) { this.tiempoLlegada = tiempoLlegada; }

    public int getTiempoInicio() { return tiempoInicio; }
    public void setTiempoInicio(int tiempoInicio) { this.tiempoInicio = tiempoInicio; }

    public int getTiempoFinalizacion() { return tiempoFinalizacion; }
    public void setTiempoFinalizacion(int tiempoFinalizaciono) {
        this.tiempoFinalizacion = tiempoFinalizaciono;
        // Al establecer el tiempo de finalización, calculamos las métricas
        calcularMetricas();
    }

    public int getTiempoEspera() { return tiempoEspera; }

    public double getPenalizacion() { return penalizacion; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    // ================= MÉTODOS AUXILIARES ================= //
    //Verifica si el proceso ya ha llegado al sistema en un momento dado
    public boolean hallegado(int tiempoActual){
        return tiempoActual >= tiempoLlegada;
    }

    //Marca el proceso como iniciado
    public void iniciar(int tiempoActual){
        this.tiempoInicio = tiempoActual;
        this.estado = EstadoProceso.EJECUTANDO;
    }

    //Marca el proceso como completado
    public void completar(int tiempoActual){
        this.tiempoFinalizacion = tiempoActual;
        this.estado = EstadoProceso.COMPLETADO;
        //Cuando el proceso haya sido terminado entonces mandamos a calcular las métricas
        calcularMetricas();
    }

    //Representación en String del proceso
    @Override
    public String toString() {
        return String.format("Proceso[%s, CPU = %d, Estado = %s, E = %d, F = %d, P = %.2f]", nombre, tiempoCPU, estado,
                tiempoEspera, tiempoFinalizacion, penalizacion);
    }

    //Crea una copia del proceso (útil para la simulación)
    public Proceso copiar(){
        Proceso copia = new Proceso(this.nombre, this.tiempoCPU, this.tiempoCreacion, this.tiempoLlegada);
        copia.setClienteId(this.clienteId);
        return copia;
    }
}
