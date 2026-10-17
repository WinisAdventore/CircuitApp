import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.sound.sampled.*;

public class CircuitApp extends JFrame {

    private final CanvasPanel canvas = new CanvasPanel();
    private final JPanel componentMenu = new JPanel();

    // === Панель свойств ===
    private final JPanel propsPanel = new JPanel();
    private final JLabel propsTitle = new JLabel("Свойства");
    private final JTextArea propsText = new JTextArea();
    private final JButton deleteWireBtn = new JButton("Удалить провод");
    private final JButton deleteCompBtn = new JButton("Удалить компонент");
    private final JButton toggleLedBtn = new JButton("Переключить вручную");
    private final JButton rotateBtn = new JButton("Повернуть на 90°");
    private final JButton repairLedBtn = new JButton("Восстановить светодиод");
    private final JButton toggleSwitchBtn = new JButton("Переключить (ВКЛ/ВЫКЛ)"); 
    
    // Панели настроек
    private final JPanel powerSettingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JSpinner powerSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));

    private final JPanel resSettingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JSpinner resSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 10000, 10));

    public CircuitApp() {
        setTitle("Редактор схем (Логика + Электрика)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1250, 850);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Иконка приложения (из ресурсов JAR)
        try {
            java.net.URL iconUrl = getClass().getResource("/resources/cpu.png");
            if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());
        } catch (Exception e) {}

        // ===== Верхняя панель =====
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        topBar.setBackground(new Color(240, 240, 240));
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));

        JButton runBtn = makeTopButton("start.png", "Запуск");
        runBtn.addActionListener(e -> canvas.runSimulation());

        JButton chipBtn = makeTopButton("cpu.png", "Компоненты");
        chipBtn.addActionListener(e -> toggleComponentMenu(chipBtn));

        topBar.add(runBtn);
        topBar.add(chipBtn);
        add(topBar, BorderLayout.NORTH);

        // ===== Правая панель =====
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(240, 0));
        rightPanel.setBackground(new Color(250, 250, 250));
        rightPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel compList = new JPanel();
        compList.setLayout(new BoxLayout(compList, BoxLayout.Y_AXIS));
        compList.setBackground(new Color(250, 250, 250));

        JLabel title = new JLabel("Компоненты");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        compList.add(title);
        compList.add(Box.createVerticalStrut(10));

        addComponentButton(compList, "Светодиод", "LED");
        addComponentButton(compList, "Резистор", "RES");
        addComponentButton(compList, "Выключатель", "SWITCH"); 
        addComponentButton(compList, "AND (И)", "AND");
        addComponentButton(compList, "OR (ИЛИ)", "OR");
        addComponentButton(compList, "NOT (НЕ)", "NOT");
        addComponentButton(compList, "Заземление", "GND");
        addComponentButton(compList, "Питание", "VCC");
        compList.add(Box.createVerticalStrut(20));

        JButton clearBtn = new JButton("Очистить всё");
        clearBtn.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        clearBtn.addActionListener(e -> canvas.clearAll());
        compList.add(clearBtn);
        compList.add(Box.createVerticalStrut(20));

        // --- Блок свойств ---
        propsPanel.setLayout(new BoxLayout(propsPanel, BoxLayout.Y_AXIS));
        propsPanel.setBackground(new Color(245, 245, 250));
        propsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 210)),
                new EmptyBorder(8, 8, 8, 8)));

        propsTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        propsTitle.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        propsPanel.add(propsTitle);
        propsPanel.add(Box.createVerticalStrut(6));

        propsText.setEditable(false);
        propsText.setOpaque(false);
        propsText.setFont(new Font("SansSerif", Font.PLAIN, 12));
        propsText.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        propsText.setMaximumSize(new Dimension(210, 180));
        propsText.setText("Кликните на провод или компонент,\nчтобы увидеть свойства.");
        propsPanel.add(propsText);
        propsPanel.add(Box.createVerticalStrut(8));

        // Мощность питания
        powerSettingsPanel.setOpaque(false);
        powerSettingsPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        powerSettingsPanel.setMaximumSize(new Dimension(210, 30));
        powerSettingsPanel.add(new JLabel("Мощность (Вт): "));
        powerSpinner.setPreferredSize(new Dimension(60, 24));
        powerSpinner.addChangeListener(e -> {
            CircuitComponent sel = canvas.getSelectedComponent();
            if (sel instanceof Power) {
                ((Power) sel).wattage = (Integer) powerSpinner.getValue();
                canvas.runSimulationSilent();
                updatePropsUI();
            }
        });
        powerSettingsPanel.add(powerSpinner);
        powerSettingsPanel.setVisible(false);
        propsPanel.add(powerSettingsPanel);
        propsPanel.add(Box.createVerticalStrut(6));

        // Сопротивление резистора
        resSettingsPanel.setOpaque(false);
        resSettingsPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        resSettingsPanel.setMaximumSize(new Dimension(210, 30));
        resSettingsPanel.add(new JLabel("Сопротивление: "));
        resSpinner.setPreferredSize(new Dimension(70, 24));
        resSpinner.addChangeListener(e -> {
            CircuitComponent sel = canvas.getSelectedComponent();
            if (sel instanceof Resistor) {
                ((Resistor) sel).resistance = (Integer) resSpinner.getValue();
                canvas.runSimulationSilent();
                updatePropsUI();
            }
        });
        resSettingsPanel.add(resSpinner);
        resSettingsPanel.add(new JLabel(" Ом"));
        resSettingsPanel.setVisible(false);
        propsPanel.add(resSettingsPanel);
        propsPanel.add(Box.createVerticalStrut(6));

        deleteWireBtn.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        deleteWireBtn.setMaximumSize(new Dimension(210, 28));
        deleteWireBtn.setVisible(false);
        deleteWireBtn.addActionListener(e -> {
            canvas.deleteSelectedWire();
            updatePropsUI();
        });
        propsPanel.add(deleteWireBtn);

        deleteCompBtn.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        deleteCompBtn.setMaximumSize(new Dimension(210, 28));
        deleteCompBtn.setVisible(false);
        deleteCompBtn.addActionListener(e -> {
            canvas.deleteSelectedComponent();
            updatePropsUI();
        });
        propsPanel.add(deleteCompBtn);

        toggleLedBtn.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        toggleLedBtn.setMaximumSize(new Dimension(210, 28));
        toggleLedBtn.setVisible(false);
        toggleLedBtn.addActionListener(e -> {
            canvas.toggleLedManually();
            updatePropsUI();
        });
        propsPanel.add(toggleLedBtn);
        
        toggleSwitchBtn.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        toggleSwitchBtn.setMaximumSize(new Dimension(210, 28));
        toggleSwitchBtn.setVisible(false);
        toggleSwitchBtn.addActionListener(e -> {
            CircuitComponent sel = canvas.getSelectedComponent();
            if (sel instanceof Switch) {
                ((Switch) sel).isClosed = !((Switch) sel).isClosed;
                canvas.runSimulationSilent();
                updatePropsUI();
            }
        });
        propsPanel.add(toggleSwitchBtn);

        rotateBtn.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        rotateBtn.setMaximumSize(new Dimension(210, 28));
        rotateBtn.setVisible(false);
        rotateBtn.addActionListener(e -> {
            canvas.rotateSelectedComponent();
            updatePropsUI();
        });
        propsPanel.add(rotateBtn);
        
        repairLedBtn.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        repairLedBtn.setMaximumSize(new Dimension(210, 28));
        repairLedBtn.setVisible(false);
        repairLedBtn.addActionListener(e -> {
            CircuitComponent sel = canvas.getSelectedComponent();
            if (sel instanceof Led) {
                ((Led) sel).isBurned = false;
                ((Led) sel).manualOn = false;
                canvas.runSimulationSilent();
                updatePropsUI();
            }
        });
        propsPanel.add(repairLedBtn);

        rightPanel.add(compList, BorderLayout.NORTH);
        rightPanel.add(propsPanel, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);

        // ===== Выпадающее меню компонентов =====
        componentMenu.setLayout(new BoxLayout(componentMenu, BoxLayout.Y_AXIS));
        componentMenu.setBackground(Color.WHITE);
        componentMenu.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        componentMenu.setVisible(false);

        // ===== Холст =====
        add(canvas, BorderLayout.CENTER);
        canvas.setPropsListener(this::updatePropsUI);

        setGlassPane(componentMenu);
        getGlassPane().setVisible(false);

        // ===== Delete на клавиатуре =====
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSel");
        getRootPane().getActionMap().put("deleteSel", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                canvas.deleteSelected();
                updatePropsUI();
            }
        });
    }

    // =========================================================
    // Обновление блока свойств
    // =========================================================
    private void updatePropsUI() {
        CircuitComponent selComp = canvas.getSelectedComponent();
        Wire selWire = canvas.getSelectedWire();

        propsText.setText("");
        deleteWireBtn.setVisible(false);
        deleteCompBtn.setVisible(false);
        toggleLedBtn.setVisible(false);
        toggleSwitchBtn.setVisible(false);
        rotateBtn.setVisible(false);
        powerSettingsPanel.setVisible(false);
        repairLedBtn.setVisible(false);
        resSettingsPanel.setVisible(false);

        if (selWire != null) {
            propsTitle.setText("Провод");
            propsText.setText(
                    "Тип: провод\n" +
                    "От: " + selWire.a.owner.getClass().getSimpleName() + "\n" +
                    "К:  " + selWire.b.owner.getClass().getSimpleName() + "\n\n" +
                    "Удалить: ПКМ / Delete / кнопка.");
            deleteWireBtn.setVisible(true);
        } else if (selComp != null) {
            propsTitle.setText("Компонент: " + selComp.getClass().getSimpleName());
            StringBuilder sb = new StringBuilder();
            sb.append("Тип: ").append(selComp.getClass().getSimpleName()).append("\n");
            sb.append("Позиция: (").append(selComp.x).append(", ").append(selComp.y).append(")\n");
            sb.append("Угол: ").append(selComp.angle).append("°\n");
            sb.append("Состояние: ").append(selComp.lit ? "ВКЛ" : "выкл").append("\n");

            if (selComp instanceof Led) {
                Led led = (Led) selComp;
                sb.append("\nАнод(+) сверху, Катод(-) снизу\n");
                
                boolean toSignal = canvas.hasSignal(led, 0);
                boolean toPower = canvas.hasPathToPinType(led, Power.class, 0);
                int watts = canvas.getMaxWattsToPin(led, 0); 
                
                boolean toGround = canvas.hasPathToPinType(led, Ground.class, 1);
                
                if (toSignal) {
                    sb.append("Логический сигнал (1): ЕСТЬ\n");
                } else if (toPower) {
                    sb.append("Питание: есть (").append(watts).append(" Вт)\n");
                } else {
                    sb.append("Питание/Сигнал: нет\n");
                }
                
                sb.append("Земля на Катод: ").append(toGround ? "да" : "нет").append("\n");
                
                if (led.isBurned) {
                    sb.append("\u274C СВЕТОДИОД СГОРЕЛ! (> 5 Вт)\n");
                    sb.append("Нажмите \"Восстановить\".\n");
                    repairLedBtn.setVisible(true);
                } else if (toSignal && toGround) {
                    sb.append("\u2705 Логика: ВКЛ (1)\n");
                } else if (toPower && toGround) {
                    if (watts < 3) {
                        sb.append("\u26A0 Недостаточно мощности (< 3 Вт)\n");
                    } else if (watts >= 3 && watts <= 5) {
                        sb.append("\u2705 Нормальный режим (3-5 Вт)\n");
                    }
                }

                sb.append("\nРучное вкл: ").append(led.manualOn ? "да" : "нет");
                toggleLedBtn.setText(led.manualOn ? "Ручное: ВКЛ (выключить)" : "Ручное: ВЫКЛ (включить)");
                toggleLedBtn.setVisible(true);
            } else if (selComp instanceof Power) {
                Power p = (Power) selComp;
                sb.append("\nМощность: ").append(p.wattage).append(" Вт\n");
                powerSpinner.setValue(p.wattage);
                powerSettingsPanel.setVisible(true);
            } else if (selComp instanceof Resistor) {
                Resistor r = (Resistor) selComp;
                sb.append("\nСопротивление: ").append(r.resistance).append(" Ом\n");
                sb.append("Потери: ").append(r.getPowerLoss()).append(" Вт\n");
                resSpinner.setValue(r.resistance);
                resSettingsPanel.setVisible(true);
            } else if (selComp instanceof Switch) {
                Switch sw = (Switch) selComp;
                sb.append("\nСигнал (Выход): ").append(sw.isClosed ? "1 (ВКЛ)" : "0 (ВЫКЛ)").append("\n");
                toggleSwitchBtn.setText(sw.isClosed ? "Переключить в 0 (ВЫКЛ)" : "Переключить в 1 (ВКЛ)");
                toggleSwitchBtn.setVisible(true);
            } else if (selComp instanceof LogicGate) {
                LogicGate gate = (LogicGate) selComp;
                sb.append("\nВход A: ").append(gate.inA ? "1" : "0").append("\n");
                if (gate.type != LogicGate.TYPE_NOT) {
                    sb.append("Вход B: ").append(gate.inB ? "1" : "0").append("\n");
                }
                sb.append("Выход: ").append(gate.out ? "1 (ВКЛ)" : "0 (ВЫКЛ)").append("\n");
            } else if (selComp instanceof Junction) {
                sb.append("\nУзел соединения (Junction)\n");
                sb.append("К нему можно подключать другие провода.\n");
            }
            
            propsText.setText(sb.toString());
            deleteCompBtn.setVisible(true);
            rotateBtn.setVisible(true);
        } else {
            propsTitle.setText("Свойства");
            propsText.setText("Кликните на провод или компонент,\nчтобы увидеть свойства.");
        }
        propsPanel.revalidate();
        propsPanel.repaint();
    }

    // =========================================================
    // Верхние кнопки
    // =========================================================
    private JButton makeTopButton(String iconPath, String tooltip) {
        ImageIcon icon = loadIcon(iconPath, 28);
        JButton b = (icon != null) ? new JButton(icon) : new JButton(tooltip);
        b.setToolTipText(tooltip);
        b.setPreferredSize(new Dimension(44, 36));
        b.setFocusPainted(false);
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        return b;
    }

    private ImageIcon loadIcon(String path, int targetSize) {
        try {
            // Загружаем картинку ИЗНУТРИ JAR (из папки resources)
            java.net.URL url = getClass().getResource("/resources/" + path);
            if (url == null) {
                System.err.println("Иконка не найдена в JAR: " + path);
                return null;
            }
            ImageIcon original = new ImageIcon(url);
            int w = original.getIconWidth(), h = original.getIconHeight();
            if (w <= 0 || h <= 0) return original;
            double s = (double) targetSize / Math.max(w, h);
            return new ImageIcon(original.getImage().getScaledInstance(
                    Math.max(1, (int) (w * s)), Math.max(1, (int) (h * s)), Image.SCALE_SMOOTH));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки иконки: " + e.getMessage());
            return null;
        }
    }

    private void addComponentButton(JPanel parent, String name, String type) {
        JButton b = new JButton(name);
        b.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(200, 32));
        b.addActionListener(e -> canvas.setPendingComponent(type));
        parent.add(b);
        parent.add(Box.createVerticalStrut(6));
    }

    private void toggleComponentMenu(JButton chipBtn) {
        componentMenu.removeAll();
        String[] items = {"Светодиод", "Резистор", "Выключатель", "AND (И)", "OR (ИЛИ)", "NOT (НЕ)", "Заземление", "Питание"};
        String[] types = {"LED", "RES", "SWITCH", "AND", "OR", "NOT", "GND", "VCC"};
        for (int i = 0; i < items.length; i++) {
            final String type = types[i];
            JButton item = new JButton("   " + items[i] + "   ");
            item.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            item.setHorizontalAlignment(SwingConstants.LEFT);
            item.setMaximumSize(new Dimension(160, 30));
            item.addActionListener(ev -> {
                canvas.setPendingComponent(type);
                hideComponentMenu();
            });
            componentMenu.add(item);
        }
        componentMenu.add(Box.createVerticalStrut(5));

        Point p = SwingUtilities.convertPoint(chipBtn, 0, chipBtn.getHeight(), getContentPane());
        componentMenu.setLocation(p);
        componentMenu.setSize(160, 260);

        if (getGlassPane().isVisible()) hideComponentMenu();
        else getGlassPane().setVisible(true);
    }

    private void hideComponentMenu() { getGlassPane().setVisible(false); }

    // =========================================================
    // Холст схемы
    // =========================================================
    static class CanvasPanel extends JPanel {
        private final java.util.List<CircuitComponent> components = new ArrayList<>();
        private final java.util.List<Wire> wires = new ArrayList<>();

        private String pendingComponent = null;
        private double scale = 1.0;
        private double offsetX = 0, offsetY = 0;

        private Point dragStart;
        private Pin draggingFrom = null;
        private Point draggingEnd = null;
        private CircuitComponent movingComp = null;
        private Point movingOffset = null;

        private CircuitComponent selectedComponent = null;
        private Wire selectedWire = null;
        
        private Point mousePos = null;

        private Runnable propsListener = () -> {};
        public void setPropsListener(Runnable r) { this.propsListener = r; }
        public CircuitComponent getSelectedComponent() { return selectedComponent; }
        public Wire getSelectedWire() { return selectedWire; }

        public CanvasPanel() {
            setBackground(new Color(255, 255, 245));

            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    Point world = toWorld(e.getPoint());

                    if (e.getButton() == MouseEvent.BUTTON3) {
                        Wire w = findWireAt(world);
                        if (w != null) {
                            wires.remove(w); if (selectedWire == w) selectedWire = null;
                            repaint(); propsListener.run(); return;
                        }
                        CircuitComponent c = findCircuitAt(world);
                        if (c != null) {
                            components.remove(c);
                            wires.removeIf(wire -> wire.a.owner == c || wire.b.owner == c);
                            if (selectedComponent == c) selectedComponent = null;
                            repaint(); propsListener.run(); return;
                        }
                        return;
                    }

                    Wire w = findWireAt(world);
                    if (w != null) {
                        selectedWire = w; selectedComponent = null;
                        repaint(); propsListener.run(); return;
                    }

                    Pin pin = findPinAt(world);
                    if (pin != null) {
                        draggingFrom = pin; draggingEnd = world; return;
                    }

                    CircuitComponent comp = findCircuitAt(world);
                    if (comp != null) {
                        selectedComponent = comp; selectedWire = null;
                        movingComp = comp; movingOffset = new Point(world.x - comp.x, world.y - comp.y);
                        repaint(); propsListener.run(); return;
                    }

                    selectedComponent = null; selectedWire = null; propsListener.run();

                    if (pendingComponent != null) {
                        addComponentAt(pendingComponent, world);
                        pendingComponent = null; setCursor(Cursor.getDefaultCursor()); repaint();
                    } else {
                        dragStart = e.getPoint();
                    }
                }

                @Override public void mouseDragged(MouseEvent e) {
                    Point world = toWorld(e.getPoint());
                    mousePos = world;
                    if (draggingFrom != null) {
                        draggingEnd = world; repaint();
                    } else if (movingComp != null) {
                        movingComp.x = world.x - movingOffset.x;
                        movingComp.y = world.y - movingOffset.y;
                        movingComp.updatePins(); repaint();
                    } else if (dragStart != null) {
                        offsetX += e.getX() - dragStart.x;
                        offsetY += e.getY() - dragStart.y;
                        dragStart = e.getPoint(); repaint();
                    }
                }

                @Override public void mouseReleased(MouseEvent e) {
                    Point world = toWorld(e.getPoint());
                    if (draggingFrom != null) {
                        Pin target = findPinAt(world);
                        if (target != null && target != draggingFrom && target.owner != draggingFrom.owner) {
                            Wire w = new Wire(draggingFrom, target);
                            wires.add(w); selectedWire = w; selectedComponent = null;
                            propsListener.run();
                        } else {
                            Wire wOver = findWireAt(world);
                            if (wOver != null) {
                                Junction j = new Junction(world.x, world.y);
                                components.add(j);
                                wires.add(new Wire(draggingFrom, j.getPins().get(0)));
                                selectedComponent = j;
                                selectedWire = null;
                                propsListener.run();
                            }
                        }
                        draggingFrom = null; draggingEnd = null; repaint();
                    }
                    movingComp = null; movingOffset = null; dragStart = null;
                }

                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        Point world = toWorld(e.getPoint());
                        Wire w = findWireAt(world);
                        if (w != null) {
                            wires.remove(w); if (selectedWire == w) selectedWire = null;
                            repaint(); propsListener.run();
                        }
                    }
                }
                
                @Override public void mouseMoved(MouseEvent e) {
                    mousePos = toWorld(e.getPoint());
                    repaint();
                }
            };

            addMouseListener(ma);
            addMouseMotionListener(ma);

            addMouseWheelListener(e -> {
                double factor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
                Point before = toWorld(e.getPoint());
                scale *= factor; scale = Math.max(0.3, Math.min(3.0, scale));
                Point after = toWorld(e.getPoint());
                offsetX += (after.x - before.x) * scale; offsetY += (after.y - before.y) * scale;
                repaint();
            });
        }

        public void setPendingComponent(String type) {
            pendingComponent = type;
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }

        public void clearAll() {
            components.clear(); wires.clear(); selectedComponent = null; selectedWire = null;
            repaint(); propsListener.run();
        }

        private void addComponentAt(String type, Point p) {
            CircuitComponent c;
            switch (type) {
                case "LED": c = new Led(p.x, p.y); break;
                case "RES": c = new Resistor(p.x, p.y); break;
                case "SWITCH": c = new Switch(p.x, p.y); break;
                case "AND": c = new LogicGate(p.x, p.y, LogicGate.TYPE_AND); break;
                case "OR": c = new LogicGate(p.x, p.y, LogicGate.TYPE_OR); break;
                case "NOT": c = new LogicGate(p.x, p.y, LogicGate.TYPE_NOT); break;
                case "GND": c = new Ground(p.x, p.y); break;
                case "VCC": c = new Power(p.x, p.y); break;
                default: return;
            }
            components.add(c); selectedComponent = c; selectedWire = null; propsListener.run();
        }

        public void deleteSelected() {
            if (selectedWire != null) { wires.remove(selectedWire); selectedWire = null; }
            else if (selectedComponent != null) {
                CircuitComponent c = selectedComponent;
                components.remove(c); wires.removeIf(w -> w.a.owner == c || w.b.owner == c);
                selectedComponent = null;
            }
            repaint();
        }
        public void deleteSelectedWire() {
            if (selectedWire != null) { wires.remove(selectedWire); selectedWire = null; repaint(); }
        }
        public void deleteSelectedComponent() {
            if (selectedComponent != null) {
                CircuitComponent c = selectedComponent;
                components.remove(c); wires.removeIf(w -> w.a.owner == c || w.b.owner == c);
                selectedComponent = null; repaint();
            }
        }
        public void toggleLedManually() {
            if (selectedComponent instanceof Led) {
                Led led = (Led) selectedComponent;
                led.manualOn = !led.manualOn;
                runSimulationSilent(); 
            }
        }
        public void rotateSelectedComponent() {
            if (selectedComponent != null) {
                selectedComponent.angle = (selectedComponent.angle + 90) % 360;
                selectedComponent.updatePins();
                repaint();
            }
        }

        public boolean hasSignal(CircuitComponent from, int pinIndex) {
            if (pinIndex >= from.getPins().size()) return false;
            Pin startPin = from.getPins().get(pinIndex);
            return hasSignalFromPin(startPin, new HashSet<>());
        }

        private boolean hasSignalFromPin(Pin startPin, Set<CircuitComponent> visited) {
            for (Wire w : wires) {
                CircuitComponent other = null;
                Pin nextPin = null;
                if (w.a == startPin) { other = w.b.owner; nextPin = w.b; }
                else if (w.b == startPin) { other = w.a.owner; nextPin = w.a; }
                
                if (other != null && !visited.contains(other)) {
                    if (other instanceof Switch) {
                        if (((Switch) other).isClosed) return true;
                    } else if (other instanceof LogicGate) {
                        LogicGate gate = (LogicGate) other;
                        int outIdx = (gate.type == LogicGate.TYPE_NOT) ? 1 : 2;
                        if (nextPin == gate.getPins().get(outIdx)) {
                            if (gate.out) return true;
                        } else {
                            visited.add(other);
                            for (Pin p : gate.getPins()) {
                                if (p != nextPin && hasSignalFromPin(p, visited)) return true;
                            }
                        }
                    } else {
                        visited.add(other);
                        for (Pin p : other.getPins()) {
                            if (p != nextPin && hasSignalFromPin(p, visited)) return true;
                        }
                    }
                }
            }
            return false;
        }

        public int getMaxWattsToPin(CircuitComponent from, int pinIndex) {
            if (pinIndex >= from.getPins().size()) return 0;
            Pin startPin = from.getPins().get(pinIndex);
            return findMaxWattsFromPin(startPin, new HashSet<>());
        }

        private int findMaxWattsFromPin(Pin startPin, Set<CircuitComponent> visited) {
            int maxWatts = 0;
            for (Wire w : wires) {
                CircuitComponent other = null;
                Pin nextPin = null;
                if (w.a == startPin) { other = w.b.owner; nextPin = w.b; }
                else if (w.b == startPin) { other = w.a.owner; nextPin = w.a; }
                
                if (other != null && !visited.contains(other)) {
                    if (other instanceof Power) {
                        maxWatts = Math.max(maxWatts, ((Power) other).wattage);
                    } else if (other instanceof Resistor) {
                        Resistor r = (Resistor) other;
                        visited.add(other);
                        for (Pin p : other.getPins()) {
                            if (p != nextPin) {
                                int wIn = findMaxWattsFromPin(p, visited);
                                if (wIn > 0) {
                                    int wOut = Math.max(0, wIn - r.getPowerLoss());
                                    maxWatts = Math.max(maxWatts, wOut);
                                }
                            }
                        }
                    } else if (other instanceof Switch) {
                        Switch sw = (Switch) other;
                        if (sw.isClosed) {
                            visited.add(other);
                            for (Pin p : other.getPins()) {
                                if (p != nextPin) {
                                    maxWatts = Math.max(maxWatts, findMaxWattsFromPin(p, visited));
                                }
                            }
                        }
                    } else {
                        visited.add(other);
                        for (Pin p : other.getPins()) {
                            if (p != nextPin) {
                                maxWatts = Math.max(maxWatts, findMaxWattsFromPin(p, visited));
                            }
                        }
                    }
                }
            }
            return maxWatts;
        }

        public boolean hasPathToPinType(CircuitComponent from, Class<?> targetType, int pinIndex) {
            if (pinIndex >= from.getPins().size()) return false;
            Pin startPin = from.getPins().get(pinIndex);
            return hasPathFromPin(startPin, targetType, new HashSet<>());
        }

        private boolean hasPathFromPin(Pin startPin, Class<?> targetType, Set<CircuitComponent> visited) {
            for (Wire w : wires) {
                CircuitComponent other = null;
                Pin nextPin = null;
                if (w.a == startPin) { other = w.b.owner; nextPin = w.b; }
                else if (w.b == startPin) { other = w.a.owner; nextPin = w.a; }
                
                if (other != null) {
                    if (targetType.isInstance(other)) return true;
                    if (!visited.contains(other)) {
                        visited.add(other);
                        for (Pin p : other.getPins()) {
                            if (p != nextPin && hasPathFromPin(p, targetType, visited)) return true;
                        }
                    }
                }
            }
            return false;
        }

        private Pin findPinAt(Point world) {
            for (CircuitComponent c : components)
                for (Pin pin : c.getPins())
                    if (pin.position.distance(world) < 8) return pin;
            return null;
        }

        private CircuitComponent findCircuitAt(Point world) {
            for (int i = components.size() - 1; i >= 0; i--) {
                CircuitComponent c = components.get(i);
                if (Math.abs(world.x - c.x) < 25 && Math.abs(world.y - c.y) < 25) return c;
            }
            return null;
        }

        private Wire findWireAt(Point world) {
            for (Wire w : wires) if (distanceToWire(world, w) < 6) return w;
            return null;
        }

        private double distanceToWire(Point p, Wire w) {
            Point a = w.a.position, b = w.b.position;
            if (Math.abs(a.x - b.x) < 3 || Math.abs(a.y - b.y) < 3) return distanceToSegment(p, a, b);
            int midX = (a.x + b.x) / 2;
            Point p1 = new Point(midX, a.y), p2 = new Point(midX, b.y);
            return Math.min(Math.min(distanceToSegment(p, a, p1), distanceToSegment(p, p1, p2)), distanceToSegment(p, p2, b));
        }

        private double distanceToSegment(Point p, Point a, Point b) {
            double dx = b.x - a.x, dy = b.y - a.y;
            double len2 = dx * dx + dy * dy;
            if (len2 == 0) return p.distance(a);
            double t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / len2;
            t = Math.max(0, Math.min(1, t));
            return Math.hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy));
        }

        private Point toWorld(Point screen) {
            return new Point((int) ((screen.x - offsetX) / scale), (int) ((screen.y - offsetY) / scale));
        }

        public void runSimulation() {
            components.removeIf(c -> c instanceof Junction && c.getPins().get(0).connections.isEmpty());
            runSimulationSilent();
            JOptionPane.showMessageDialog(this,
                    "Симуляция выполнена.\nСветодиод горит от логической 1 или от Питания (3-5 Вт).",
                    "Результат", JOptionPane.INFORMATION_MESSAGE);
        }

        public void runSimulationSilent() {
            for (CircuitComponent c : components) {
                c.lit = false;
                if (c instanceof Led) {
                    ((Led) c).isBurned = false;
                }
            }
            
            for (CircuitComponent c : components) {
                if (c instanceof LogicGate) {
                    LogicGate gate = (LogicGate) c;
                    gate.inA = hasSignal(gate, 0);
                    if (gate.type != LogicGate.TYPE_NOT) {
                        gate.inB = hasSignal(gate, 1);
                    }
                    
                    if (gate.type == LogicGate.TYPE_AND) {
                        gate.out = gate.inA && gate.inB;
                    } else if (gate.type == LogicGate.TYPE_OR) {
                        gate.out = gate.inA || gate.inB;
                    } else if (gate.type == LogicGate.TYPE_NOT) {
                        gate.out = !gate.inA;
                    }
                }
            }
            
            for (CircuitComponent led : components) {
                if (!(led instanceof Led)) continue;
                Led l = (Led) led;
                
                boolean toSignal = hasSignal(l, 0); 
                boolean toGround = hasPathToPinType(l, Ground.class, 1); 
                
                boolean toPower = hasPathToPinType(l, Power.class, 0);
                int watts = getMaxWattsToPin(l, 0);
                
                if (toGround) {
                    if (toSignal) {
                        l.lit = true;
                    } else if (toPower) {
                        if (watts > 5) {
                            l.isBurned = true; 
                            l.lit = false;
                        } else if (watts >= 3) {
                            l.lit = true; 
                        } else {
                            l.lit = false;
                        }
                    }
                }
                
                if (l.manualOn && !l.isBurned) {
                    l.lit = true;
                }
            }
            repaint(); propsListener.run();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(235, 235, 235));
            int grid = (int) (30 * scale);
            int ox = (int) (offsetX % grid), oy = (int) (offsetY % grid);
            for (int x = ox; x < getWidth(); x += grid) g2.drawLine(x, 0, x, getHeight());
            for (int y = oy; y < getHeight(); y += grid) g2.drawLine(0, y, getWidth(), y);

            g2.translate(offsetX, offsetY);
            g2.scale(scale, scale);

            for (Wire w : wires) {
                boolean sel = (w == selectedWire);
                g2.setColor(sel ? new Color(255, 140, 0) : new Color(70, 70, 70));
                g2.setStroke(new BasicStroke(sel ? 3.5f : 2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawOrthogonalWire(g2, w.a.position, w.b.position);
            }

            if (draggingFrom != null && draggingEnd != null) {
                g2.setColor(Color.BLUE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 4f}, 0f));
                drawOrthogonalWire(g2, draggingFrom.position, draggingEnd);
            }

            for (CircuitComponent c : components) c.draw(g2);

            if (selectedComponent != null) {
                CircuitComponent c = selectedComponent;
                g2.setColor(new Color(255, 140, 0));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 3f}, 0f));
                g2.drawRect(c.x - 28, c.y - 28, 56, 56);
            }

            for (CircuitComponent c : components) {
                for (Pin pin : c.getPins()) {
                    boolean isHovered = (mousePos != null && pin.position.distance(mousePos) < 8);
                    
                    if (isHovered) {
                        g2.setColor(new Color(0, 200, 255)); 
                        g2.fillOval(pin.position.x - 7, pin.position.y - 7, 14, 14);
                    } else {
                        g2.setColor(new Color(30, 100, 200));
                        g2.fillOval(pin.position.x - 5, pin.position.y - 5, 10, 10);
                    }
                    
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1));
                    g2.drawOval(pin.position.x - 5, pin.position.y - 5, 10, 10);
                }
            }
            g2.dispose();
        }

        private void drawOrthogonalWire(Graphics2D g2, Point a, Point b) {
            if (Math.abs(a.x - b.x) < 3 || Math.abs(a.y - b.y) < 3) {
                g2.drawLine(a.x, a.y, b.x, b.y); return;
            }
            int midX = (a.x + b.x) / 2;
            g2.drawLine(a.x, a.y, midX, a.y);
            g2.drawLine(midX, a.y, midX, b.y);
            g2.drawLine(midX, b.y, b.x, b.y);
        }
    }

    // =========================================================
    // Модели
    // =========================================================
    static class Pin {
        CircuitComponent owner;
        Point position;
        java.util.List<Pin> connections = new ArrayList<>();
        Pin(CircuitComponent owner, Point position) { this.owner = owner; this.position = position; }
    }

    static class Wire {
        Pin a, b;
        Wire(Pin a, Pin b) { 
            this.a = a; this.b = b; 
            a.connections.add(b);
            b.connections.add(a);
        }
    }

    static abstract class CircuitComponent {
        int x, y;
        int angle = 0; 
        boolean lit = false;
        java.util.List<Pin> pins = new ArrayList<>();
        CircuitComponent(int x, int y) { this.x = x; this.y = y; }
        java.util.List<Pin> getPins() { return pins; }
        abstract void updatePins();
        abstract void draw(Graphics2D g);

        Point getRotatedPoint(int dx, int dy) {
            double rad = Math.toRadians(angle);
            int nx = (int) (x + dx * Math.cos(rad) - dy * Math.sin(rad));
            int ny = (int) (y + dx * Math.sin(rad) + dy * Math.cos(rad));
            return new Point(nx, ny);
        }
    }

    static class Junction extends CircuitComponent {
        Junction(int x, int y) {
            super(x, y);
            pins.add(new Pin(this, new Point(x, y)));
        }

        @Override void updatePins() {
            if (!pins.isEmpty()) {
                pins.get(0).position.setLocation(x, y);
            }
        }

        @Override void draw(Graphics2D g) {
            g.setColor(Color.BLACK);
            g.fillOval(x - 4, y - 4, 8, 8);
        }
    }

    static class Led extends CircuitComponent {
        boolean manualOn = false;
        boolean isBurned = false; 
        
        Led(int x, int y) { super(x, y); updatePins(); }
        
        @Override void updatePins() {
            if (pins.isEmpty()) {
                pins.add(new Pin(this, getRotatedPoint(0, -25))); 
                pins.add(new Pin(this, getRotatedPoint(0, 25)));  
            } else {
                pins.get(0).position.setLocation(getRotatedPoint(0, -25));
                pins.get(1).position.setLocation(getRotatedPoint(0, 25));
            }
        }

        @Override void draw(Graphics2D g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.rotate(Math.toRadians(angle));

            g2.setColor(new Color(200, 200, 200));
            g2.fillOval(-18, -18, 36, 36);
            g2.setColor(Color.DARK_GRAY);
            g2.drawOval(-18, -18, 36, 36);

            if (isBurned) {
                g2.setColor(new Color(40, 40, 40));
                g2.fillOval(-14, -14, 28, 28);
                g2.setColor(Color.RED);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(-8, -8, 8, 8);
                g2.drawLine(8, -8, -8, 8);
            } else if (lit) {
                for (int i = 3; i >= 1; i--) {
                    g2.setColor(new Color(255, 60, 60, 40));
                    int r = 18 + i * 6;
                    g2.fillOval(-r, -r, r * 2, r * 2);
                }
                g2.setColor(new Color(255, 60, 60));
                g2.fillOval(-14, -14, 28, 28);
                g2.setColor(Color.WHITE);
                g2.fillPolygon(new int[]{-7, 7, 0}, new int[]{-6, -6, 6}, 3);
            } else {
                g2.setColor(new Color(120, 30, 30));
                g2.fillOval(-14, -14, 28, 28);
                g2.setColor(Color.WHITE);
                g2.fillPolygon(new int[]{-7, 7, 0}, new int[]{-6, -6, 6}, 3);
            }

            g2.setColor(Color.BLACK);
            g2.drawLine(0, -18, 0, -25);
            g2.drawLine(0, 18, 0, 25);
            g2.dispose();
        }
    }

    static class Resistor extends CircuitComponent {
        int resistance = 100; 
        
        Resistor(int x, int y) { super(x, y); updatePins(); }
        
        public int getPowerLoss() {
            return resistance / 100; 
        }
        
        @Override void updatePins() {
            if (pins.isEmpty()) {
                pins.add(new Pin(this, getRotatedPoint(0, -25)));
                pins.add(new Pin(this, getRotatedPoint(0, 25)));
            } else {
                pins.get(0).position.setLocation(getRotatedPoint(0, -25));
                pins.get(1).position.setLocation(getRotatedPoint(0, 25));
            }
        }

        @Override void draw(Graphics2D g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.rotate(Math.toRadians(angle));

            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(0, -25, 0, -15);
            g2.drawLine(0, 15, 0, 25);
            
            g2.setColor(Color.WHITE);
            g2.fillRect(-8, -15, 16, 30);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(-8, -15, 16, 30);
            
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(-8, -10, 8, -10);
            g2.drawLine(-8, 0, 8, 0);
            g2.drawLine(-8, 10, 8, 10);

            g2.setColor(new Color(0, 100, 0));
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString(resistance + "Ω", -15, -18);
            
            g2.dispose();
        }
    }

    static class Switch extends CircuitComponent {
        boolean isClosed = false; 
        
        Switch(int x, int y) { super(x, y); updatePins(); }
        
        @Override void updatePins() {
            if (pins.isEmpty()) {
                pins.add(new Pin(this, getRotatedPoint(0, -25)));
                pins.add(new Pin(this, getRotatedPoint(0, 25)));
            } else {
                pins.get(0).position.setLocation(getRotatedPoint(0, -25));
                pins.get(1).position.setLocation(getRotatedPoint(0, 25));
            }
        }

        @Override void draw(Graphics2D g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.rotate(Math.toRadians(angle));

            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(0, -25, 0, -10);
            g2.drawLine(0, 10, 0, 25);
            
            g2.fillOval(-3, -12, 6, 6);
            g2.fillOval(-3, 6, 6, 6);
            
            if (isClosed) {
                g2.drawLine(0, -9, 0, 9);
            } else {
                g2.drawLine(0, 9, 12, -8);
            }
            g2.dispose();
        }
    }

    static class LogicGate extends CircuitComponent {
        static final int TYPE_AND = 0;
        static final int TYPE_OR = 1;
        static final int TYPE_NOT = 2;
        
        int type;
        boolean inA = false, inB = false, out = false;
        
        LogicGate(int x, int y, int type) { 
            super(x, y); 
            this.type = type;
            updatePins(); 
        }
        
        @Override void updatePins() {
            pins.clear();
            if (type == TYPE_NOT) {
                pins.add(new Pin(this, getRotatedPoint(-25, 0))); 
                pins.add(new Pin(this, getRotatedPoint(25, 0)));  
            } else {
                pins.add(new Pin(this, getRotatedPoint(-25, -12))); 
                pins.add(new Pin(this, getRotatedPoint(-25, 12)));  
                pins.add(new Pin(this, getRotatedPoint(25, 0)));    
            }
        }

        @Override void draw(Graphics2D g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.rotate(Math.toRadians(angle));

            g2.setColor(new Color(220, 220, 220));
            g2.fillRect(-20, -20, 40, 40);
            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(-20, -20, 40, 40);
            
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            String label = "";
            if (type == TYPE_AND) label = "&";
            else if (type == TYPE_OR) label = "\u22651";
            else if (type == TYPE_NOT) label = "1";
            
            int strW = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, -strW/2, 5);
            
            g2.dispose();
        }
    }

    static class Ground extends CircuitComponent {
        Ground(int x, int y) { super(x, y); updatePins(); }
        
        @Override void updatePins() {
            if (pins.isEmpty()) {
                pins.add(new Pin(this, getRotatedPoint(0, -25)));
            } else {
                pins.get(0).position.setLocation(getRotatedPoint(0, -25));
            }
        }

        @Override void draw(Graphics2D g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.rotate(Math.toRadians(angle));

            g2.setColor(Color.DARK_GRAY);
            g2.drawLine(0, -25, 0, -5);
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(-18, -5, 18, -5);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawLine(-12, 3, 12, 3);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(-6, 11, 6, 11);
            g2.dispose();
        }
    }

    static class Power extends CircuitComponent {
        int wattage = 5; 
        
        Power(int x, int y) { super(x, y); updatePins(); }
        
        @Override void updatePins() {
            if (pins.isEmpty()) {
                pins.add(new Pin(this, getRotatedPoint(0, 25)));
            } else {
                pins.get(0).position.setLocation(getRotatedPoint(0, 25));
            }
        }

        @Override void draw(Graphics2D g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.rotate(Math.toRadians(angle));

            g2.setColor(Color.DARK_GRAY);
            g2.drawLine(0, 25, 0, 5);
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(-18, 5, 18, 5);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(-10, -3, 10, -3);
            g2.setColor(new Color(200, 0, 0));
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString("+", -4, -8);
            g2.drawString(wattage + "W", -12, -15);
            g2.dispose();
        }
    }

    // =========================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CircuitApp().setVisible(true));
    }
}