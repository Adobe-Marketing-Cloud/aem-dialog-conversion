$(document).ready(function () {
    'use strict';

    var DIALOG_PATHS = ".js-cq-DialogConverter-path";

    var basePath = "/libs/cq/dialogconversion";

    var $window = $(window);
    var $dialogSearchPath = $(".js-cq-DialogConverter-searchPath");
    var $dialogSearchFormField = $dialogSearchPath.closest(".coral-Form-fieldwrapper");
    var conversionResults = document.querySelector(".js-cq-DialogConverter-conversionResults");
    var $infoText = $(".js-cq-DialogConverter-infoText");
    var $dialogPaths = $(DIALOG_PATHS);
    var dialogsContainer = document.querySelector(".js-cq-DialogConverter-dialogsContainer");

    var $pathTextfield = $(".js-coral-pathbrowser-input", $dialogSearchFormField);
    var $showDialogsButton = $(".js-cq-DialogConverter-showDialogs");

    var $convertDialogsButton = $(".js-cq-DialogConverter-convertDialogs");
    var toggleDialogPaths = document.querySelector(".js-cq-DialogConverter-toggleDialogPaths");

    // Prefill pathbrowser with value from the url
    $(".js-coral-pathbrowser-input", $dialogSearchFormField).val(dialogsContainer.dataset.searchPath);

    /**
     * Click handler for the "show dialogs" button
     */
    $showDialogsButton.click(function () {
        var path = $pathTextfield.val();
        // Set the path query parameter
        window.location = window.location.pathname + "?path=" + path;
    });

    /**
     * Keeps track of the time of the last selection of the pathbrowser select list.
     */
    var selectedMillis;
    var $selectList = $(".coral-SelectList", $dialogSearchFormField).data("selectList");
    $selectList.on("selected", function (e) {
        selectedMillis = new Date().getTime();
    });

    /**
     * Enable enter key for the path field
     */
    $pathTextfield.keyup(function(event){
        // Enter key
        if (event.keyCode == 13) {
            // if the time between this event and the last selection is small, then we ignore this event
            // (needed to ignore events coming from hitting enter when using the pathbrowser select list)
            if (selectedMillis && (new Date().getTime()) - selectedMillis <= 150) {
                return;
            }

            $showDialogsButton.click();
        }
    });

    /**
     * Delegate clicks on table rows to checkbox.
     */
    $("#dialogs td").parent().click(function (e) {
        // handles clicks on table rows
        $(".coral-Checkbox", this).click();
    });

    /**
     * Prevent events from bubbling down to the table row when clicking
     * on the checkboxes or links in the table.
     */
    $("#dialogs td a, .coral-Checkbox").click(function (e) {
        e.stopPropagation();
    });

    var adjustConvertButton = function () {
        var count = document.querySelectorAll(DIALOG_PATHS + ":checked").length;
        // hide "convert dialogs" button if no dialogs are selected
        $convertDialogsButton.toggle(count > 0);
        // adjust button label
        var label = "Convert " + count + " dialog" + (count == 1 ? "" : "s");
        if ($("span", $convertDialogsButton).length) {
            // 6.0
            $("span", $convertDialogsButton).text(label);
        } else {
            // 6.1
            $convertDialogsButton.text(label);
        }
    };

    $dialogPaths.change(function () {
        adjustConvertButton();

        // check if there are checked and / or unchecked checkboxes
        var hasChecked = false;
        var hasUnchecked = false;
        $dialogPaths.each(function() {
            if ($(this).prop("disabled")) {
                return true;
            }
            if ($(this).prop("checked")) {
                hasChecked = true;
            } else {
                hasUnchecked = true;
            }
            if (hasChecked && hasUnchecked) {
                return false;
            }
        });
        // adjust state of main checkbox
        if (hasChecked && hasUnchecked) {
            toggleDialogPaths.setAttribute("aria-checked", "mixed");
            toggleDialogPaths.setAttribute("indeterminate", true);
            toggleDialogPaths.checked = false;
        } else {
            toggleDialogPaths.removeAttribute("aria-checked");
            toggleDialogPaths.removeAttribute("indeterminate");
            toggleDialogPaths.checked = hasChecked;
        }
    });

    Coral.commons.ready(toggleDialogPaths, function () {
        toggleDialogPaths.on("change", function () {
            var ariaChecked = toggleDialogPaths.getAttribute("aria-checked");
            if (ariaChecked === "mixed") {
                this.removeAttribute("indeterminate");
                this.removeAttribute("aria-checked");
                // if the state is 'mixed', we check all checkboxes
                $dialogPaths.prop("checked", true);
            } else {
                $dialogPaths.prop("checked", this.checked);
            }
            // uncheck disabled checkboxes
            $(DIALOG_PATHS + "[disabled=disabled]").prop("checked", false);
            // adjust the convert button
            adjustConvertButton();
        });
    });

    $convertDialogsButton.click(function () {
        // get paths from table
        var paths = $(DIALOG_PATHS + ":checked").map(function () {
            return $(this).val();
        }).get();

        var url = basePath + "/content/convert.json";
        var data = {
            paths : paths
        };

        // show overlay and wait
        var ui = $window.adaptTo("foundation-ui");
        ui.wait();

        $.post(url, data, function (data) {
            $convertDialogsButton.remove();
            dialogsContainer.parentNode.removeChild(dialogsContainer);
            conversionResults.style.display = "table";

            // build result table
            var count = 0;
            var successCount = 0;
            var errorCount = 0;
            var tableBody = conversionResults.querySelector("tbody");

            for (var path in data) {
                count++;

                // Create a row for the results table
                var row = document.createElement("tr", {is: "coral-table-row"});
                tableBody.appendChild(row);

                // Create a cell that will contain the path to the dialog
                var pathCell = document.createElement("td", {is: "coral-table-cell"});
                pathCell.textContent = path;

                row.appendChild(pathCell);

                var links = "";
                var iconType = "check";
                var message = Granite.I18n.get("Converted dialog successfully");
                var resultPath = data[path].resultPath;

                if (resultPath) {
                    // success
                    successCount++;
                    var href = Granite.HTTP.externalize(basePath + "/content/render.html" + resultPath);
                    var crxHref = Granite.HTTP.externalize("/crx/de/index.jsp#" + resultPath.replace(":", "%3A"));
                    links += '<a href="' + href + '" target="_blank" class="coral-Link">show</a>&nbsp;/&nbsp;<a href="' + crxHref + '" target="_blank" class="coral-Link">crxde</a>';
                } else {
                    // error
                    errorCount++;
                    iconType = "close";
                    message = Granite.I18n.get("Error");

                    if (data[path].errorMessage) {
                        message += ": " + data[path].errorMessage;
                    }
                }

                // Create the cell that contains the links
                var linksCell = document.createElement("td", {is: "coral-table-cell"});
                row.appendChild(linksCell);
                linksCell.innerHTML = links;

                // Add the icon
                var iconElement = new Coral.Icon();
                iconElement.setAttribute("icon", iconType);
                linksCell.insertBefore(iconElement, linksCell.firstChild);

                // Add the cell that contains the message
                var messageCell = document.createElement("td", {is: "coral-table-cell"});
                messageCell.textContent = message;
                row.appendChild(messageCell);
            }

            // change info text
            $infoText.empty();
            var text = "Ran dialog conversion on <b>" + count + "</b> dialog" + (count == 1 ? "" : "s") + " ";
            text += "(<b>" + successCount + "</b> successful conversion" + (successCount == 1 ? "" : "s") + ", ";
            text += "<b>" + errorCount + "</b> error" + (errorCount == 1 ? "" : "s") + "):";
            $infoText.append(text);
        }).fail(function () {
            var title = Granite.I18n.get("Error");
            var message = Granite.I18n.get("Call to dialog conversion servlet failed. Please view the logs.");
            ui.alert(title, message, "error");
        }).always(function () {
            ui.clearWait();
        });
    });
});
