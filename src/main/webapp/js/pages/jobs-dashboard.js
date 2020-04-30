(function($) {

    function refresh() {
        $("#runningJobs").load("runningJobs");
        $("#recentlyFinishedJobs").load("recentlyFinishedJobs");

        $.getJSON("api/json", function(jobs) {
            if(jobs.runningJobs.length > 0) {
                window.setTimeout(refresh, 1000);
            }
        });
    }

    $(document).ready(function() {
        window.setTimeout(refresh, 1000);
    });

})(jQuery);