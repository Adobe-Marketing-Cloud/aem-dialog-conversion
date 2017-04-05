$(document).ready(function () {
    'use strict';

    var REMOTE_DIALOG_CONVERSION_SERVICE_PATH = "/libs/cq/dialogconversion";

    var ui = $(window).adaptTo("foundation-ui");

    var dialogRows;
    var convertedRows = [];
    var dialogConverterContainer = document.querySelector(".js-cq-DialogConverter-container");
    var showConverted = document.querySelector(".js-cq-DialogConverter-showConvertedDialogRows");
    var searchPathField = document.querySelector(".js-cq-DialogConverter-searchPath");
    var infoText = document.querySelector(".js-cq-DialogConverter-infoText");
    var showDialogsButton = document.querySelector(".js-cq-DialogConverter-showDialogs");
    var convertDialogsButton = document.querySelector(".js-cq-DialogConverter-convertDialogs");
    var dialogSearch = document.querySelector(".js-cq-DialogConverter-dialogSearch");
    var convertedDialogs = document.querySelector(".js-cq-DialogConverter-convertedDialogs");
    var dialogTable = document.querySelector(".js-cq-DialogConverter-dialogs");

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
            var row = dialogRows[i];

            if (row.hasAttribute("data-dialogconversion-dialog-converted")) {
                convertedRows.push(row);
            }
        }

        // Prefill pathfield with value from the url
        if (searchPathField) {
            searchPathField.value = dialogSearch.dataset["dialogconversionSearchPath"];
        }

        if (infoText) {
          infoText.textContent = "";

          if (dialogRows && dialogRows.length > 0) {
            if (dialogRows.length === 1 && !dialogRows[0].dataset["dialogconversionDialogPath"]) {
              // The empty row, hide the infoText
              infoText.setAttribute("hidden", true);
            } else {
              infoText.textContent = Granite.I18n.get("Found {0} dialog(s)", dialogRows.length);
            }
          }
        }

        showDialogsButton.on("click", updateRepositoryPathParameter);

        dialogTable.on("coral-table:change", function (event) {
            var selection = event && event.detail && event.detail.selection ? event.detail.selection : [];
            var count = selection.length;

            // Deselect already converted dialogs
            for (var i = 0; i < selection.length; i++) {
                var row = selection[i];
                if (row.hasAttribute("data-dialogconversion-dialog-converted")) {
                    // Don't trigger too many events
                    row.set('selected', false, true);
                    count--;
                }
            }

            adjustConvertButton(count);
        });

        showConverted.on("change", function () {
            for (var i = 0, length = convertedRows.length; i < length; i++) {
                var row = convertedRows[i];

                if (showConverted.checked) {
                    row.removeAttribute("hidden");
                } else {
                    row.setAttribute("hidden", true);
                }
            }
        });

        convertDialogsButton.on("click", function () {
            // get paths from table
            var paths = [];

            var selectedDialogRows = dialogTable.selectedItems;
            for (var i = 0, length = selectedDialogRows.length; i < length; i++) {
                var value = selectedDialogRows[i].dataset["dialogconversionDialogPath"];

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
                dialogSearch.hidden = true;
                convertedDialogs.hidden = false;

                // build result table
                var count = 0;
                var successCount = 0;
                var errorCount = 0;

                if (Object.keys(data).length) {
                    convertedDialogs.items.clear();
                }

                for (var path in data) {
                    count++;

                    // Create a row for the results table
                    var row = new Coral.Table.Row();
                    convertedDialogs.items.add(row);

                    // Create a cell that will contain the path to the dialog
                    var pathCell = new Coral.Table.Cell();
                    pathCell.textContent = path;

                    row.appendChild(pathCell);

                    var links = "-";
                    var message = Granite.I18n.get("Successfully converted dialog");
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
