$(document).ready(function () {
    'use strict';

    var DIALOG_PATHS = ".js-cq-DialogConverter-path";
    var DIALOG_SEARCH_PATH_FIELD = ".js-cq-DialogConverter-searchPath";
    var REMOTE_DIALOG_CONVERSION_SERVICE_PATH = "/libs/cq/dialogconversion";

    var ARIA_CHECKED_ATTRIBUTE = "aria-checked";

    var ui = $(window).adaptTo("foundation-ui");

    var toggleDialogPaths;
    var dialogConverterContainer = document.querySelector(".js-cq-DialogConverter-container");
    var searchPathField = dialogConverterContainer.querySelector(DIALOG_SEARCH_PATH_FIELD);
    var infoText = dialogConverterContainer.querySelector(".js-cq-DialogConverter-infoText");
    var showDialogsButton = dialogConverterContainer.querySelector(".js-cq-DialogConverter-showDialogs");
    var convertDialogsButton = dialogConverterContainer.querySelector(".js-cq-DialogConverter-convertDialogs");
    var dialogsContainer = dialogConverterContainer.querySelector(".js-cq-DialogConverter-dialogsContainer");
    var conversionResults = dialogConverterContainer.querySelector(".js-cq-DialogConverter-conversionResults");
    var dialogTable = dialogConverterContainer.querySelector(".js-cq-DialogConverter-dialogs");
    var dialogPaths = dialogTable.querySelectorAll(DIALOG_PATHS);

    function updateRepositoryPathParameter () {
        if (!searchPathField) {
            return;
        }

        var path = searchPathField.value;
        // Set the path query parameter
        window.location = window.location.pathname + "?path=" + path;
    }

    function init () {
        var firstTableHeader = dialogTable.querySelector("th:first-child > coral-table-headercell-content");

        if (firstTableHeader) {
            toggleDialogPaths = new Coral.Checkbox();
            toggleDialogPaths.classList.add("js-cq-DialogConverter-toggleDialogPaths");
            firstTableHeader.appendChild(toggleDialogPaths);
        }
        // Prefill pathbrowser with value from the url
        if (searchPathField) {
            searchPathField.value = dialogsContainer.dataset.searchPath;
        }

        infoText.textContent = "";
        if (infoText && dialogPaths && dialogPaths.length > 0) {
            infoText.textContent = Granite.I18n.get("Found {0} dialog(s)", dialogPaths.length);
        }

        /**
         * Click handler for the "show dialogs" button
         */
        showDialogsButton.on("click", updateRepositoryPathParameter);

        /**
         * Keeps track of the time of the last selection of the pathbrowser select list.
         */
        var selectedMillis;
        // var selectSearchList = dialogSearchFormField.querySelector(".coral-SelectList");
        // selectSearchList.on("selected", function () {
        //     selectedMillis = new Date().getTime();
        // });

        /**
         * Enable enter key for the path field
         */
        $(searchPathField).on("keyup", function(event){
            // Enter key
            if (event.keyCode === 13) {
                // if the time between this event and the last selection is small, then we ignore this event
                // (needed to ignore events coming from hitting enter when using the pathbrowser select list)
                if (selectedMillis && (new Date().getTime()) - selectedMillis <= 150) {
                    return;
                }

                updateRepositoryPathParameter();
            }
        });

        var adjustConvertButton = function () {
            var count = dialogTable.querySelectorAll(DIALOG_PATHS + "[checked]").length;
            // hide "convert dialogs" button if no dialogs are selected
            convertDialogsButton.hidden = count < 1;
            // adjust button label
            convertDialogsButton.textContent = Granite.I18n.get("Convert {0} dialog(s)", count, "Number of dialogs to be converted");
        };

        $(dialogPaths).change(function () {
            adjustConvertButton();

            // check if there are checked and / or unchecked checkboxes
            var hasChecked = false;
            var hasUnchecked = false;

            var i = 0;
            do {
                var dialogPath = dialogPaths[i];

                if (dialogPath.disabled) {
                    i++;
                    continue;
                }

                if (dialogPath.checked) {
                    hasChecked = true;
                } else {
                    hasUnchecked = true;
                }

                i++;
            } while (dialogPaths && i < dialogPaths.length && !(hasChecked && hasUnchecked));

            // adjust state of main checkbox
            if (hasChecked && hasUnchecked) {
                toggleDialogPaths.setAttribute(ARIA_CHECKED_ATTRIBUTE, "mixed");
                toggleDialogPaths.setAttribute("indeterminate", true);
                toggleDialogPaths.checked = false;
            } else {
                toggleDialogPaths.setAttribute(ARIA_CHECKED_ATTRIBUTE, false);
                toggleDialogPaths.removeAttribute("indeterminate");
                toggleDialogPaths.checked = hasChecked;
            }
        });

        $(toggleDialogPaths).on("change", function () {
            var i;
            var ariaCheckedMixed = "mixed" === toggleDialogPaths.getAttribute(ARIA_CHECKED_ATTRIBUTE);

            if (ariaCheckedMixed) {
                this.removeAttribute("indeterminate");
                this.setAttribute(ARIA_CHECKED_ATTRIBUTE, false);
            } else {
                this.setAttribute(ARIA_CHECKED_ATTRIBUTE, true);
            }

            for (i = 0, length = dialogPaths.length; i < length; i++) {
                // if the state is 'mixed', we check all checkboxes
                dialogPaths[i].checked = ariaCheckedMixed ? true : this.checked;
            }

            // uncheck disabled checkboxes
            var disabledDialogPaths = dialogTable.querySelectorAll(DIALOG_PATHS + "[disabled]");
            for (i = 0, length = disabledDialogPaths.length; i < length; i++) {
                disabledDialogPaths[i].checked = false;
            }

            // adjust the convert button
            adjustConvertButton();
        });

        convertDialogsButton.on("click", function () {
            // get paths from table
            var paths = [];

            var selectedDialogPaths = dialogTable.querySelectorAll(DIALOG_PATHS + "[checked]");
            for (var i = 0, length = selectedDialogPaths.length; i < length; i++) {
                var value = selectedDialogPaths[i].value;

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
