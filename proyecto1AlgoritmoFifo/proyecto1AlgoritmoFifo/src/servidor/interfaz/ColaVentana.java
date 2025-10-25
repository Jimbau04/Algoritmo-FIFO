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
        setSize(800, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);

        setLayout(new GridLayout(1, 5, 10, 10));
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        modeloNuevo = new DefaultListModel<>();
        modeloListos = new DefaultListModel<>();
        modeloEjecutando = new DefaultListModel<>();
        modeloBloqueado = new DefaultListModel<>();
        modeloTerminados = new DefaultListModel<>();

        add(crearPanelLista("1. Nuevo", modeloNuevo));
        add(crearPanelLista("2. Listo", modeloListos));
        add(crearPanelLista("3. En Ejecución", modeloEjecutando));
        add(crearPanelLista("4. Bloqueado", modeloBloqueado));
        add(crearPanelLista("5. Terminado", modeloTerminados));
    }

    private JPanel crearPanelLista(String titulo, DefaultListModel<String> modelo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(titulo));
        JList<String> lista = new JList<>(modelo);
        lista.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(new JScrollPane(lista), BorderLayout.CENTER);
        return panel;
    }

    // --- MÉTODOS PÚBLICOS (Thread-Safe) ---

    public void agregarNuevo(String nombreProceso) {
        SwingUtilities.invokeLater(() -> {
            limpiarDeTodasLasColas(nombreProceso); // Limpiar por si acaso
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
        modeloNuevo.removeElement(nombre);
        modeloListos.removeElement(nombre);
        modeloEjecutando.removeElement(nombre);
        modeloBloqueado.removeElement(nombre);
        modeloTerminados.removeElement(nombre);
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