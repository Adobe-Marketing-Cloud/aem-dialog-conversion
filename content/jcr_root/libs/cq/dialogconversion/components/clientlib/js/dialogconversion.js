$(document).ready(function () {

    var basePath = "/libs/cq/dialogconversion";

    var $pathTextfield = $(".js-coral-pathbrowser-input", $("#path").closest(".coral-Form-fieldwrapper"));
    var $showDialogsButton = $("#show-dialogs");
    var $convertDialogsButton = $("#convert-dialogs");
    var $checkAll = $("#check-all");

    /**
     * Click handler for the "show dialogs" button
     */
    $showDialogsButton.click(function () {
        var path = $pathTextfield.val();
        window.location = window.location.pathname + "?path=" + path;
    });

    /**
     * Keeps track of the time of the last selection of the pathbrowser select list.
     */
    var selectedMillis;
    var selectList = $(".coral-SelectList", $("#path").closest(".coral-Form-fieldwrapper")).data("selectList");
    selectList.on("selected", function (e) {
        selectedMillis = new Date().getTime();
    });

    /**
     * Enable enter key for the path field
     */
    $($pathTextfield).keyup(function(event){
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
        var count = $(".path:checked").length;
        // hide "convert dialogs" button if no dialogs are selected
        $convertDialogsButton.toggle(count > 0);
        // adjust button label
        var label = "Convert " + count + " dialog" + (count > 1 ? "s" : "");
        if ($("span", $convertDialogsButton).length) {
            // 6.0
            $("span", $convertDialogsButton).text(label);
        } else {
            // 6.1
            $convertDialogsButton.text(label);
        }
    };

    $(".path").change(function () {
        adjustConvertButton();

        // check if there are checked and / or unchecked checkboxes
        var hasChecked = false;
        var hasUnchecked = false;
        $(".path").each(function() {
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
            $checkAll.attr("aria-checked", "mixed").prop({
                "indeterminate": true,
                "checked": false
            });
        } else {
            $("#check-all").prop({
                "indeterminate": false,
                "checked": hasChecked
            }).removeAttr("aria-checked");
        }
    });

    $checkAll.change(function () {
        var ariaChecked = $(this).attr("aria-checked");
        if (ariaChecked === "mixed") {
            $(this).indeterminate = false;
            $(this).removeAttr("aria-checked");
            // if the state is 'mixed', we check all checkboxes
            $(".path").prop("checked", true);
        } else {
            $(".path").prop("checked", $(this).prop("checked"));
        }
        // uncheck disabled checkboxes
        $(".path[disabled=disabled]").prop("checked", false);
        // adjust the convert button
        adjustConvertButton();
    });


    $convertDialogsButton.click(function () {
        // get paths from table
        var paths = $(".path:checked").map(function () {
            return $(this).val();
        }).get();

        var url = basePath + "/content/convert.json";
        var data = {
            paths : paths
        };

        // show overlay and wait
        var ui = $(window).adaptTo("foundation-ui");
        ui.wait();

        $.post(url, data, function (data) {
            $("#dialogs-container").remove();
            $convertDialogsButton.remove();
            $("#conversion-results").show();

            // build result table
            var count = 0;
            var $tbody = $("#conversion-results tbody");
            for (var path in data) {
                count++;
                var $tr = $('<tr class="coral-Table-row"></tr>').appendTo($tbody);
                $tr.append('<td class="coral-Table-cell">' + path + '</td>');
                var links = "";
                var iconClass = "coral-Icon--check";
                var message = Granite.I18n.get("Converted dialog successfully");
                var resultPath = data[path].resultPath;
                if (resultPath) {
                    var href = Granite.HTTP.externalize(basePath + "/content/render.html" + resultPath);
                    var crxHref = Granite.HTTP.externalize("/crx/de/index.jsp#" + resultPath.replace(":", "%3A"));
                    links += '<a href="' + href + '" target="_blank" class="coral-Link">show</a> / <a href="' + crxHref + '" target="_blank" class="coral-Link">crxde</a>';
                } else {
                    iconClass = "coral-Icon--close";
                    message = Granite.I18n.get("Error");
                    if (data[path].errorMessage) {
                        message += ": " + data[path].errorMessage;
                    }
                }
                $tr.append('<td class="coral-Table-cell centered"><i class="coral-Icon ' + iconClass + '" />' + links + '</td>');
                $tr.append('<td class="coral-Table-cell">' + message + '</td>');
            }

            // change info text
            $("#info-text").empty();
            $("#info-text").append("Ran conversion on <b>" + count + "</b> dialog" + (count > 1 ? "s" : "") + ":");
        }).fail(function () {
            var title = "Error";
            var message = "Call to dialog conversion servlet failed. Please view the logs.";
            ui.alert(title, message, "error");
        }).always(function () {
            ui.clearWait();
        });
    });

});
