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

    // —— 2. 对于跨行/跨列的anchor单元格，需要特殊处理 ——
    if (span != null
        && span.anchorRow == row
        && span.anchorCol == column
        && (span.rowSpan > 1 || span.colSpan > 1)) {
        
        // 先获取原始渲染器组件以获取正确的尺寸信息
        Component originalComponent = super.prepareRenderer(renderer, row, column);
        
        // 创建一个包装器，只显示背景但保持原始组件的尺寸特性
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BorderLayout());
        wrapper.setOpaque(true);
        
        // 设置正确的背景色
        if (isCellSelected(row, column)) {
            wrapper.setBackground(getSelectionBackground());
            wrapper.setForeground(getSelectionForeground());
        } else {
            wrapper.setBackground(getBackground());
            wrapper.setForeground(getForeground());
        }
        
        // 创建一个透明的组件来保持尺寸，但不显示内容
        JLabel sizeKeeper = new JLabel();
        sizeKeeper.setOpaque(false);
        
        // 如果原始组件是JLabel，复制其文本来保持宽度计算
        if (originalComponent instanceof JLabel) {
            JLabel originalLabel = (JLabel) originalComponent;
            sizeKeeper.setText(originalLabel.getText());
            sizeKeeper.setFont(originalLabel.getFont());
            sizeKeeper.setIcon(originalLabel.getIcon());
            // 设置文本颜色为透明，这样文本不可见但仍占用空间
            sizeKeeper.setForeground(new Color(0, 0, 0, 0));
        }
        // 如果原始组件是复选框，保持其尺寸
        else if (originalComponent instanceof JCheckBox) {
            JCheckBox originalCheckBox = (JCheckBox) originalComponent;
            JCheckBox invisibleCheckBox = new JCheckBox();
            invisibleCheckBox.setSelected(originalCheckBox.isSelected());
            invisibleCheckBox.setText(originalCheckBox.getText());
            invisibleCheckBox.setFont(originalCheckBox.getFont());
            // 使复选框透明但保持尺寸
            invisibleCheckBox.setOpaque(false);
            invisibleCheckBox.setForeground(new Color(0, 0, 0, 0));
            invisibleCheckBox.setContentAreaFilled(false);
            invisibleCheckBox.setBorderPainted(false);
            sizeKeeper = invisibleCheckBox;
        }
        
        wrapper.add(sizeKeeper, BorderLayout.CENTER);
        return wrapper;
    }

    // —— 3. 其它（包括普通单元格 & 1×1 跨度的 anchor）都用默认渲染 —— 
    return super.prepareRenderer(renderer, row, column);
}