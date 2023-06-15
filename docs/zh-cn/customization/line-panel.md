# 行号提示面板

编辑器提供了控制行号提示面板位置的API供您使用。

!> 当未规定位置时，默认将跟随在滚动条的中间

## 相对定位

当设置行号面板的定位策略为`相对定位`时，行号面板将会跟随滚动条。在此情况下，您可以规定它是跟随在滚动条的上方、下方还是中间。

```
app:lnPanelPositionMode="follow"
app:lnPanelPosition="center" <!-- top/center/bottom -->
```

## 绝对定位

当设置行号面板的定位策略为`绝对定位`时，行号面板固定在编辑器的指定位置。

```
app:lnPanelPositionMode="fixed"
app:lnPanelPosition="top|center" <!-- 将显示在编辑器上方正中间的位置 -->
```

lnPanelPosition可选项：

- top
- bottom
- left
- right
- center