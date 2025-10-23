import modelo.Proceso;
import modelo.ColaProceso;
import modelo.EstadoProceso;

// Clase de prueba para verificar el funcionamiento del algoritmo FIFO
// Simula lo que el profe presento en el pizarron

public class PruebaFIFO {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("    PRUEBA DEL ALGORITMO FIFO - SIN RED");
        System.out.println("=================================================\n");

        // Crear el gestor de procesos
        ColaProceso cola = new ColaProceso();

        // Crear procesos basados en la imagen de ejemplo
        // Proceso(nombre, tiempoCPU, tiempoCreacion, tiempoLlegada)
        Proceso procesoA = new Proceso("A", 3, 0, 0);   // Llega en t=0
        Proceso procesoB = new Proceso("B", 6, 0, 1);   // Llega en t=1
        Proceso procesoC = new Proceso("C", 4, 0, 4);   // Llega en t=4
        Proceso procesoD = new Proceso("D", 5, 0, 6);   // Llega en t=6
        Proceso procesoE = new Proceso("E", 2, 0, 7);   // Llega en t=7
        Proceso procesoF = new Proceso("F", 9, 0, 8);   // Llega en t=8
        Proceso procesoG = new Proceso("G", 2, 0, 9);   // Llega en t=9

        // Agregar procesos a la cola en el orden que llegan
        System.out.println("AGREGANDO PROCESOS A LA COLA\n");
        cola.agregarProceso(procesoA);
        cola.agregarProceso(procesoB);
        cola.agregarProceso(procesoC);
        cola.agregarProceso(procesoD);
        cola.agregarProceso(procesoE);
        cola.agregarProceso(procesoF);
        cola.agregarProceso(procesoG);

        System.out.println("\n");
        cola.imprimirEstado();

        // Simular la ejecución FIFO
        System.out.println("\nEJECUTANDO PROCESOS EN ORDEN FIFO\n");

        while (!cola.estaVacia() || cola.hayProcesoEnEjecucion()) {
            if (!cola.hayProcesoEnEjecucion() && !cola.estaVacia()) {
                // Obtener el siguiente proceso
                Proceso siguiente = cola.obtenerSiguienteProceso();
                System.out.println(" >> Iniciando proceso: " + siguiente.getNombre());
            }

            if (cola.hayProcesoEnEjecucion()) {
                // Completar el proceso actual
                cola.completaProcesoActual();
            }
        }

        // Mostrar resultados finales
        System.out.println("\n=================================================");
        System.out.println("           RESULTADOS FINALES");
        System.out.println("=================================================\n");

        System.out.println("┌─────────┬──────┬──────────┬────────────┬──────────────┬───────────────┐");
        System.out.println("│ Proceso │  t   │  E       │  F         │  P           │  Estado       │");
        System.out.println("│         │ (CPU)│ (Espera) │ (Final)    │ (Penaliz.)   │               │");
        System.out.println("├─────────┼──────┼──────────┼────────────┼──────────────┼───────────────┤");

        for (Proceso p : cola.getTodosLosProcesos()) {
            System.out.printf("│   %-5s │  %-3d │    %-5d │    %-7d │    %-9.2f │  %-12s │%n",
                    p.getNombre(),
                    p.getTiempoCPU(),
                    p.getTiempoEspera(),
                    p.getTiempoFinalizacion(),
                    p.getPenalizacion(),
                    p.getEstado()
            );
        }

        System.out.println("└─────────┴──────┴──────────┴────────────┴──────────────┴───────────────┘");

        // Mostrar promedios
        System.out.println("\nMÉTRICAS PROMEDIO:");
        System.out.printf("   • Tiempo de Espera Promedio:     %.2f%n",
                cola.calcularTiempoEsperaPromedio());
        System.out.printf("   • Tiempo de Finalización Promedio: %.2f%n",
                cola.calcularTiempoFinalizacionPromedio());
        System.out.printf("   • Penalización Promedio:         %.2f%n",
                cola.calcularPenalizacionPromedio());

        System.out.println(" Prueba completada exitosamente");

        // Probar eliminación de proceso
        System.out.println("\nPRUEBA DE ELIMINACIÓN DE PROCESO\n");

        ColaProceso cola2 = new ColaProceso();
        Proceso p1 = new Proceso("P1", 5, 0, 0);
        Proceso p2 = new Proceso("P2", 3, 0, 0);
        Proceso p3 = new Proceso("P3", 7, 0, 0);

        cola2.agregarProceso(p1);
        cola2.agregarProceso(p2);
        cola2.agregarProceso(p3);

        System.out.println("\nAntes de eliminar:");
        cola2.imprimirEstado();

        // Eliminar P2
        cola2.eliminarProceso("P2");

        System.out.println("\nDespués de eliminar P2:");
        cola2.imprimirEstado();
    }
}
