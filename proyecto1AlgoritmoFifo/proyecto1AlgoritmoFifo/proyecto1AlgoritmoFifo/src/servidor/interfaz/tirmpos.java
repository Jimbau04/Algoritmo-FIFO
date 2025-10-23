package servidor.interfaz;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

// --- CAMBIO PRINCIPAL: Ahora es un JFrame ---
public class tirmpos extends JFrame {

    private DefaultTableModel modeloEspera;
    private DefaultTableModel modeloFinalizacion;
    private DefaultTableModel modeloPenalizacion;

    public tirmpos() {
        // --- NUEVO: Configuración de la ventana ---
        setTitle("Estadísticas de Simulación");
        setSize(900, 300); // Tamaño para la ventana
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Que no cierre todo
        setLocationByPlatform(true); // Dejar que el SO la acomode

        // --- Lógica anterior (ahora se aplica al content pane) ---
        // Usamos el content pane para añadir los paneles
        Container c = getContentPane();
        c.setLayout(new GridLayout(1, 3, 15, 10));
        ((JPanel) c).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        c.setBackground(Color.decode("#E8F4F8"));

        // Crear las tres tablas
        c.add(crearPanelTiempoEspera());
        c.add(crearPanelTiempoFinalizacion());
        c.add(crearPanelPenalizacion());

        // Iniciar vacío
        limpiarTablas();
    }

    // ... (Todos los demás métodos: crearPanelTiempoEspera, crearPanelTiempoFinalizacion,
    //      crearPanelPenalizacion, configurarTabla, actualizarTiempoEspera,
    //      actualizarTiempoFinalizacion, actualizarPenalizacion, agregarProceso,
    //      eliminarProceso, calcularYActualizarMedias, limpiarTablas...
    //      ...PERMANECEN EXACTAMENTE IGUALES) ...

    private JPanel crearPanelTiempoEspera() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.decode("#E8F4F8"));
        JLabel titulo = new JLabel("Tiempo de Espera", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 14));
        titulo.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        panel.add(titulo, BorderLayout.NORTH);
        String[] columnas = {"", ""};
        modeloEspera = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tablaTiempoEspera = new JTable(modeloEspera);
        configurarTabla(tablaTiempoEspera);
        JScrollPane scroll = new JScrollPane(tablaTiempoEspera);
        scroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelTiempoFinalizacion() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.decode("#E8F4F8"));
        JLabel titulo = new JLabel("Tiempo de Finalización", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 14));
        titulo.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        panel.add(titulo, BorderLayout.NORTH);
        String[] columnas = {"", ""};
        modeloFinalizacion = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tablaTiempoFinalizacion = new JTable(modeloFinalizacion);
        configurarTabla(tablaTiempoFinalizacion);
        JScrollPane scroll = new JScrollPane(tablaTiempoFinalizacion);
        scroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelPenalizacion() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.decode("#E8F4F8"));
        JLabel titulo = new JLabel("Penalización", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 14));
        titulo.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        panel.add(titulo, BorderLayout.NORTH);
        String[] columnas = {"", ""};
        modeloPenalizacion = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tablaPenalizacion = new JTable(modeloPenalizacion);
        configurarTabla(tablaPenalizacion);
        JScrollPane scroll = new JScrollPane(tablaPenalizacion);
        scroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void configurarTabla(JTable tabla) {
        tabla.setRowHeight(25);
        tabla.setFont(new Font("Arial", Font.PLAIN, 12));
        tabla.setGridColor(Color.LIGHT_GRAY);
        tabla.getTableHeader().setVisible(false);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(40);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(60);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tabla.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tabla.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
    }

    public void actualizarTiempoEspera(String proceso, Object valor) {
        for (int i = 0; i < modeloEspera.getRowCount() - 1; i++) {
            if (modeloEspera.getValueAt(i, 0).equals(proceso)) {
                modeloEspera.setValueAt(valor, i, 1);
                return;
            }
        }
    }

    public void actualizarTiempoFinalizacion(String proceso, Object valor) {
        for (int i = 0; i < modeloFinalizacion.getRowCount() - 1; i++) {
            if (modeloFinalizacion.getValueAt(i, 0).equals(proceso)) {
                modeloFinalizacion.setValueAt(valor, i, 1);
                return;
            }
        }
    }

    public void actualizarPenalizacion(String proceso, Object valor) {
        for (int i = 0; i < modeloPenalizacion.getRowCount() - 1; i++) {
            if (modeloPenalizacion.getValueAt(i, 0).equals(proceso)) {
                modeloPenalizacion.setValueAt(valor, i, 1);
                return;
            }
        }
    }

    public void agregarProceso(String nombreProceso) {
        int ultimaFila = modeloEspera.getRowCount() - 1;
        if (ultimaFila < 0) ultimaFila = 0;
        modeloEspera.insertRow(ultimaFila, new Object[]{nombreProceso, 0});
        modeloFinalizacion.insertRow(ultimaFila, new Object[]{nombreProceso, 0});
        modeloPenalizacion.insertRow(ultimaFila, new Object[]{nombreProceso, "0.00"});
    }

    public void eliminarProceso(String nombreProceso) {
        eliminarFilaPorProceso(modeloEspera, nombreProceso);
        eliminarFilaPorProceso(modeloFinalizacion, nombreProceso);
        eliminarFilaPorProceso(modeloPenalizacion, nombreProceso);
    }

    private void eliminarFilaPorProceso(DefaultTableModel modelo, String proceso) {
        for (int i = 0; i < modelo.getRowCount() - 1; i++) {
            if (modelo.getValueAt(i, 0).equals(proceso)) {
                modelo.removeRow(i);
                return;
            }
        }
    }

    public void calcularYActualizarMedias() {
        // Calcular media de Tiempo de Espera
        double sumaEspera = 0;
        int contadorEspera = 0;
        for (int i = 0; i < modeloEspera.getRowCount() - 1; i++) {
            Object valor = modeloEspera.getValueAt(i, 1);
            if (valor instanceof Number) {
                sumaEspera += ((Number) valor).doubleValue();
                contadorEspera++;
            }
        }
        if (contadorEspera > 0 && modeloEspera.getRowCount() > 0) {
            double mediaEspera = sumaEspera / contadorEspera;
            modeloEspera.setValueAt(String.format("%.2f", mediaEspera),
                    modeloEspera.getRowCount() - 1, 1);
        }

        // Calcular media de Tiempo de Finalización
        double sumaFin = 0;
        int contadorFin = 0;
        for (int i = 0; i < modeloFinalizacion.getRowCount() - 1; i++) {
            Object valor = modeloFinalizacion.getValueAt(i, 1);
            if (valor instanceof Number) {
                sumaFin += ((Number) valor).doubleValue();
                contadorFin++;
            }
        }
        if (contadorFin > 0 && modeloFinalizacion.getRowCount() > 0) {
            double mediaFin = sumaFin / contadorFin;
            modeloFinalizacion.setValueAt(String.format("%.1f", mediaFin),
                    modeloFinalizacion.getRowCount() - 1, 1);
        }

        // Calcular media de Penalización
        double sumaPen = 0;
        int contadorPen = 0;
        for (int i = 0; i < modeloPenalizacion.getRowCount() - 1; i++) {
            Object valor = modeloPenalizacion.getValueAt(i, 1);
            try {
                sumaPen += Double.parseDouble(String.valueOf(valor));
                contadorPen++;
            } catch (NumberFormatException e) {
                // Ignorar
            }
        }
        if (contadorPen > 0 && modeloPenalizacion.getRowCount() > 0) {
            double mediaPen = sumaPen / contadorPen;
            modeloPenalizacion.setValueAt(String.format("%.2f", mediaPen),
                    modeloPenalizacion.getRowCount() - 1, 1);
        }
    }

    public void limpiarTablas() {
        modeloEspera.setRowCount(0);
        modeloFinalizacion.setRowCount(0);
        modeloPenalizacion.setRowCount(0);
        modeloEspera.addRow(new Object[]{"media", "0.00"});
        modeloFinalizacion.addRow(new Object[]{"media", "0.0"});
        modeloPenalizacion.addRow(new Object[]{"media", "0.00"});
    }

    // Método de prueba (sigue funcionando)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tablas de Estadísticas");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 400);

            tirmpos tablas = new tirmpos();
            // frame.add(tablas); // Ya no se añade, ¡es la ventana!
            tablas.setVisible(true); // Se hace visible ella misma

            // frame.setLocationRelativeTo(null); // Ya lo hace tirmpos
            // frame.setVisible(true); // Ya lo hace tirmpos
        });
    }
}