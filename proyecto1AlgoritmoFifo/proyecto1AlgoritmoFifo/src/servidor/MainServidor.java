package servidor;

import servidor.interfaz.ColaVentana;
import servidor.interfaz.procesos;
import servidor.interfaz.tirmpos;
import util.Constantes;
import javax.swing.SwingUtilities;

public class MainServidor {

    public static void main(String[] args) {

        ServidorFIFO servidor = new ServidorFIFO(
                Constantes.PUERTO_SERVIDOR,
                Constantes.RANGO_PLANIFICADOR
        );

        SwingUtilities.invokeLater(() -> {
            procesos guiProcesos = new procesos(Constantes.RANGO_PLANIFICADOR);
            tirmpos guiTiempos = new tirmpos();
            ColaVentana guiColas = new ColaVentana();

            servidor.setInterfaces(guiProcesos, guiTiempos, guiColas);

            guiProcesos.setVisible(true);
            guiTiempos.setVisible(true);
            guiColas.setVisible(true);
        });

        try {
            servidor.iniciar();
        } catch (Exception e) {
            System.err.println("✗ No se pudo iniciar el servidor: " + e.getMessage());
        }
    }
}
