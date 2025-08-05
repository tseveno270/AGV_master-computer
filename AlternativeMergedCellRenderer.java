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

    // 获取原始渲染组件
    Component originalComponent = super.prepareRenderer(renderer, row, column);

    // —— 2. 对于跨行/跨列的anchor单元格，创建只显示背景的版本 ——
    if (span != null
        && span.anchorRow == row
        && span.anchorCol == column
        && (span.rowSpan > 1 || span.colSpan > 1)) {
        
        // 创建一个新的组件，继承原组件的所有属性但清空显示内容
        Component backgroundOnly = createBackgroundOnlyComponent(originalComponent, row, column);
        return backgroundOnly;
    }

    // —— 3. 其它情况使用原始渲染 ——
    return originalComponent;
}

/**
 * 创建一个只显示背景的组件，保持原组件的尺寸属性
 */
private Component createBackgroundOnlyComponent(Component original, int row, int column) {
    if (original instanceof JLabel) {
        JLabel originalLabel = (JLabel) original;
        JLabel bgLabel = new JLabel();
        
        // 复制尺寸相关属性
        bgLabel.setFont(originalLabel.getFont());
        bgLabel.setBorder(originalLabel.getBorder());
        bgLabel.setHorizontalAlignment(originalLabel.getHorizontalAlignment());
        bgLabel.setVerticalAlignment(originalLabel.getVerticalAlignment());
        
        // 设置透明文本以保持宽度，但不显示
        String text = originalLabel.getText();
        if (text != null && !text.isEmpty()) {
            bgLabel.setText(text);
            bgLabel.setForeground(new Color(0, 0, 0, 0)); // 完全透明
        }
        
        // 复制图标但设为透明
        if (originalLabel.getIcon() != null) {
            // 创建透明图标保持空间占用
            Icon originalIcon = originalLabel.getIcon();
            bgLabel.setIcon(createTransparentIcon(originalIcon));
        }
        
        bgLabel.setOpaque(true);
        setBackgroundColors(bgLabel, row, column);
        return bgLabel;
        
    } else if (original instanceof JCheckBox) {
        JCheckBox originalCheckBox = (JCheckBox) original;
        JCheckBox bgCheckBox = new JCheckBox();
        
        // 复制属性但不显示内容
        bgCheckBox.setFont(originalCheckBox.getFont());
        bgCheckBox.setBorder(originalCheckBox.getBorder());
        bgCheckBox.setText(originalCheckBox.getText());
        
        // 隐藏复选框和文本的显示
        bgCheckBox.setContentAreaFilled(false);
        bgCheckBox.setBorderPainted(false);
        bgCheckBox.setFocusPainted(false);
        bgCheckBox.setForeground(new Color(0, 0, 0, 0));
        
        bgCheckBox.setOpaque(true);
        setBackgroundColors(bgCheckBox, row, column);
        return bgCheckBox;
        
    } else {
        // 对于其他类型的组件，创建一个具有相同尺寸的空白组件
        JPanel bgPanel = new JPanel();
        bgPanel.setPreferredSize(original.getPreferredSize());
        bgPanel.setMinimumSize(original.getMinimumSize());
        bgPanel.setMaximumSize(original.getMaximumSize());
        bgPanel.setOpaque(true);
        setBackgroundColors(bgPanel, row, column);
        return bgPanel;
    }
}

/**
 * 设置背景和前景色
 */
private void setBackgroundColors(JComponent component, int row, int column) {
    if (isCellSelected(row, column)) {
        component.setBackground(getSelectionBackground());
        // 注意：前景色可能不需要设置，因为我们不显示文本
    } else {
        component.setBackground(getBackground());
    }
}

/**
 * 创建透明图标以保持空间占用
 */
private Icon createTransparentIcon(Icon original) {
    return new Icon() {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            // 不绘制任何内容，只占用空间
        }

        @Override
        public int getIconWidth() {
            return original.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return original.getIconHeight();
        }
    };
}