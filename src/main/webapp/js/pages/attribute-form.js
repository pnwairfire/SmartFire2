$(document).ready(function() {
    hideAll();
    showSelected();

    $('#ingestMethod').click(function() {
        hideAll();
        showSelected();
    });

    $('#clumpMethod').click(function() {
        hideAll();
        showSelected();
    });

    $('#fetchMethod').click(function() {
        hideAll();
        showSelected();
    });

    $('#assocMethod').click(function() {
        hideAll();
        showSelected();
    });

    $('#probabilityMethod').click(function() {
        hideAll();
        showSelected();
    });
    
    $('#fireTypeMethod').click(function() {
        hideAll();
        showSelected();
    });

});

function showSelected() {
    $("select option:selected").each(function(i) {
        var methodId = '#' + $(this).text() + '-formset';
        $(methodId.replace(/\./g, '\\.')).show();
    });
}

function hideAll() {
    $('#method-attributes').children('div').each(function(i) {
        $(this).hide();
    });
}