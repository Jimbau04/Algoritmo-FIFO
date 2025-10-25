package servidor.interfaz;

import javax.swing.*;
import java.awt.*;

public class ColaVentana extends JFrame {

    private DefaultListModel<String> modeloNuevo;
    private DefaultListModel<String> modeloListos;
    private DefaultListModel<String> modeloEjecutando;
    private DefaultListModel<String> modeloBloqueado;
    private DefaultListModel<String> modeloTerminados;

    public ColaVentana() {
        setTitle("Colas del Planificador (5 Estados)");
        setSize(850, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);

        setLayout(new GridLayout(1, 5, 10, 10));
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        modeloNuevo = new DefaultListModel<>();
        modeloListos = new DefaultListModel<>();
        modeloEjecutando = new DefaultListModel<>();
        modeloBloqueado = new DefaultListModel<>();
        modeloTerminados = new DefaultListModel<>();

        add(crearPanelLista("1. Nuevo", modeloNuevo, Color.decode("#E8F4F8")));
        add(crearPanelLista("2. Listo", modeloListos, Color.decode("#D5F4E6")));
        add(crearPanelLista("3. En Ejecución", modeloEjecutando, Color.decode("#FFF9C4")));
        add(crearPanelLista("4. En Espera", modeloBloqueado, Color.decode("#FFCCBC")));
        add(crearPanelLista("5. Terminado", modeloTerminados, Color.decode("#F3E5F5")));
    }

    private JPanel crearPanelLista(String titulo, DefaultListModel<String> modelo, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                titulo,
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 13)
        ));
        panel.setBackground(color);

        JList<String> lista = new JList<>(modelo);
        lista.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lista.setBackground(color);
        lista.setSelectionBackground(Color.decode("#BBDEFB"));

        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // --- MÉTODOS PÚBLICOS (Thread-Safe) ---

    public void agregarNuevo(String nombreProceso) {
        SwingUtilities.invokeLater(() -> {
            limpiarDeTodasLasColas(nombreProceso);
            if (!modeloNuevo.contains(nombreProceso)) {
                modeloNuevo.addElement(nombreProceso);
            }
        });
    }

    public void moverA_Listo(String nombreProceso) {
        SwingUtilities.invokeLater(() -> {
            limpiarDeTodasLasColas(nombreProceso);
            if (!modeloListos.contains(nombreProceso)) {
                modeloListos.addElement(nombreProceso);
            }
        });
    }

    public void moverA_Ejecucion(String nombreProceso) {
        SwingUtilities.invokeLater(() -> {
            limpiarDeTodasLasColas(nombreProceso);
            if (!modeloEjecutando.contains(nombreProceso)) {
                modeloEjecutando.addElement(nombreProceso);
            }
        });
    }

    public void moverA_Bloqueado(String nombreProceso) {
        SwingUtilities.invokeLater(() -> {
            limpiarDeTodasLasColas(nombreProceso);
            if (!modeloBloqueado.contains(nombreProceso)) {
                modeloBloqueado.addElement(nombreProceso);
            }
        });
    }

    public void moverA_Terminado(String nombreProceso) {
        SwingUtilities.invokeLater(() -> {
            limpiarDeTodasLasColas(nombreProceso);
            if (!modeloTerminados.contains(nombreProceso)) {
                modeloTerminados.addElement(nombreProceso);
            }
        });
    }

    // Ayudante para evitar duplicados
    private void limpiarDeTodasLasColas(String nombre) {
        // Limpiar también versiones con sufijos
        for (int i = modeloNuevo.size() - 1; i >= 0; i--) {
            if (modeloNuevo.get(i).startsWith(nombre)) {
                modeloNuevo.remove(i);
            }
        }
        for (int i = modeloListos.size() - 1; i >= 0; i--) {
            if (modeloListos.get(i).startsWith(nombre)) {
                modeloListos.remove(i);
            }
        }
        for (int i = modeloEjecutando.size() - 1; i >= 0; i--) {
            if (modeloEjecutando.get(i).startsWith(nombre)) {
                modeloEjecutando.remove(i);
            }
        }
        for (int i = modeloBloqueado.size() - 1; i >= 0; i--) {
            if (modeloBloqueado.get(i).startsWith(nombre)) {
                modeloBloqueado.remove(i);
            }
        }
        for (int i = modeloTerminados.size() - 1; i >= 0; i--) {
            if (modeloTerminados.get(i).startsWith(nombre)) {
                modeloTerminados.remove(i);
            }
        }
    }

    public void limpiarColas() {
        SwingUtilities.invokeLater(() -> {
            modeloNuevo.clear();
            modeloListos.clear();
            modeloEjecutando.clear();
            modeloBloqueado.clear();
            modeloTerminados.clear();
        });
    }
}