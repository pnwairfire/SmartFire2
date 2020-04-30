function enableCronFetch() {
    $("#fetchCron").removeAttr('disabled');
}

function disableCronFetch() {
    $("#fetchCron").attr('disabled', 'disabled');
    $("#fetchCron").text("");
}