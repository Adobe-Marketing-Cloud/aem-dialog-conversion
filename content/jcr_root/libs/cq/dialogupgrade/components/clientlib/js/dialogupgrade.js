$(document).ready(function () {

    var basePath = "/libs/cq/dialogupgrade";

    var $pathTextfield = $(".js-coral-pathbrowser-input", $("#path").closest(".coral-Form-fieldwrapper"));
    var $showDialogsButton = $("#show-dialogs");
    var $upgradeDialogsButton = $("#upgrade-dialogs");
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

    var adjustUpgradeButton = function () {
        var count = $(".path:checked").length;
        // hide "upgrade dialogs" button if no dialogs are selected
        $upgradeDialogsButton.toggle(count > 0);
        // adjust button label
        var label = "Upgrade " + count + " dialogs";
        if ($("span", $upgradeDialogsButton).length) {
            // 6.0
            $("span", $upgradeDialogsButton).text(label);
        } else {
            // 6.1
            $upgradeDialogsButton.text(label);
        }
    };

    $(".path").change(function () {
        adjustUpgradeButton();

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
        // adjust the upgrade button
        adjustUpgradeButton();
    });


    $upgradeDialogsButton.click(function () {
        // get paths from table
        var paths = $(".path:checked").map(function () {
            return $(this).val();
        }).get();

        var url = basePath + "/content/upgrade.json";
        var data = {
            paths : paths
        };

        // show overlay and wait
        var ui = $(window).adaptTo("foundation-ui");
        ui.wait();

        $.post(url, data, function (data) {
            $("#dialogs-container").remove();
            $upgradeDialogsButton.remove();
            $("#upgrade-results").show();

            // build result table
            var count = 0;
            var $tbody = $("#upgrade-results tbody");
            for (var path in data) {
                count++;
                var $tr = $('<tr class="coral-Table-row"></tr>').appendTo($tbody);
                $tr.append('<td class="coral-Table-cell">' + path + '</td>');
                var result = data[path].result;
                var iconClass = "coral-Icon--check";
                var message = Granite.I18n.get("Upgraded dialog successfully");
                if (result == "ERROR") {
                    iconClass = "coral-Icon--close";
                    if (data[path].message) {
                        message = Granite.I18n.get("Upgrade failed:") + " " + data[path].message;
                    }
                } else if (result == "ALREADY_UPGRADED") {
                    iconClass = "coral-Icon--exclude";
                    message = Granite.I18n.get("Skipped (Touch UI dialog already exists)");
                }
                var links = "";
                if (data[path].path) {
                    var href = Granite.HTTP.externalize(basePath + "/content/render.html" + data[path].path);
                    var crxHref = Granite.HTTP.externalize("/crx/de/index.jsp#" + data[path].path.replace(":", "%3A"));
                    links += '<a href="' + href + '" target="_blank" class="coral-Link">show</a> / <a href="' + crxHref + '" target="_blank" class="coral-Link">crxde</a>';
                }
                $tr.append('<td class="coral-Table-cell centered"><i class="coral-Icon ' + iconClass + '" />' + links + '</td>');
                $tr.append('<td class="coral-Table-cell">' + message + '</td>');
            }

            // change info text
            $("#info-text").empty();
            $("#info-text").append("Ran upgrade procedure on <b>" + count + "</b> dialogs:");
        }).fail(function () {
            var title = "Error";
            var message = "Call to dialog upgrade servlet failed. Please review the logs.";
            ui.alert(title, message, "error");
        }).always(function () {
            ui.clearWait();
        });
    });

});
