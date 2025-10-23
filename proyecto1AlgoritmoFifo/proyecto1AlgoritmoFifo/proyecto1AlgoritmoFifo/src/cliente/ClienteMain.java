package cliente;
import cliente.Interfaz.ClienteGUI;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada principal para la aplicación cliente
 */
public class ClienteMain {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════");
        System.out.println("  CLIENTE PLANIFICADOR FIFO");
        System.out.println("═══════════════════════════════════════");
        System.out.println();

        // Configurar Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("⚠ No se pudo configurar el Look and Feel del sistema");
        }

        // Iniciar la GUI
        SwingUtilities.invokeLater(() -> {
            ClienteGUI gui = new ClienteGUI();
            gui.setVisible(true);
        });
    }
}
