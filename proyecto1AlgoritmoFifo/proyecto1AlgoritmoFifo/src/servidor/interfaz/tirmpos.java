package servidor.interfaz;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class tirmpos extends JFrame {

    private DefaultTableModel modeloEspera;
    private DefaultTableModel modeloFinalizacion;
    private DefaultTableModel modeloPenalizacion;

    public tirmpos() {
        setTitle("Estadísticas de Simulación");
        setSize(900, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);

        Container c = getContentPane();
        c.setLayout(new GridLayout(1, 3, 15, 10));
        ((JPanel) c).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        c.setBackground(Color.decode("#E8F4F8"));

        c.add(crearPanelTiempoEspera());
        c.add(crearPanelTiempoFinalizacion());
        c.add(crearPanelPenalizacion());

        limpiarTablas();
    }

    private JPanel crearPanelTiempoEspera() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.decode("#E8F4F8"));
        JLabel titulo = new JLabel("Tiempo de Espera", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 14));
        titulo.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        panel.add(titulo, BorderLayout.NORTH);
        String[] columnas = {"", ""};
        modeloEspera = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
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
            @Override public boolean isCellEditable(int row, int column) { return false; }
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
            @Override public boolean isCellEditable(int row, int column) { return false; }
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
        actualizarValorTabla(modeloEspera, proceso, valor);
    }

    public void actualizarTiempoFinalizacion(String proceso, Object valor) {
        actualizarValorTabla(modeloFinalizacion, proceso, valor);
    }

    public void actualizarPenalizacion(String proceso, Object valor) {
        actualizarValorTabla(modeloPenalizacion, proceso, valor);
    }

    private void actualizarValorTabla(DefaultTableModel modelo, String proceso, Object valor) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < modelo.getRowCount() - 1; i++) {
                if (modelo.getValueAt(i, 0).equals(proceso)) {
                    modelo.setValueAt(valor, i, 1);
                    return;
                }
            }
        });
    }

    public void agregarProceso(String nombreProceso) {
        SwingUtilities.invokeLater(() -> {
            int ultimaFila = modeloEspera.getRowCount() - 1;
            if (ultimaFila < 0) ultimaFila = 0;
            modeloEspera.insertRow(ultimaFila, new Object[]{nombreProceso, 0});
            modeloFinalizacion.insertRow(ultimaFila, new Object[]{nombreProceso, 0});
            modeloPenalizacion.insertRow(ultimaFila, new Object[]{"0.00"});
        });
    }

    public void eliminarProceso(String nombreProceso) {
        SwingUtilities.invokeLater(() -> {
            eliminarFilaPorProceso(modeloEspera, nombreProceso);
            eliminarFilaPorProceso(modeloFinalizacion, nombreProceso);
            eliminarFilaPorProceso(modeloPenalizacion, nombreProceso);
        });
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
        SwingUtilities.invokeLater(() -> {
            calcularMedia(modeloEspera, "%.2f");
            calcularMedia(modeloFinalizacion, "%.1f");
            calcularMedia(modeloPenalizacion, "%.2f");
        });
    }

    private void calcularMedia(DefaultTableModel modelo, String formato) {
        double suma = 0;
        int contador = 0;
        for (int i = 0; i < modelo.getRowCount() - 1; i++) {
            Object valor = modelo.getValueAt(i, 1);
            try {
                suma += Double.parseDouble(String.valueOf(valor));
                contador++;
            } catch (NumberFormatException e) {
                // Ignorar valores no numéricos como "ESPERA"
            }
        }
        if (contador > 0 && modelo.getRowCount() > 0) {
            double media = suma / contador;
            modelo.setValueAt(String.format(formato, media),
                    modelo.getRowCount() - 1, 1);
        }
    }


    public void limpiarTablas() {
        SwingUtilities.invokeLater(() -> {
            modeloEspera.setRowCount(0);
            modeloFinalizacion.setRowCount(0);
            modeloPenalizacion.setRowCount(0);
            modeloEspera.addRow(new Object[]{"media", "0.00"});
            modeloFinalizacion.addRow(new Object[]{"media", "0.0"});
            modeloPenalizacion.addRow(new Object[]{"media", "0.00"});
        });
    }
}