package servidor.interfaz;

import javax.swing.SwingUtilities;

public class InterfazServidor {

    public procesos v1;
    public tirmpos vTiempos;
    public ColaVentana vColas;

    public InterfazServidor() {
        // El constructor ahora crea las 3 ventanas
        v1 = new procesos(20); // Ventana 1 con rango 20
        vTiempos = new tirmpos(); // Ventana de estadísticas
        vColas = new ColaVentana();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 1. Crear el servidor (y sus 3 ventanas)
            InterfazServidor servidor = new InterfazServidor();

            // --- NUEVO: Conectar las ventanas ---
            // Les decimos a v1 y v2 que reporten a vTiempos
            servidor.v1.setPanelTiempos(servidor.vTiempos);
            servidor.v1.setPanelColas(servidor.vColas);
            // --- FIN NUEVO ---

            // 2. Hacer visibles las 3 ventanas
            servidor.v1.setVisible(true);
            servidor.vTiempos.setVisible(true);
            servidor.vColas.setVisible(true);// <-- HACER VISIBLE

            // 3. Iniciar la simulación (este método no necesita cambiar)
            iniciarSimulacionDeLlegadas(servidor.v1);
        });
    }

    /**
     * Simula la llegada de tareas.
     * Este método NO NECESITA CAMBIOS, ya que registrarTareaCompleta
     * ahora usa la variable interna que ya asignamos.
     */
    private static void iniciarSimulacionDeLlegadas(procesos panel1) {

        Runnable simulacion = () -> {
            try {
                // --- Simulación para Ventana 1 ---
                Thread.sleep(500);
                panel1.registrarTareaCompleta("P-A (Win1)", 1, 3, 8);

                // --- Ventana 1 ---
                Thread.sleep(1000);
                panel1.registrarTareaCompleta("P-B (Win1)", 2, 5, 12);

                // --- Ventana 1 ---
                Thread.sleep(1000);
                panel1.registrarTareaCompleta("P-C (Win1)", 4, 9, 15);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        new Thread(simulacion).start();
    }
}