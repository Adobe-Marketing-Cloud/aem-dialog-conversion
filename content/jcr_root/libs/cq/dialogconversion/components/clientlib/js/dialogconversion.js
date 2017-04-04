$(document).ready(function () {
    'use strict';

    var REMOTE_DIALOG_CONVERSION_SERVICE_PATH = "/libs/cq/dialogconversion";
    var CORAL_TABLE_ROW_SELECTABLE = 'coral-table-rowselect';

    var ui = $(window).adaptTo("foundation-ui");

    var dialogRows;
    var dialogConverterContainer = document.querySelector(".js-cq-DialogConverter-container");
    var toggleDialogRows = dialogConverterContainer.querySelector(".js-cq-DialogConverter-toggleDialogRows");
    var searchPathField = dialogConverterContainer.querySelector(".js-cq-DialogConverter-searchPath");
    var infoText = dialogConverterContainer.querySelector(".js-cq-DialogConverter-infoText");
    var showDialogsButton = dialogConverterContainer.querySelector(".js-cq-DialogConverter-showDialogs");
    var convertDialogsButton = dialogConverterContainer.querySelector(".js-cq-DialogConverter-convertDialogs");
    var dialogsContainer = dialogConverterContainer.querySelector(".js-cq-DialogConverter-dialogsContainer");
    var conversionResults = dialogConverterContainer.querySelector(".js-cq-DialogConverter-conversionResults");
    var dialogTable = dialogConverterContainer.querySelector(".js-cq-DialogConverter-dialogs");

    function updateRepositoryPathParameter () {
        if (!searchPathField) {
            return;
        }

        var path = searchPathField.value;
        // Set the path query parameter
        window.location = window.location.pathname + "?path=" + path;
    }

    function adjustConvertButton (selectionCount) {
        // hide "convert dialogs" button if no dialogs are selected
        convertDialogsButton.hidden = selectionCount < 1;
        // adjust button label
        convertDialogsButton.textContent = Granite.I18n.get("Convert {0} dialog(s)", selectionCount, "Number of dialogs to be converted");
    }

    function init () {
        dialogRows = dialogTable.items.getAll();

        for (var i = 0, length = dialogRows.length; i < length; i++) {
            var dialogRow = dialogRows[i];

            if (dialogRow.hasAttribute('data-converted')) {
                dialogRow.removeAttribute('selected');
                dialogRow.removeAttribute(CORAL_TABLE_ROW_SELECTABLE);
            } else {
                dialogRow.setAttribute(CORAL_TABLE_ROW_SELECTABLE, true);
            }
        }

        // Prefill pathbrowser with value from the url
        if (searchPathField) {
            searchPathField.value = dialogsContainer.dataset.searchPath;
        }

        infoText.textContent = "";
        if (infoText && dialogRows && dialogRows.length > 0) {
            infoText.textContent = Granite.I18n.get("Found {0} dialog(s)", dialogRows.length);
        }

        /**
         * Click handler for the "show dialogs" button
         */
        showDialogsButton.on("click", updateRepositoryPathParameter);

        dialogTable.on("coral-table:change", function (event) {
            var selectionCount = event && event.detail && event.detail.selection ? event.detail.selection.length : 0;
            adjustConvertButton(selectionCount);

            if (event.target.dataset.toggleAll) {
                return;
            }

            var totalDialogPaths = dialogRows.length;

            // Adjust state of toggle all checkbox
            if (selectionCount > 0 && selectionCount < totalDialogPaths) {
                toggleDialogRows.setAttribute("indeterminate", true);
                toggleDialogRows.checked = false;
            } else {
                toggleDialogRows.removeAttribute("indeterminate");
                toggleDialogRows.checked = selectionCount > 0;
            }
        });

        toggleDialogRows.on("change", function () {
            var i;
            var checkedMixed = !!(toggleDialogRows.getAttribute("indeterminate"));

            if (checkedMixed) {
                this.removeAttribute("indeterminate");
            }

            // Flag the table as being edited by the toggle all checkbox
            dialogTable.dataset.toggleAll = true;

            for (i = 0, length = dialogRows.length; i < length; i++) {
                var dialogPath = dialogRows[i];

                if (dialogPath.hasAttribute('data-converted')) {
                    dialogPath.removeAttribute('selected');
                } else {
                    // if the state is 'mixed', we select the row
                    dialogPath.selected = checkedMixed ? true : this.checked;
                }
            }

            delete dialogTable.dataset.toggleAll;
        });

        convertDialogsButton.on("click", function () {
            // get paths from table
            var paths = [];

            var selectedDialogRows = dialogTable.selectedItems;
            for (var i = 0, length = selectedDialogRows.length; i < length; i++) {
                var value = selectedDialogRows[i].dataset.path;

                if (value) {
                    paths.push(value);
                }
            }

            var url = REMOTE_DIALOG_CONVERSION_SERVICE_PATH + "/content/convert.json";
            var data = {
                paths : paths
            };

            // show overlay and wait
            ui.wait();

            $.post(url, data, function (data) {
                convertDialogsButton.hidden = true;
                dialogsContainer.hidden = true;
                conversionResults.hidden = false;

                // build result table
                var count = 0;
                var successCount = 0;
                var errorCount = 0;

                if (Object.keys(data).length) {
                    conversionResults.items.clear();
                }

                for (var path in data) {
                    count++;

                    // Create a row for the results table
                    var row = new Coral.Table.Row();
                    conversionResults.items.add(row);

                    // Create a cell that will contain the path to the dialog
                    var pathCell = new Coral.Table.Cell();
                    pathCell.textContent = path;

                    row.appendChild(pathCell);

                    var links = "-";
                    var message = Granite.I18n.get("Converted dialog successfully");
                    var resultPath = data[path].resultPath;

                    if (resultPath) {
                        // success
                        successCount++;
                        var crxHref = Granite.HTTP.externalize("/crx/de/index.jsp#" + resultPath.replace(":", "%3A"));
                        links = '<a href="' + crxHref + '" target="_blank" class="coral-Link">crxde</a>';
                    } else {
                        // error
                        errorCount++;
                        message = Granite.I18n.get("Error");

                        if (data[path].errorMessage) {
                            message += ": " + data[path].errorMessage;
                        }
                    }

                    // Create the cell that contains the links
                    var linksCell = new Coral.Table.Cell();
                    row.appendChild(linksCell);
                    linksCell.innerHTML = links;

                    // Add the cell that contains the message
                    var messageCell = new Coral.Table.Cell();
                    messageCell.textContent = message;
                    row.appendChild(messageCell);
                }

                // Change info text
                var text = "Ran dialog conversion on <b>" + count + "</b> dialog" + (count == 1 ? "" : "s") + " ";
                text += "(<b>" + successCount + "</b> successful conversion" + (successCount == 1 ? "" : "s") + ", ";
                text += "<b>" + errorCount + "</b> error" + (errorCount == 1 ? "" : "s") + "):";

                infoText.innerHTML = text;

            }).fail(function () {
                var title = Granite.I18n.get("Error");
                var message = Granite.I18n.get("Call to dialog conversion servlet failed. Please view the logs.");
                ui.alert(title, message, "error");
            }).always(function () {
                ui.clearWait();
            });
        });
    }

    Coral.commons.ready(dialogConverterContainer, init);
});
