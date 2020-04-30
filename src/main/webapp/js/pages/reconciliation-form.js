function toggleCronReconciliation() {
    $("#reconciliationCron").attr('disabled', function(_, attrVal) {
        return !attrVal;
    });
}