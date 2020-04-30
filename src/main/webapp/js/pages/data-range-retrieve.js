$(document).ready(function() {
    $('#range-select').click(function() {
        var startdate = $('#startdate').val();
        var enddate = $('#enddate').val();
        window.location = "range/" + "?startDate=" + encodeURIComponent(startdate) + "&endDate=" + encodeURIComponent(enddate);
    });
});