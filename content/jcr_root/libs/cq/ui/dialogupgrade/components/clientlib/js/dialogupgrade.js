$(document).ready(function () {

    $pathTextfield = $(".js-coral-pathbrowser-input", $("#path").closest(".coral-Form-fieldwrapper"));

    $("#show-dialogs").click(function () {
        var path = $pathTextfield.val();
        window.location = window.location.pathname + "?path=" + path;
    });

    // enable enter key for the path field
    $($pathTextfield).keyup(function(event){
        if(event.keyCode == 13){
            $("#show-dialogs").click();
        }
    });

    $("#upgrade-dialogs").click(function () {
        // get paths from table
        var paths = $(".path").map(function () {
            return $(this).text();
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
            var $tbody = $("#upgrade-results tbody");
            for (var path in data) {
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
                $tr.append('<td class="coral-Table-cell centered"><i class="coral-Icon ' + iconClass + '" /></td>');
                $tr.append('<td class="coral-Table-cell">' + message + '</td>');
            }
        }).fail(function () {
            var title = "Error";
            var message = "Call to dialog upgrade servlet failed. Please review the logs.";
            ui.alert(title, message, "error");
        }).always(function () {
            ui.clearWait();
        });
    });

});
