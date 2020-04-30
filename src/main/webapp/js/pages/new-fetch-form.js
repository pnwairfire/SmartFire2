$(document).ready(function() {
    hideAllForms();

    $('#newFetchLink').click(function(e) {
        e.preventDefault();
        hideAllForms();
        $("#newFetchForm").show();
    });
    
    $('#newUploadLink').click(function(e) {
        e.preventDefault();
        hideAllForms();
        $("#newUploadForm").show();
    });
});

function hideAllForms() {
    $('#forms').children('div').each(function(i) {
        $(this).hide();
    });
}