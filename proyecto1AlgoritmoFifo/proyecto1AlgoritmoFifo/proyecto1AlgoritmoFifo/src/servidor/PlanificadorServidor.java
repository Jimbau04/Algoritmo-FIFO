package servidor;

import servidor.interfaz.InterfazServidor;
import javax.swing.SwingUtilities;

/**
 * ARCHIVO: servidor/PlanificadorServidor.java
 *
 * Clase principal que:
 * 1. Inicia el servidor XML-RPC
 * 2. Inicia la interfaz gráfica del servidor
 * 3. Conecta ambos componentes
 *
 * ESTE ES EL PUNTO DE ENTRADA DEL SERVIDOR
 */
public class PlanificadorServidor {

    private ServidorFIFO servidorRPC;
    private InterfazServidor interfazGrafica;
    private static final int PUERTO_DEFECTO = 8080;

    public PlanificadorServidor(int puerto) {
        try {
            // 1. Crear el servidor XML-RPC
            servidorRPC = new ServidorFIFO(puerto);

            // 2. Iniciar el servidor
            servidorRPC.iniciar();

            // 3. Crear la interfaz gráfica
            SwingUtilities.invokeLater(() -> {
                interfazGrafica = new InterfazServidor();

                // 4. Hacer visibles las ventanas
                interfazGrafica.v1.setVisible(true);
                interfazGrafica.vTiempos.setVisible(true);
                interfazGrafica.vColas.setVisible(true);
            });

            System.out.println("═══════════════════════════════════════");
            System.out.println("  SERVIDOR PLANIFICADOR FIFO INICIADO");
            System.out.println("  Puerto: " + puerto);
            System.out.println("═══════════════════════════════════════");

        } catch (Exception e) {
            System.err.println("✗ Error al iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Punto de entrada principal del servidor
     */
    public static void main(String[] args) {
        int puerto = PUERTO_DEFECTO;

        // Permitir especificar el puerto por argumentos
        if (args.length > 0) {
            try {
                puerto = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Puerto inválido, usando puerto por defecto: " + PUERTO_DEFECTO);
            }
        }

        new PlanificadorServidor(puerto);
    }
}