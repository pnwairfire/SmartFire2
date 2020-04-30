create table reconciliation_stream_summary_data_layers (
    reconciliation_stream_id int4 not null,
    summary_data_layer_id int4 not null,
    primary key (reconciliation_stream_id, summary_data_layer_id)
);

alter table reconciliation_stream_summary_data_layers
    add constraint fk_stream_layers_layer
    foreign key (summary_data_layer_id)
    references summary_data_layer;

alter table reconciliation_stream_summary_data_layers
    add constraint fk_stream_layers_stream
    foreign key (reconciliation_stream_id)
    references reconciliation_stream;