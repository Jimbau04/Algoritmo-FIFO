package servidor.interfaz;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class procesos extends JFrame {
    private JTable tabla;
    private JTable tablaProcesos;
    private DefaultTableModel modelo;
    private DefaultTableModel modeloProcesos;
    private Map<String, Color> coloresTareas;
    private int rangoPlanificador;
    private Random rand = new Random();

    public procesos(int rangoPlanificador) {
        this.rangoPlanificador = rangoPlanificador;
        setTitle("Vista del Planificador (Servidor) - Rango: " + rangoPlanificador);
        setSize(1200, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        coloresTareas = new HashMap<>();

        add(crearPanelSuperior(), BorderLayout.NORTH);
        add(crearPanelProcesos(), BorderLayout.WEST);
        add(crearPanelGantt(), BorderLayout.CENTER);
        setLocationRelativeTo(null);
    }

    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.decode("#E8F4F8"));
        JLabel titulo = new JLabel("DIAGRAMA DE GANTT - GESTIÓN DE PROYECTOS");
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        titulo.setForeground(Color.decode("#2C3E50"));
        panel.add(titulo);
        return panel;
    }

    private JPanel crearPanelGantt() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Procesos x Tiempo"));

        String[] columnas = new String[rangoPlanificador + 1];
        columnas[0] = "Tarea";
        for (int i = 1; i <= rangoPlanificador; i++) {
            columnas[i] = String.valueOf(i);
        }

        modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        tabla = new JTable(modelo);
        tabla.setRowHeight(35);
        tabla.setGridColor(Color.LIGHT_GRAY);
        tabla.setFont(new Font("Arial", Font.PLAIN, 12));
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        tabla.getColumnModel().getColumn(0).setPreferredWidth(120);
        for (int i = 1; i <= rangoPlanificador; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(35);
        }

        // Aplicar renderer a todas las columnas
        tabla.setDefaultRenderer(Object.class, new GanttRenderer());

        JScrollPane scrollPane = new JScrollPane(tabla);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout());
        JButton btnLimpiar = new JButton("Limpiar Tablas");
        btnLimpiar.addActionListener(e -> limpiarTablas());
        panelBotones.add(btnLimpiar);
        panel.add(panelBotones, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearPanelProcesos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Datos de Procesos"));
        panel.setPreferredSize(new Dimension(250, 0));

        String[] columnas = {"Proceso", "C", "t"};
        modeloProcesos = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tablaProcesos = new JTable(modeloProcesos);
        tablaProcesos.setRowHeight(30);
        tablaProcesos.getColumnModel().getColumn(0).setPreferredWidth(80);
        tablaProcesos.getColumnModel().getColumn(1).setPreferredWidth(50);
        tablaProcesos.getColumnModel().getColumn(2).setPreferredWidth(50);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < tablaProcesos.getColumnCount(); i++) {
            tablaProcesos.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scroll = new JScrollPane(tablaProcesos);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    public void limpiarTablas() {
        modelo.setRowCount(0);
        modeloProcesos.setRowCount(0);
        coloresTareas.clear();
    }

    public void registrarTareaCompleta(String nombre, int tiempoLlegada, int inicio, int fin) {
        SwingUtilities.invokeLater(() -> {
            int filaGantt = -1;
            for (int i = 0; i < modelo.getRowCount(); i++) {
                if (modelo.getValueAt(i, 0).equals(nombre)) {
                    filaGantt = i;
                    break;
                }
            }

            int duracion = (fin > 0 && inicio >= 0) ? (fin - inicio) + 1 : 0;
            if (duracion < 0) duracion = 0;

            if (filaGantt == -1) {
                Color color = new Color(rand.nextInt(200)+55, rand.nextInt(200)+55, rand.nextInt(200)+55);
                coloresTareas.put(nombre, color);

                Object[] fila = new Object[rangoPlanificador + 1];
                fila[0] = nombre;
                for (int i = 1; i <= rangoPlanificador; i++) fila[i] = "";

                if (tiempoLlegada >= 1 && tiempoLlegada <= rangoPlanificador) {
                    fila[tiempoLlegada] = "L"; // Pintar "Llegada"
                }
                modelo.addRow(fila);

                modeloProcesos.addRow(new Object[]{nombre, (inicio > 0) ? duracion : "?", tiempoLlegada});
            } else {
                // Actualizar Tabla Procesos (si ya existe)
                for (int i = 0; i < modeloProcesos.getRowCount(); i++) {
                    if (modeloProcesos.getValueAt(i, 0).equals(nombre)) {
                        modeloProcesos.setValueAt((inicio > 0) ? duracion : "?", i, 1);
                        modeloProcesos.setValueAt(tiempoLlegada, i, 2); // Actualizar llegada (por si re-planificó)
                        break;
                    }
                }
            }
        });
    }

    public void pintarTickEjecucion(String nombre, int tick) {
        SwingUtilities.invokeLater(() -> {
            if (tick < 1 || tick > rangoPlanificador) return;

            for (int i = 0; i < modelo.getRowCount(); i++) {
                if (modelo.getValueAt(i, 0).equals(nombre)) {
                    modelo.setValueAt("█", i, tick);
                    return;
                }
            }
        });
    }

    class GanttRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);

            if (column == 0) {
                c.setBackground(isSelected ? Color.LIGHT_GRAY : Color.WHITE);
                c.setForeground(Color.BLACK);
                setHorizontalAlignment(LEFT);
                setText(value.toString());
                return c;
            }

            String nombreTarea = (String) modelo.getValueAt(row, 0);
            Color colorTarea = coloresTareas.get(nombreTarea);
            String sValue = (value == null) ? "" : value.toString();

            if (sValue.equals("█")) {
                c.setBackground(colorTarea != null ? colorTarea : Color.GRAY);
                c.setForeground(colorTarea != null ? colorTarea : Color.GRAY);
                setText("");
            } else if (sValue.equals("L")) {
                c.setBackground(isSelected ? Color.LIGHT_GRAY : Color.WHITE);
                c.setForeground(Color.BLACK);
                setText(" L");
                setHorizontalAlignment(CENTER);
            } else {
                c.setBackground(isSelected ? Color.LIGHT_GRAY : Color.WHITE);
                c.setForeground(Color.BLACK);
                setText("");
            }

            return c;
        }
    }
}