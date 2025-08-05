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