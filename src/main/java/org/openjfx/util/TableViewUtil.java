package org.openjfx.util;

import javafx.beans.binding.Bindings;
import javafx.scene.control.*;

public final class TableViewUtil {
    private TableViewUtil() {}

    /**
     * Apply common row interactions: context menu with export action and double-click to show details.
     * exportAction and detailsAction are run only when a non-empty row is clicked.
     */
    public static <T> void applyRowInteractions(TableView<T> tableView, Runnable exportAction, java.util.function.Consumer<T> detailsAction) {
        tableView.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            MenuItem exportCtx = new MenuItem("Export Certificate…");
            exportCtx.setOnAction(e -> { if (!row.isEmpty() && exportAction != null) exportAction.run(); });
            ContextMenu ctx = new ContextMenu(exportCtx);
            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty())
                    .then((ContextMenu) null)
                    .otherwise(ctx));
            row.setOnMouseClicked(me -> {
                if (me.getClickCount() == 2 && !row.isEmpty() && detailsAction != null) {
                    detailsAction.accept(row.getItem());
                }
            });
            return row;
        });
    }
}
