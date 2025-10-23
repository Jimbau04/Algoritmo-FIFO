import cliente.ClienteFIFO;
import util.Constantes;

import java.util.Map;
import java.util.Scanner;

/**
 * Clase de prueba para verificar la comunicación cliente-servidor
 */
public class PruebaClienteServidor {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("═══════════════════════════════════════");
        System.out.println("  PRUEBA CLIENTE-SERVIDOR XML-RPC");
        System.out.println("═══════════════════════════════════════\n");

        // 1. Crear cliente con ID único
        String clienteId = "Cliente-" + System.currentTimeMillis();
        ClienteFIFO cliente = new ClienteFIFO(Constantes.SERVIDOR_URL, clienteId);

        // 2. Conectar al servidor
        System.out.println("Presiona ENTER para conectar al servidor...");
        scanner.nextLine();

        if (!cliente.conectar()) {
            System.out.println("✗ No se pudo conectar. ¿Está el servidor ejecutándose?");
            return;
        }

        // 3. Agregar algunos procesos
        System.out.println("\n--- AGREGANDO PROCESOS ---");
        System.out.println("Presiona ENTER para continuar...");
        scanner.nextLine();

        cliente.agregarProceso("Proceso-A", 3);
        esperar(500);

        cliente.agregarProceso("Proceso-B", 5);
        esperar(500);

        cliente.agregarProceso("Proceso-C", 2);
        esperar(500);

        // 4. Obtener estado de un proceso
        System.out.println("\n--- CONSULTANDO ESTADO ---");
        System.out.println("Presiona ENTER para continuar...");
        scanner.nextLine();

        Map<String, Object> estado = cliente.obtenerEstadoProceso("Proceso-A");
        if (!estado.isEmpty()) {
            System.out.println("Estado del Proceso-A:");
            estado.forEach((key, value) -> System.out.println("  " + key + ": " + value));
        }

        // 5. Listar todos los procesos
        System.out.println("\n--- LISTANDO TODOS LOS PROCESOS ---");
        System.out.println("Presiona ENTER para continuar...");
        scanner.nextLine();

        Object[] procesos = cliente.obtenerTodosLosProcesos();
        System.out.println("Total de procesos: " + procesos.length);

        for (int i = 0; i < procesos.length; i++) {
            System.out.println("\nProceso " + (i + 1) + ":");
            @SuppressWarnings("unchecked")
            Map<String, Object> proceso = (Map<String, Object>) procesos[i];
            proceso.forEach((key, value) -> System.out.println("  " + key + ": " + value));
        }

        // 6. Esperar para ver la ejecución
        System.out.println("\n--- ESPERANDO EJECUCIÓN ---");
        System.out.println("El servidor está ejecutando los procesos...");
        System.out.println("Presiona ENTER para continuar consultando el estado...");
        scanner.nextLine();

        // Consultar estado varias veces
        for (int i = 0; i < 5; i++) {
            System.out.println("\nConsulta " + (i + 1) + ":");
            Map<String, Object> estadoActual = cliente.obtenerEstadoProceso("Proceso-A");
            if (!estadoActual.isEmpty()) {
                System.out.println("  Estado: " + estadoActual.get("estado"));
                System.out.println("  Tiempo Espera: " + estadoActual.get("tiempoEspera"));
                System.out.println("  Tiempo Finalización: " + estadoActual.get("tiempoFinalizacion"));
            }
            esperar(2000);
        }

        // 7. Intentar eliminar un proceso
        System.out.println("\n--- INTENTANDO ELIMINAR PROCESO ---");
        System.out.println("Presiona ENTER para continuar...");
        scanner.nextLine();

        boolean eliminado = cliente.eliminarProceso("Proceso-B");
        System.out.println("Resultado: " + (eliminado ? "Eliminado" : "No se pudo eliminar"));

        // 8. Desconectar
        System.out.println("\n--- DESCONECTANDO ---");
        System.out.println("Presiona ENTER para desconectar...");
        scanner.nextLine();

        cliente.desconectar();

        System.out.println("\n✓ Prueba completada");
        scanner.close();
    }

    private static void esperar(int milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}