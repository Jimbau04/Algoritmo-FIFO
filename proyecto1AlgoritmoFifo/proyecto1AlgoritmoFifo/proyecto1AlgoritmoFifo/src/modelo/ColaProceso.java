package modelo;
// ================= CLASE COLA PPOCESO ================= //
/*Esta clase implementa la cola FIFO para la planifiación del proceso, utilizare Thread-safe para uso en entorno de
red con múltiples clientes
 */

import java.util.List;
import java.util.Queue;
import java.util.ArrayList;
import java.util.LinkedList;

public class ColaProceso {
    // ================= ATRIBUTOS ================= //
    // Cola de procesos en espera (FIFO)
    private Queue<Proceso> colaEspera;

    // Lista de todos los procesos (para llevar registro histórico)
    private List<Proceso> todosLosProcesos;

    // Proceso actualmente en ejecución
    private Proceso procesoEnEjecucion;

    // Reloj del sistema (Se ocupa para simular el tiempo actual)
    private int tiempoActual;

    // ================ CONSTRUCTOR ================ //
    public ColaProceso() {
        this.colaEspera = new LinkedList<>();
        this.todosLosProcesos = new ArrayList<>();
        this.procesoEnEjecucion = null;
        this.tiempoActual = 0;
    }

    // ================ OPERACIONES DE LA COLA (MÉTODOS) ================= //
    // Agregar un proceso a la cola de espera (El proceso una vez dentro para al estado LISTO)
    public synchronized void agregarProceso(Proceso proceso) {
        if(proceso == null){
            throw new IllegalArgumentException("El proceso no puede ser nulo");
        }

        // Si el proceso acaba de llegar, actualizamos su tiempo de llegada
        if(proceso.getEstado() == EstadoProceso.NUEVO){
            proceso.setTiempoLlegada(tiempoActual);
            proceso.setEstado(EstadoProceso.LISTO);
        }

        colaEspera.offer(proceso); // Agregamos el proceso al final de la cola de espera con offer
        todosLosProcesos.add(proceso);

        // Para debugiar chavoooos
        System.out.println("Proceso" + proceso.getNombre() + " agregado a la cola en tiempo " + tiempoActual);
    }

    // Obtiene el siguiente proceso en la cola (Cambiamos el proceso ha estado EJECUTANDO)
    public synchronized Proceso obtenerSiguienteProceso() {
        if(colaEspera.isEmpty()){
            // En esta parte cuando integremos el RPC podemos poner un tiempo de espera antes de KILL el servidor
            System.out.println("La cola de procesos esta vacia");
            return null;
        }

        Proceso proceso = colaEspera.poll(); // Obtiene el primer elemento de la cola, lo elimina y devuelve el elemento
        proceso.iniciar(tiempoActual); // Mandamos el método a iniciar
        procesoEnEjecucion = proceso; // Le decimos al servidor que hay un proceso en marcha

        System.out.println("Proceso " + proceso.getNombre() + " comenzó ejecución en tiempo " + tiempoActual);

        return proceso;
    }

    // Marca el proceso actual como completado (Actualiza el reloj del sistema sumando el tiempo de CPU del proceso)
    public synchronized void completaProcesoActual(){
        if(procesoEnEjecucion == null){
            System.out.println("No hay procesos en ejecución");
            return;
        }

        // Avanzamos el reloj del sistema
        tiempoActual += procesoEnEjecucion.getTiempoCPU();

        // Completamos el proceso
        procesoEnEjecucion.completar(tiempoActual);

        System.out.println("Proceso " + procesoEnEjecucion.getNombre() + " completado en tiempo " + tiempoActual);

        procesoEnEjecucion = null;
    }

    /*Elimina un proceso de la cola de espera por su nombre
        @return true si eliminó, false si no se encontró
     */
    public synchronized boolean eliminarProceso(String nombreProceso) {
        Proceso procesoEliminar = null;

        //Busca el proceso en la cola
        for(Proceso p : colaEspera){
            if(p.getNombre().equals(nombreProceso)){
                procesoEliminar = p;
                break;
            }
        }

        if(procesoEliminar != null){
            colaEspera.remove(procesoEliminar); //Eliminamos el proceso de la cola (Ivan no empieces con tus obsenidades ... sin mi)
            procesoEliminar.setEstado(EstadoProceso.ELIMINADO);
            System.out.println("Proceso " + nombreProceso + " ha sido eliminado de la cola");
            return true;
        }

        System.out.println("Proceso " + nombreProceso + " no encontrado");
        return false;
    }

    // ================= CONSULTAS ================= //
    // Retorna la cantidad de procesos en la cola de espera
    public synchronized int tamanioCola(){
        return colaEspera.size();
    }

    // Verifica si la cola esta vacía
    public synchronized boolean estaVacia() {
        return colaEspera.isEmpty();
    }

    // Verifica si hay un proceso ejecutándose
    public synchronized boolean hayProcesoEnEjecucion() {
        return procesoEnEjecucion != null;
    }

    // Obtiene el proceso actualmente en ejecución
    public synchronized Proceso getProcesoEnEjecucion() {
        return procesoEnEjecucion;
    }

    // Obtiene una copia de la cola de espera (para mostrar en la interfaz)
    public synchronized List<Proceso> getColaEspera() {
        return new ArrayList<>(colaEspera);
    }

    // Obtiene todos los procesos (históricos completo)
    public synchronized List<Proceso> getTodosLosProcesos(){
        return new ArrayList<>(todosLosProcesos);
    }

    // Obtiene solo los procesos completados
    public synchronized List<Proceso> getProcesosCompletados(){
        List<Proceso> completados = new ArrayList<>();
        for(Proceso p : todosLosProcesos){
            if (p.getEstado() == EstadoProceso.COMPLETADO){
                completados.add(p); // Agregamos el proceso solo si su estado es COMPLETADO
            }
        }
        return completados;
    }

    // Obtiene el tiempo actual del sistema
    public synchronized int getTiempoActual() {
        return tiempoActual;
    }

    // Establece el tiempo actual (Util para la inicialización que vamos a simular, aunque podemos tomar el tiempo de la CPU)
    public synchronized void setTiempoActual(int tiempo) {
        this.tiempoActual = tiempo;
    }

    // ================= MÉTRICAS ================= //
    // Calcula el tiempo promedio de espera de todos los proceso completados
    public synchronized double calcularTiempoEsperaPromedio(){
        List<Proceso> completados = getProcesosCompletados();
        if(completados.isEmpty()){
            return 0.0;
        }

        int sumaEspera = 0;
        for(Proceso p : completados){
            sumaEspera += p.getTiempoCPU();
        }

        return (double)sumaEspera / completados.size();
    }

    // Calcula el tiempo promedio de finalización
    public synchronized double calcularTiempoFinalizacionPromedio(){
        List<Proceso> completados = getProcesosCompletados();
        if(completados.isEmpty()){
            return 0.0;
        }

        int sumaFinalizacion = 0;
        for(Proceso p : completados){
            sumaFinalizacion += p.getTiempoFinalizacion();
        }

        return (double)sumaFinalizacion / completados.size();
    }

    // Calcula la penalización promedio
    public synchronized double calcularPenalizacionPromedio(){
        List<Proceso> completados = getProcesosCompletados();
        if(completados.isEmpty()){
            return 0.0;
        }

        double sumaPenalizacion = 0;
        for(Proceso p : completados){
            sumaPenalizacion += p.getPenalizacion();
        }

        return sumaPenalizacion / completados.size();
    }

    // ================= UTILIDADES ================= //
    // Imprime el estado actual de la cola (Para debugiar chavooooos)
    public synchronized void imprimirEstado(){
        System.out.println(" ================= ESTADO DEL SISTEMA ================= ");
        System.out.println(" >> Tiempo Actual: " + tiempoActual);
        System.out.println(" >> Proceso en ejecución " + (procesoEnEjecucion != null ? procesoEnEjecucion.getNombre() : "Ninguno jijiji" ));
        System.out.println(" >> Procesos en cola de espera: " + colaEspera.size());

        if(!colaEspera.isEmpty()){
            System.out.println(" Cola FIFO \n");
            int posicion = 1;
            for(Proceso p : colaEspera){
                System.out.println(" >> " + posicion + ": " + p.getNombre() + " Tiempo en CPU = " + p.getTiempoCPU());
                posicion++;
            }
        }

        System.out.println(" >> Procesos completados: " + getProcesosCompletados().size());
        System.out.println(" ====================================================== ");
    }

    // Limpia toda la cola (por si el profe quiere matar al servidor, es capaz de hacerlo)
    public synchronized void limpiar(){
        colaEspera.clear(); // Borramos la base de datos XD
        todosLosProcesos.clear();
        procesoEnEjecucion = null;
        tiempoActual = 0;
        System.out.println("Servidor Reiniciado");
    }
}

