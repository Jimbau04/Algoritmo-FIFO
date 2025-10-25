package cliente;

import cliente.Interfaz.ClienteGUI;
import javax.swing.SwingUtilities;

public class MainCliente {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteGUI cliente = new ClienteGUI();
            cliente.setVisible(true);
        });
    }
}
