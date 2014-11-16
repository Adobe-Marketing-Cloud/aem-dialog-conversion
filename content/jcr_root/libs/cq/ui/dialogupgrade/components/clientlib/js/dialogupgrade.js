$(document).ready(function () {

    $("#show-dialogs").click(function () {
        var path = $(".js-coral-pathbrowser-input", $("#path").closest(".coral-Form-fieldwrapper")).val();
        window.location = window.location.pathname + "?path=" + path;
    });


    $("#upgrade").click(function () {
        // get paths from table
        var paths = $(".path").map(function () {
            return $(this).text();
        }).get();

        var url = "/libs/cq/ui/dialogupgrade/content/upgrade";
        var data = {
            paths : paths
        };
        $.post(url, data, function () {
            alert('success');
        }).fail(function () {
            alert('fail');
        });
    });

});
