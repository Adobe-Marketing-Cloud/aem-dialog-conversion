$(document).ready(function () {

    $pathTextfield = $(".js-coral-pathbrowser-input", $("#path").closest(".coral-Form-fieldwrapper"));

    /**
     * Click handler for the "show dialogs" button
     */
    $("#show-dialogs").click(function () {
        var path = $pathTextfield.val();
        window.location = window.location.pathname + "?path=" + path;
    });

    /**
     * Enable enter key for the path field
     */
    $($pathTextfield).keyup(function(event){
        if(event.keyCode == 13){
            $("#show-dialogs").click();
        }
    });

    /**
     * Delegate clicks on table rows to checkbox.
     */
    $("td.dialog-cell").parent().click(function (e) {
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

    $(".path").change(function () {
        var count = $(".path:checked").length;
        // hide "upgrade dialogs" button if no dialogs are selected
        $("#upgrade-dialogs").toggle(count > 0);
        // adjust button text
        $("#upgrade-dialogs span").text("Upgrade " + count + " dialogs");
    });


    $("#upgrade-dialogs").click(function () {
        // get paths from table
        var paths = $(".path:checked").map(function () {
            return $(this).val();
        }).get();

        var url = "/libs/cq/ui/dialogupgrade/content/upgrade.json";
        var data = {
            paths : paths
        };

        // show overlay and wait
        var ui = $(window).adaptTo("foundation-ui");
        ui.wait();

        $.post(url, data, function (data) {
            $("#dialogs").remove();
            $("#upgrade-dialogs").remove();
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
                    var href = Granite.HTTP.externalize("/libs/cq/ui/dialogupgrade/content/render.html" + data[path].path);
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
