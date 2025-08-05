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
        return new BackgroundOnlyComponent(originalComponent, this, row, column);
    }

    // —— 3. 其它情况使用原始渲染 ——
    return originalComponent;
}

/**
 * 自定义组件：保持原组件的所有尺寸属性，但只绘制背景
 * 这样既避免了重复渲染，又保持了正确的自适应宽度
 */
private static class BackgroundOnlyComponent extends JComponent {
    private final Component originalComponent;
    private final JTable table;
    private final int row;
    private final int column;
    
    public BackgroundOnlyComponent(Component original, JTable table, int row, int column) {
        this.originalComponent = original;
        this.table = table;
        this.row = row;
        this.column = column;
        
        // 复制原组件的所有尺寸和布局属性
        copyComponentProperties(original);
        setOpaque(true);
    }
    
    /**
     * 复制原组件的关键属性以保持正确的尺寸计算
     */
    private void copyComponentProperties(Component original) {
        // 复制尺寸信息
        setPreferredSize(original.getPreferredSize());
        setMinimumSize(original.getMinimumSize());
        setMaximumSize(original.getMaximumSize());
        
        // 如果原组件是JComponent，复制更多属性
        if (original instanceof JComponent) {
            JComponent jOriginal = (JComponent) original;
            setBorder(jOriginal.getBorder());
            
            // 复制字体信息（影响文本宽度计算）
            if (jOriginal.getFont() != null) {
                setFont(jOriginal.getFont());
            }
        }
        
        // 特别处理不同类型的组件以确保尺寸计算正确
        if (original instanceof JLabel) {
            JLabel label = (JLabel) original;
            // 通过设置不可见的文本来保持宽度计算，但不显示
            putClientProperty("original.text", label.getText());
            putClientProperty("original.icon", label.getIcon());
        } else if (original instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) original;
            putClientProperty("original.text", checkBox.getText());
            putClientProperty("original.selected", checkBox.isSelected());
        }
    }
    
    @Override
    public Dimension getPreferredSize() {
        // 确保返回原组件的首选尺寸
        Dimension originalSize = originalComponent.getPreferredSize();
        return originalSize != null ? new Dimension(originalSize) : super.getPreferredSize();
    }
    
    @Override
    public Dimension getMinimumSize() {
        Dimension originalSize = originalComponent.getMinimumSize();
        return originalSize != null ? new Dimension(originalSize) : super.getMinimumSize();
    }
    
    @Override
    public Dimension getMaximumSize() {
        Dimension originalSize = originalComponent.getMaximumSize();
        return originalSize != null ? new Dimension(originalSize) : super.getMaximumSize();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 只绘制背景，不绘制内容
        Graphics2D g2d = (Graphics2D) g.create();
        
        // 设置背景色
        if (table.isCellSelected(row, column)) {
            g2d.setColor(table.getSelectionBackground());
        } else {
            g2d.setColor(table.getBackground());
        }
        
        // 填充背景
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // 如果需要边框，可以从原组件复制
        if (getBorder() != null) {
            getBorder().paintBorder(this, g2d, 0, 0, getWidth(), getHeight());
        }
        
        g2d.dispose();
    }
    
    /**
     * 重写此方法以确保尺寸计算时考虑文本内容，但实际不显示
     */
    @Override
    public FontMetrics getFontMetrics(Font font) {
        // 使用原组件的字体度量，确保文本宽度计算正确
        if (originalComponent instanceof JComponent) {
            return ((JComponent) originalComponent).getFontMetrics(font);
        }
        return super.getFontMetrics(font);
    }
}