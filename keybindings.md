## Key bindings

The following key bindings are currently supported by the editor :

| Keybinding             | Description                                                                                                                                |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `Ctrl + A`             | Select all.                                                                                                                                |
| `Ctrl + X`             | If no content is selected, cuts the current line. Otherwise, performs the usual cut operation.                                             |
| `Ctrl + C`             | If no content is selected, selects and copies the current line. Otherwise, performs the usual copy operation.                              |
| `Ctrl + V`             | The usual paste action.                                                                                                                    |
| `Ctrl + Z`             | Undo the last action.                                                                                                                      |
| `Ctrl + R`             | Redo the last action.                                                                                                                      |
| `Ctrl + D`             | If content is selected, duplicates the selected content else duplicates the current line.                                                  |
| `Ctrl + W`             | Selects the word at the left selection handle.                                                                                             |
| `Ctrl + Left`          | Move to word start. If the cursor is already at the current word's start, moves the cursor to previous word's start, skipping whitespaces. |
| `Ctrl + Right`         | Move to word end. If the cursor is already at the current word's end, moves the cursor to next word's end, skipping whitespaces.           |
| `Ctrl + Up`            | Scroll up by one row.                                                                                                                      |
| `Ctrl + Down`          | Scroll down by one row.                                                                                                                    |
| `Ctrl + Home`          | Move the cursor to the beginning of content.                                                                                               |
| `Ctrl + End`           | Move the cursor to the end of content.                                                                                                     |
| `Ctrl + PgUp`          | Move the cursor to the page top.                                                                                                           |
| `Ctrl + PgDn`          | Move the cursor to the page bottom.                                                                                                        |
| `Ctrl + Enter`         | Split line. If content is selected, deletes the selected content then splits the line.                                                     |
| `Ctrl + Shift + Left`  | Same as `Ctrl+Left`, but starts or updates the selection.                                                                                  |
| `Ctrl + Shift + Right` | Same as `Ctrl+Right`, but starts or updates the selection.                                                                                 |
| `Ctrl + Shift + Up`    | Move the current line (or all selected lines) up by one line.                                                                              |
| `Ctrl + Shift + Down`  | Move the current line (or all selected lines) down by one line.                                                                            |
| `Ctrl + Shift + Home`  | Same as `Ctrl+Home`, but starts or updates the selection.                                                                                  |
| `Ctrl + Shift + End`   | Same as `Ctrl+End`, but starts or updates the selection.                                                                                   |
| `Ctrl + Shift + PgUp`  | Move the cursor to the page top with selection.                                                                                            |
| `Ctrl + Shift + PgDn`  | Move the cursor to the page bottom with selection.                                                                                         |
| `Ctrl + Alt + Enter`   | Start a new line before current line.                                                                                                      |
| `Ctrl + Shift + J`     | Join current line and next line.                                                                                                           |
| `Shift + Enter`        | Start a new line.                                                                                                                          |
| `Selection + TAB`      | If the text is selected, pressing the `TAB` key indents all selected lines.                                                                |
| `Shift + TAB`          | Unindents the current line. If the text is selected, unindents all the selected lines.                                                     |