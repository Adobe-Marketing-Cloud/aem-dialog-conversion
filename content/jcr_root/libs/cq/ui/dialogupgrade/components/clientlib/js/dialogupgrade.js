$(document).ready(function () {

    $("#show-dialogs").click(function () {
        var path = $(".js-coral-pathbrowser-input", $("#path").closest(".coral-Form-fieldwrapper")).val();
        window.location = window.location.pathname + "?path=" + path;
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
        // todo: wait popup
        $.post(url, data, function (data) {
            $("#dialogs").remove();
            $("#upgrade-dialogs").remove();
            $("#upgrade-results").show();

            var $tbody = $("#upgrade-results tbody");
            for (var path in data) {
                var $tr = $('<tr class="coral-Table-row"></tr>').appendTo($tbody);
                $tr.append('<td class="coral-Table-cell">' + path + '</td>');
                var result = data[path].result;
                $tr.append('<td class="coral-Table-cell centered">' + result + '</td>');
            }
        }).fail(function () {
            alert('Error');
        });
    });

});
