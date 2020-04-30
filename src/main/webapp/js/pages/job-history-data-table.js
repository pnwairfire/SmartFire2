$(document).ready(function() {
    $('.data-table').dataTable( {
        "bPaginate": false,
        "bScrollCollapse": true,
        "aaSorting": [ [0, 'desc'] ],
        "aoColumns": [
        null,
        null,
        null,
        null,
        null
        ]
    } );
} );