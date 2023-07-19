# Line Number Panel

The editor provides APIs to control the position of the line number hint panel for you to use.

!> The default position is middle aligned with the scrollbar when not specified.

## Relative Positioning

When the positioning strategy of the line number panel is set to `follow`, the line number panel
will follow the scrollbar. In this case, you can specify whether it should follow above, below or in
the middle of the scrollbar.

```
app:lnPanelPositionMode="follow"
app:lnPanelPosition="center" <!-- top/center/bottom -->
```

## Absolute Positioning

When the positioning strategy of the line number panel is set to `fixed`, the line number panel will
be fixed at the specified position in the editor.

```
app:lnPanelPositionMode="fixed"
app:lnPanelPosition="top|center" <!-- Will be displayed at the top middle of the editor -->
```

Options for lnPanelPosition:

- top
- bottom
- left
- right
- center