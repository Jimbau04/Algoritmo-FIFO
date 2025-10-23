package cliente.Interfaz; // Asegúrate de que el paquete sea correcto

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ClienteGUI extends JFrame {

    private JTable tablaProcesos;
    private DefaultTableModel modeloProcesos;
    private JButton btnConnect, btnDisconnect, btnAddProcess, btnKillProcess;
    private JLabel statusLabel;

    public ClienteGUI() {
        setTitle("Cliente Planificador");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        // Añadir un borde vacío para que no esté pegado a los bordes
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Panel Superior (Conexión)
        add(crearPanelConexion(), BorderLayout.NORTH);

        // 2. Panel Central (Tabla de Procesos)
        add(crearPanelTabla(), BorderLayout.CENTER);

        // 3. Panel Inferior (Acciones y Estado)
        add(crearPanelAcciones(), BorderLayout.SOUTH);

        setLocationRelativeTo(null);

        // Añadir los listeners a los botones
        agregarListeners();
    }

    /**
     * Crea el panel superior con los botones de Conectar/Desconectar
     */
    private JPanel crearPanelConexion() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Conexión con el Servidor"));

        btnConnect = new JButton("Conectar");
        btnDisconnect = new JButton("Desconectar");
        btnDisconnect.setEnabled(false); // Empieza desconectado

        panel.add(btnConnect);
        panel.add(btnDisconnect);
        return panel;
    }

    /**
     * Crea el panel central con la tabla dinámica de procesos
     */
    private JScrollPane crearPanelTabla() {
        String[] columnas = {
                "ID Proceso", "Estado", "T. Ejecución (C)",
                "T. Espera", "T. Finalización", "T. Penalización"
        };

        modeloProcesos = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Hace que la tabla no sea editable
            }
        };

        tablaProcesos = new JTable(modeloProcesos);
        tablaProcesos.setRowHeight(25);
        tablaProcesos.setFont(new Font("Arial", Font.PLAIN, 14));
        tablaProcesos.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        tablaProcesos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        return new JScrollPane(tablaProcesos);
    }

    /**
     * Crea el panel inferior con botones de acción y la barra de estado
     */
    private JPanel crearPanelAcciones() {
        // Panel contenedor principal
        JPanel panel = new JPanel(new BorderLayout());

        // Sub-panel para los botones de Agregar/Eliminar
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelBotones.setBorder(BorderFactory.createTitledBorder("Acciones de Proceso"));

        btnAddProcess = new JButton("Agregar Nuevo Proceso");
        btnKillProcess = new JButton("Eliminar Proceso Seleccionado");

        panelBotones.add(btnAddProcess);
        panelBotones.add(btnKillProcess);

        panel.add(panelBotones, BorderLayout.NORTH);

        // Sub-panel para la barra de estado (lo que pediste como "tabla")
        statusLabel = new JLabel("Estado: Desconectado", SwingConstants.LEFT);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setBorder(BorderFactory.createEtchedBorder());
        statusLabel.setOpaque(true); // Necesario para que el fondo se vea

        // Estado inicial (Rojo)
        statusLabel.setBackground(Color.decode("#E74C3C"));
        statusLabel.setForeground(Color.WHITE);

        panel.add(statusLabel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Centraliza todos los ActionListeners
     */
    private void agregarListeners() {

        // --- Listener de Conexión ---
        btnConnect.addActionListener(e -> {
            // --- AQUÍ VA TU LÓGICA DE RED PARA CONECTAR ---
            // Ejemplo: if (miClienteRMI.conectar()) {
            System.out.println("Intentando conectar...");
            // Si tiene éxito:
            actualizarEstadoConexion("Conectado", Color.decode("#2ECC71"));
            btnConnect.setEnabled(false);
            btnDisconnect.setEnabled(true);
            // } else {
            //   actualizarEstadoConexion("Error de Conexión", Color.ORANGE);
            // }
        });

        // --- Listener de Desconexión ---
        btnDisconnect.addActionListener(e -> {
            // --- AQUÍ VA TU LÓGICA DE RED PARA DESCONECTAR ---
            System.out.println("Desconectando...");
            actualizarEstadoConexion("Desconectado", Color.decode("#E74C3C"));
            btnConnect.setEnabled(true);
            btnDisconnect.setEnabled(false);
        });

        // --- Listener para Agregar Proceso ---
        btnAddProcess.addActionListener(e -> {
            mostrarDialogoNuevoProceso();
        });

        // --- Listener para Eliminar Proceso ---
        btnKillProcess.addActionListener(e -> {
            int filaSeleccionada = tablaProcesos.getSelectedRow();

            if (filaSeleccionada == -1) {
                JOptionPane.showMessageDialog(this,
                        "Por favor, selecciona un proceso de la tabla para eliminar.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String idProceso = (String) modeloProcesos.getValueAt(filaSeleccionada, 0);

            // Confirmación
            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Estás seguro de que quieres eliminar el proceso '" + idProceso + "'?",
                    "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                // --- AQUÍ VA TU LÓGICA DE RED PARA ENVIAR "ELIMINAR(idProceso)" AL SERVIDOR ---
                System.out.println("Enviando solicitud para eliminar: " + idProceso);
                // El servidor debería responder con el nuevo estado "Eliminado"
                // y tu hilo de red llamaría a:
                agregarOActualizarProceso(idProceso, "Eliminado", "0", "0", "N/A", "N/A");
            }
        });
    }

    /**
     * Muestra un diálogo para que el usuario ingrese los datos del nuevo proceso.
     */
    private void mostrarDialogoNuevoProceso() {
        // Campos de texto para el diálogo
        JTextField txtId = new JTextField(10);
        JTextField txtTiempoEjecucion = new JTextField(5);

        // Panel contenedor del diálogo
        JPanel panelDialog = new JPanel(new GridLayout(0, 2, 5, 5));
        panelDialog.add(new JLabel("ID del Proceso:"));
        panelDialog.add(txtId);
        panelDialog.add(new JLabel("Tiempo de Ejecución (C):"));
        panelDialog.add(txtTiempoEjecucion);

        int result = JOptionPane.showConfirmDialog(this, panelDialog,
                "Agregar Nuevo Proceso", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String id = txtId.getText().trim();
            String tEjec = txtTiempoEjecucion.getText().trim();

            if (id.isEmpty() || tEjec.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ambos campos son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Validar que el tiempo sea un número
                Integer.parseInt(tEjec);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "El tiempo de ejecución debe ser un número.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // --- AQUÍ VA TU LÓGICA DE RED PARA ENVIAR EL NUEVO PROCESO AL SERVIDOR ---
            System.out.println("Enviando nuevo proceso al servidor: ID=" + id + ", C=" + tEjec);

            // Como feedback inmediato, lo agregamos localmente como "Creado"
            // El servidor luego confirmará la "Llegada al Servidor"
            agregarOActualizarProceso(id, "Creado", tEjec, "0", "N/A", "N/A");
        }
    }

    // --- MÉTODOS PÚBLICOS PARA CONTROLAR LA GUI DESDE EL EXTERIOR (ej. Hilos de Red) ---
    // ¡Estos son los métodos más importantes para tu lógica de red!

    /**
     * Actualiza la barra de estado. Es Thread-Safe.
     */
    public void actualizarEstadoConexion(String texto, Color colorFondo) {
        // Asegura que la actualización de la GUI se haga en el hilo de Swing
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Estado: " + texto);
            statusLabel.setBackground(colorFondo);
        });
    }

    /**
     * Añade una nueva fila o actualiza una existente. Es Thread-Safe.
     * Esta es la función clave que tu hilo de red debe llamar.
     *
     * @param id El ID único del proceso
     * @param estado (Creado, Llegada al Servidor, En Ejecución, Finalizado, Eliminado)
     * @param tEjec Tiempo de Ejecución (C)
     * @param tEspera Tiempo de Espera
     * @param tFin Tiempo de Finalización
     * @param tPenal Tiempo de Penalización
     */
    public void agregarOActualizarProceso(String id, String estado, String tEjec, String tEspera, String tFin, String tPenal) {
        SwingUtilities.invokeLater(() -> {
            // 1. Buscar si el proceso ya existe en la tabla
            int fila = buscarFilaPorID(id);

            if (fila == -1) {
                // 2. Si no existe, añadirlo como nueva fila
                Object[] nuevaFila = {id, estado, tEjec, tEspera, tFin, tPenal};
                modeloProcesos.addRow(nuevaFila);
            } else {
                // 3. Si existe, actualizar los datos de esa fila
                modeloProcesos.setValueAt(estado, fila, 1);
                modeloProcesos.setValueAt(tEjec, fila, 2);
                modeloProcesos.setValueAt(tEspera, fila, 3);
                modeloProcesos.setValueAt(tFin, fila, 4);
                modeloProcesos.setValueAt(tPenal, fila, 5);
            }
        });
    }

    /**
     * Elimina un proceso de la tabla. Es Thread-Safe.
     * (Opcional, puedes simplemente cambiar el estado a "Eliminado")
     */
    public void eliminarProcesoDeTabla(String id) {
        SwingUtilities.invokeLater(() -> {
            int fila = buscarFilaPorID(id);
            if (fila != -1) {
                modeloProcesos.removeRow(fila);
            }
        });
    }

    /**
     * Método de ayuda para encontrar una fila basado en el ID de la columna 0.
     * @return El índice de la fila, o -1 si no se encuentra.
     */
    private int buscarFilaPorID(String id) {
        for (int i = 0; i < modeloProcesos.getRowCount(); i++) {
            if (id.equals(modeloProcesos.getValueAt(i, 0))) {
                return i;
            }
        }
        return -1; // No se encontró
    }

    // --- Main para probar la GUI ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteGUI cliente = new ClienteGUI();
            cliente.setVisible(true);

            // --- Prueba de actualización dinámica ---
            // (Simula lo que haría tu hilo de red)
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    cliente.agregarOActualizarProceso("P1", "Creado", "10", "0", "N/A", "N/A");
                    Thread.sleep(1000);
                    cliente.agregarOActualizarProceso("P2", "Creado", "5", "0", "N/A", "N/A");
                    Thread.sleep(2000);
                    cliente.agregarOActualizarProceso("P1", "Llegada al Servidor", "10", "0", "N/A", "N/A");
                    Thread.sleep(2000);
                    cliente.agregarOActualizarProceso("P2", "Llegada al Servidor", "5", "0", "N/A", "N/A");
                    Thread.sleep(1000);
                    cliente.agregarOActualizarProceso("P1", "En Ejecución", "10", "2", "N/A", "N/A");
                    Thread.sleep(3000);
                    cliente.agregarOActualizarProceso("P1", "Finalizado", "10", "2", "15", "1.3");
                } catch (InterruptedException e) {}
            }).start();
        });
    }
}
