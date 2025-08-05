package com.weichai.mac.process.reuse;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * 支持行列合并且垂直/水平居中的 JTable 实现
 */
public class SpanTable extends JTable {
    private final SpanTableModel spanTableModel;

    public SpanTable(SpanTableModel model) {
        super(model);
        this.spanTableModel = model;
        // 关闭默认网格线并移除单元格间隙（你 UI 里自己画 border）
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
    }

    public SpanTableModel getSpanModel() {
        return spanTableModel;
    }



    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (spanTableModel != null) {
            CellSpan span = spanTableModel.getSpan(row, column);
            if (span != null && span.anchorRow == row && span.anchorCol == column
                    && (span.rowSpan > 1 || span.colSpan > 1)) {
                // 这是合并区域的 anchor，用空渲染器让默认不画内容
                return (table, value, isSelected, hasFocus, r, c) -> {
                    JLabel placeholder = new JLabel();
                    placeholder.setOpaque(false);
                    return placeholder;
                };
            }
            // 覆盖被合并后作为"被覆盖单元"的绘制（可选：如果你不想它们画边框内容）
            if (span != null && (span.rowSpan == 0 || span.colSpan == 0)) {
                return (table, value, isSelected, hasFocus, r, c) -> {
                    JLabel placeholder = new JLabel();
                    placeholder.setOpaque(false);
                    return placeholder;
                };
            }
        }
        return super.getCellRenderer(row, column);
    }



    /**
     * 不重写扩展逻辑交给 prepareRenderer 处理显示区域
     */
    @Override
    public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
        Rectangle rect = super.getCellRect(row, column, includeSpacing);
        if (spanTableModel != null) {
            CellSpan span = spanTableModel.getSpan(row, column);
            if (span.rowSpan > 1 || span.colSpan > 1) {
                // 跨行累加高度
                for (int i = 1; i < span.rowSpan; i++) {
                    rect.height += getRowHeight(row + i);
                }
                // 跨列累加宽度
                TableColumnModel cm = getColumnModel();
                for (int j = 1; j < span.colSpan; j++) {
                    rect.width += cm.getColumn(column + j).getWidth();
                }
            }
        }
        return rect;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        int row = rowAtPoint(p), col = columnAtPoint(p);
        if (row >= 0 && col >= 0) {
            Object val = getValueAt(row, col);
            if (val != null) {
                return "<html><div style='width:300px;white-space:normal;'>" + val + "</div></html>";
            }
        }
        return null;
    }

    /**
     * 判断这个 (row,col) 是否是一个合并块的"首格"（anchor）
     */
    public boolean isAnchorCell(int row, int col) {
        CellSpan span = spanTableModel.getSpan(row, col);
        return span != null && span.rowSpan > 0 && span.colSpan > 0
                && span.anchorRow == row && span.anchorCol == col;
    }

    /**
     * 获取"未合并"版的 cell rect，用于非 anchor 的内容定位
     */
    public Rectangle getUnmergedCellRect(int row, int column, boolean includeSpacing) {
        return super.getCellRect(row, column, includeSpacing);
    }
    
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        CellSpan span = spanTableModel.getSpan(row, column);

        // —— 1. 隐藏被合并覆盖的副格 —— 
        if (span != null && (span.rowSpan == 0 || span.colSpan == 0)) {
            JLabel hidden = new JLabel();
            hidden.setOpaque(false);
            hidden.setVisible(false);
            return hidden;
        }

        // 获取原始渲染组件（只调用一次，避免重复渲染）
        Component originalComponent = super.prepareRenderer(renderer, row, column);

        // —— 2. 对于跨行/跨列的anchor单元格，创建背景占位组件 ——
        if (span != null
            && span.anchorRow == row
            && span.anchorCol == column
            && (span.rowSpan > 1 || span.colSpan > 1)) {
            
            // 创建自定义组件：保持原组件的尺寸信息，但只渲染背景
            JComponent backgroundOnly = new JComponent() {
                @Override
                public Dimension getPreferredSize() {
                    return originalComponent.getPreferredSize();
                }
                
                @Override
                public Dimension getMinimumSize() {
                    return originalComponent.getMinimumSize();
                }
                
                @Override
                public Dimension getMaximumSize() {
                    return originalComponent.getMaximumSize();
                }
                
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    
                    // 只绘制背景
                    if (isCellSelected(row, column)) {
                        g.setColor(getSelectionBackground());
                    } else {
                        g.setColor(getBackground());
                    }
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            
            // 复制关键属性以保持正确的尺寸计算
            if (originalComponent instanceof JComponent) {
                JComponent jOriginal = (JComponent) originalComponent;
                backgroundOnly.setBorder(jOriginal.getBorder());
                backgroundOnly.setFont(jOriginal.getFont());
            }
            
            backgroundOnly.setOpaque(true);
            return backgroundOnly;
        }

        // —— 3. 其它情况使用原始渲染 ——
        return originalComponent;
    }


    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component comp = super.prepareEditor(editor, row, column);
        adjustAlignment(comp, row, column);
        return comp;
    }

    private void adjustAlignment(Component comp, int row, int column) {
        CellSpan span = spanTableModel.getSpan(row, column);
        int anchorRow = (span != null ? span.anchorRow : row);
        Rectangle fullRect = getCellRect(anchorRow, column, false);
        int cellHeight = fullRect.height;

        if (comp instanceof JTextArea) {
            JTextArea ta = (JTextArea) comp;
            int colWidth = getColumnModel().getColumn(column).getWidth();
            ta.setSize(colWidth, Short.MAX_VALUE);
            int textHeight = ta.getPreferredSize().height;
            int extra = cellHeight - textHeight;
            int top = Math.max(extra / 2, 0);
            int bottom = Math.max(extra - top, 0);
            ta.setMargin(new Insets(top, 5, bottom, 5));
        } else if (comp instanceof JCheckBox) {
            JCheckBox cb = (JCheckBox) comp;
            cb.setHorizontalAlignment(SwingConstants.CENTER);
            int cbHeight = cb.getPreferredSize().height;
            int extra = cellHeight - cbHeight;
            int top = Math.max(extra / 2, 0);
            int bottom = Math.max(extra - top, 0);
            cb.setBorder(BorderFactory.createEmptyBorder(top, 0, bottom, 0));
        } else if (comp instanceof JLabel) {
            JLabel lb = (JLabel) comp;
            lb.setHorizontalAlignment(SwingConstants.CENTER);
            lb.setVerticalAlignment(SwingConstants.CENTER);
        }
    }



    /**
     * 触发一次全表的行高/重绘刷新（外部调用）
     */
    public void forceRepaint() {
        // 重新计算每行高度（简单版：保留你原本逻辑，可和外面那个 adjustRowHeights 配合）
        revalidate();
        repaint();
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(getGridColor());

        int rows = getRowCount(), cols = getColumnCount();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                CellSpan span = spanTableModel.getSpan(r, c);
                // 只在 anchorCell（合并起点）处理一次
                if (span == null || span.anchorRow != r || span.anchorCol != c) continue;

                // 计算整个合并区域的 bounds（你当前逻辑）
                Rectangle cell = super.getCellRect(r, c, true);
                for (int i = 1; i < span.rowSpan; i++) {
                    cell.height += getRowHeight(r + i);
                }
                TableColumnModel cm = getColumnModel();
                for (int j = 1; j < span.colSpan; j++) {
                    cell.width += cm.getColumn(c + j).getWidth();
                }

                // 1. 画边框
                g2.drawRect(cell.x, cell.y, cell.width - 1, cell.height - 1);

                // 2. 画内容（覆盖默认 renderer 可能分散的视觉）：文本 / checkbox
                drawAnchorCellContent(g2, r, c, cell);
            }
        }
        g2.dispose();
    }
    

    /**
     * 手工在合并区域 cellBounds 里居中绘制该 anchor cell 的 value。
     */
    private void drawAnchorCellContent(Graphics2D g2, int anchorRow, int anchorCol, Rectangle cellBounds) {
        Object value = getValueAt(anchorRow, anchorCol);
        if (value == null) return;

        // 先保存原状态
        Font originalFont = g2.getFont();
        Color originalColor = g2.getColor();

        // 判断是不是 boolean（复选框）
        if (value instanceof Boolean) {
            boolean selected = Boolean.TRUE.equals(value);
            // 用一个临时 JCheckBox 来渲染视觉（但不做事件）
            JCheckBox tmp = new JCheckBox();
            tmp.setSelected(selected);
            tmp.setHorizontalAlignment(SwingConstants.CENTER);
            tmp.setVerticalAlignment(SwingConstants.CENTER);
            tmp.setOpaque(false);
            tmp.setFont(getFont()); // 继承表格字体
            // 计算它的 preferred size
            tmp.setSize(tmp.getPreferredSize());
            Dimension pref = tmp.getPreferredSize();
            int x = cellBounds.x + (cellBounds.width - pref.width) / 2;
            int y = cellBounds.y + (cellBounds.height - pref.height) / 2;
            tmp.setBounds(x, y, pref.width, pref.height);
            // 让它画自己到主 graphics（注意坐标变换）
            Graphics cbg = g2.create(x, y, pref.width, pref.height);
            tmp.paint(cbg);
            cbg.dispose();
        } else {
            // 文本：居中绘制，考虑换行你现在用的是单行渲染器
            String text = value.toString();
            FontMetrics fm = g2.getFontMetrics(getFont());
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();

            int x = cellBounds.x + (cellBounds.width - textWidth) / 2;
            int y = cellBounds.y + (cellBounds.height - textHeight) / 2 + fm.getAscent();

            // 选中背景/字体颜色：可以参考默认 renderer 做简单处理
            g2.setColor(getForeground());
            g2.setFont(getFont());
            g2.drawString(text, x, y);
        }

        // 恢复
        g2.setFont(originalFont);
        g2.setColor(originalColor);
    }

    private static final TableCellRenderer EMPTY_CONTENT_RENDERER = (table, value, isSelected, hasFocus, row, column) -> {
        // 透明不绘制任何内容，只保留背景（isSelected 之类如果需要的话）
        JLabel placeholder = new JLabel();
        placeholder.setOpaque(false);
        return placeholder;
    };
}