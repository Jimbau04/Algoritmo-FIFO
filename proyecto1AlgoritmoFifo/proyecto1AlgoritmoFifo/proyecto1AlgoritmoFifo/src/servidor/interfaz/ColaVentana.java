package servidor.interfaz;

import javax.swing.*;
import java.awt.*;

public class ColaVentana extends JFrame {

    // --- 1. CREAR MODELOS PARA LOS 5 ESTADOS ---
    private DefaultListModel<String> modeloNuevo;
    private DefaultListModel<String> modeloListos;
    private DefaultListModel<String> modeloEjecutando;
    private DefaultListModel<String> modeloBloqueado;
    private DefaultListModel<String> modeloTerminados;

    public ColaVentana() {
        setTitle("Colas del Planificador (5 Estados)");
        setSize(800, 400); // Aumentamos el ancho para 5 columnas
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);

        // --- 2. CAMBIAR LAYOUT A 5 COLUMNAS ---
        setLayout(new GridLayout(1, 5, 10, 10));
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Inicializar modelos
        modeloNuevo = new DefaultListModel<>();
        modeloListos = new DefaultListModel<>();
        modeloEjecutando = new DefaultListModel<>();
        modeloBloqueado = new DefaultListModel<>(); // Modelo para la columna vacía
        modeloTerminados = new DefaultListModel<>();

        // --- 3. AÑADIR LOS 5 PANELES ---
        add(crearPanelLista("1. Nuevo", modeloNuevo));
        add(crearPanelLista("2. Listo", modeloListos));
        add(crearPanelLista("3. En Ejecución", modeloEjecutando));
        add(crearPanelLista("4. Bloqueado", modeloBloqueado)); // Añadido
        add(crearPanelLista("5. Terminado", modeloTerminados));
    }

    /**
     * Método de ayuda (no cambia)
     */
    private JPanel crearPanelLista(String titulo, DefaultListModel<String> modelo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(titulo));

        JList<String> lista = new JList<>(modelo);
        lista.setFont(new Font("Arial", Font.PLAIN, 14));

        panel.add(new JScrollPane(lista), BorderLayout.CENTER);
        return panel;
    }

    // --- 4. NUEVOS MÉTODOS PÚBLICOS (Thread-Safe) ---

    /**
     * Añade un proceso a la cola de "Nuevo".
     */
    public void agregarNuevo(String nombreProceso) {
        SwingUtilities.invokeLater(() -> {
            if (!modeloNuevo.contains(nombreProceso)) {
                modeloNuevo.addElement(nombreProceso);
            }
        });
    }

    /**
     * Mueve un proceso de "Nuevo" a "Listo".
     */
    public void moverA_Listo(String nombreProceso) {
        SwingUtilities.invokeLater(() -> {
            modeloNuevo.removeElement(nombreProceso);
            if (!modeloListos.contains(nombreProceso)) {
                modeloListos.addElement(nombreProceso);
            }
        });
    }

    /**
     * Mueve un proceso de "Listo" a "En Ejecución".
     * (Este método ya existía, pero ahora es más explícito)
     */
    public void moverA_Ejecucion(String nombreProceso) {
        SwingUtilities.invokeLater(() -> {
            modeloListos.removeElement(nombreProceso);
            if (!modeloEjecutando.contains(nombreProceso)) {
                modeloEjecutando.addElement(nombreProceso);
            }
        });
    }

    /**
     * Mueve un proceso de "En Ejecución" a "Terminado".
     * (Este método ya existía)
     */
    public void moverA_Terminado(String nombreProceso) {
        SwingUtilities.invokeLater(() -> {
            modeloEjecutando.removeElement(nombreProceso);
            if (!modeloTerminados.contains(nombreProceso)) {
                modeloTerminados.addElement(nombreProceso);
            }
        });
    }

    // (Estos métodos existirían si tuviéramos lógica de bloqueo)
    // public void moverA_Bloqueado(String nombreProceso) { ... }
    // public void moverDe_Bloqueado_A_Listo(String nombreProceso) { ... }


    /**
     * Limpia todas las colas.
     */
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
