
    create table clump (
        id int4 not null,
        area float8 not null,
        end_date date not null,
        shape geometry not null,
        start_date date not null,
        primary key (id)
    );

    create table clump_clump_layer_association (
        clump_id int4 not null,
        clumpLayerAssociations_feature_id numeric(19, 2) not null,
        unique (clumpLayerAssociations_feature_id)
    );

    create table clump_data (
        raw_data_id int8 not null,
        clump_id int4 not null
    );

    create table clump_layer_association (
        feature_id numeric(19, 2) not null,
        clump_id int4,
        summaryDataLayer_id int4,
        primary key (feature_id)
    );

    create table data_attribute (
        id int4 not null,
        attr_value varchar(100) not null,
        name varchar(100) not null,
        rawData_id int8,
        primary key (id)
    );

    create table default_weighting (
        id int4 not null,
        detection_rate float8 not null,
        end_time_weight float8 not null,
        false_alarm_rate float8 not null,
        fuels_weight float8 not null,
        growth_weight float8 not null,
        location_weight float8 not null,
        shape_weight float8 not null,
        size_weight float8 not null,
        start_time_weight float8 not null,
        primary key (id)
    );

    create table event (
        id int4 not null,
        create_date date not null,
        display_name varchar(100) not null,
        end_date date not null,
        outline_shape geometry not null,
        probability float8 not null,
        start_date date not null,
        total_area float8 not null,
        unique_id varchar(100) not null,
        reconciliationStream_id int4,
        primary key (id)
    );

    create table event_day (
        id int4 not null,
        daily_area float8 not null,
        event_date date not null,
        event_id int4,
        primary key (id)
    );

    create table event_day_event_layer_association (
        event_day_id int4 not null,
        eventLayerAssociations_feature_id numeric(19, 2) not null,
        unique (eventLayerAssociations_feature_id)
    );

    create table event_event_day (
        event_id int4 not null,
        eventDay_id int4 not null,
        unique (eventDay_id)
    );

    create table event_fires (
        fire_id int4 not null,
        event_id int4 not null
    );

    create table event_layer_association (
        feature_id numeric(19, 2) not null,
        eventDay_id int4,
        summaryDataLayer_id int4,
        primary key (feature_id)
    );

    create table export_format (
        id int4 not null,
        export_method varchar(100) not null,
        name varchar(100) not null,
        output_file_type varchar(100) not null,
        primary key (id)
    );

    create table fire (
        id int4 not null,
        probability float8 not null,
        unique_id varchar(100) not null,
        source_id int4,
        primary key (id)
    );

    create table fire_attribute (
        id int4 not null,
        attr_value varchar(100) not null,
        name varchar(100) not null,
        fire_id int4,
        primary key (id)
    );

    create table fire_clumps (
        fire_id int4 not null,
        clump_id int4 not null
    );

    create table fire_fire_attribute (
        fire_id int4 not null,
        fireAttributes_id int4 not null,
        primary key (fire_id, fireAttributes_id),
        unique (fireAttributes_id)
    );

    create table raw_data (
        id int8 not null,
        area float8 not null,
        end_date date not null,
        growth_potential float8,
        shape geometry not null,
        start_date date not null,
        source_id int4,
        primary key (id)
    );

    create table raw_data_data_attribute (
        raw_data_id int8 not null,
        dataAttributes_id int4 not null,
        primary key (raw_data_id, dataAttributes_id),
        unique (dataAttributes_id)
    );

    create table reconciliation_stream (
        id int4 not null,
        name varchar(100) not null,
        reconciliation_method varchar(100) not null,
        primary key (id)
    );

    create table reconciliation_stream_event (
        reconciliation_stream_id int4 not null,
        event_id int4 not null,
        unique (event_id)
    );

    create table reconciliation_stream_exports (
        reconciliation_stream_id int4 not null,
        export_format_id int4 not null
    );

    create table reconciliation_stream_formats (
        stream_format_id int4 not null,
        reconciliation_stream_id int4 not null
    );

    create table reconciliation_stream_reconciliation_weighting (
        reconciliation_stream_id int4 not null,
        reconciliationWeighting_id int4 not null,
        unique (reconciliationWeighting_id)
    );

    create table reconciliation_weighting (
        id int4 not null,
        detection_rate float8 not null,
        end_time_weight float8 not null,
        false_alarm_rate float8 not null,
        fuels_weight float8 not null,
        growth_weight float8 not null,
        location_weight float8 not null,
        shape_weight float8 not null,
        size_weight float8 not null,
        start_time_weight float8 not null,
        reconciliationStream_id int4,
        source_id int4,
        primary key (id)
    );

    create table scheduled_fetch (
        id int4 not null,
        fetch_method varchar(100) not null,
        last_fetch date not null,
        name varchar(100) not null,
        schedule varchar(100) not null,
        source_id int4,
        primary key (id)
    );

    create table source (
        id int4 not null,
        assoc_method varchar(100) not null,
        clump_method varchar(100) not null,
        geometry_type varchar(100) not null,
        influence_radius float8 not null,
        name varchar(100) not null,
        probability_method varchar(100) not null,
        primary key (id)
    );

    create table source_fire (
        source_id int4 not null,
        fire_id int4 not null,
        unique (fire_id)
    );

    create table source_raw_data (
        source_id int4 not null,
        rawData_id int8 not null,
        unique (rawData_id)
    );

    create table source_reconciliation_weighting (
        source_id int4 not null,
        reconciliationWeighting_id int4 not null,
        unique (reconciliationWeighting_id)
    );

    create table source_scheduled_fetch (
        source_id int4 not null,
        scheduledFetch_id int4 not null,
        unique (scheduledFetch_id)
    );

    create table stream_format (
        id int4 not null,
        name varchar(100) not null,
        stream_method varchar(100) not null,
        url_identifier varchar(100) not null,
        primary key (id)
    );

    create table summary_data_layer (
        id int4 not null,
        data_location varchar(100) not null,
        end_date date not null,
        extent geometry not null,
        name varchar(100) not null,
        start_date date not null,
        primary key (id)
    );

    create table summary_data_layer_clump_layer_association (
        summary_data_layer_id int4 not null,
        clumpLayerAssociation_feature_id numeric(19, 2) not null,
        unique (clumpLayerAssociation_feature_id)
    );

    create table summary_data_layer_event_layer_association (
        summary_data_layer_id int4 not null,
        eventLayerAssociation_feature_id numeric(19, 2) not null,
        unique (eventLayerAssociation_feature_id)
    );

    alter table clump_clump_layer_association 
        add constraint FKC8A901B31799650 
        foreign key (clumpLayerAssociations_feature_id) 
        references clump_layer_association;

    alter table clump_clump_layer_association 
        add constraint FKC8A901B3FC6F251C 
        foreign key (clump_id) 
        references clump;

    alter table clump_data 
        add constraint FKC9BEBE1AFC6F251C 
        foreign key (clump_id) 
        references clump;

    alter table clump_data 
        add constraint FKC9BEBE1A864AF9ED 
        foreign key (raw_data_id) 
        references raw_data;

    alter table clump_layer_association 
        add constraint FKE149C623FC6F251C 
        foreign key (clump_id) 
        references clump;

    alter table clump_layer_association 
        add constraint FKE149C62343E16ED8 
        foreign key (summaryDataLayer_id) 
        references summary_data_layer;

    alter table data_attribute 
        add constraint FK29778847708E8DDC 
        foreign key (rawData_id) 
        references raw_data;

    alter table event 
        add constraint FK5C6729A22A750F8 
        foreign key (reconciliationStream_id) 
        references reconciliation_stream;

    alter table event_day 
        add constraint FK1E44877C212AD3C 
        foreign key (event_id) 
        references event;

    alter table event_day_event_layer_association 
        add constraint FK2E36606685C2B383 
        foreign key (event_day_id) 
        references event_day;

    alter table event_day_event_layer_association 
        add constraint FK2E366066F882CF66 
        foreign key (eventLayerAssociations_feature_id) 
        references event_layer_association;

    alter table event_event_day 
        add constraint FK1898BD12C212AD3C 
        foreign key (event_id) 
        references event;

    alter table event_event_day 
        add constraint FK1898BD1283910158 
        foreign key (eventDay_id) 
        references event_day;

    alter table event_fires 
        add constraint FK1A13CB18C212AD3C 
        foreign key (event_id) 
        references event;

    alter table event_fires 
        add constraint FK1A13CB18EBDE83F8 
        foreign key (fire_id) 
        references fire;

    alter table event_layer_association 
        add constraint FKBA2482E83910158 
        foreign key (eventDay_id) 
        references event_day;

    alter table event_layer_association 
        add constraint FKBA2482E43E16ED8 
        foreign key (summaryDataLayer_id) 
        references summary_data_layer;

    alter table fire 
        add constraint FK2FF63630AB7658 
        foreign key (source_id) 
        references source;

    alter table fire_attribute 
        add constraint FKA57ACFD3EBDE83F8 
        foreign key (fire_id) 
        references fire;

    alter table fire_clumps 
        add constraint FKD81744ADFC6F251C 
        foreign key (clump_id) 
        references clump;

    alter table fire_clumps 
        add constraint FKD81744ADEBDE83F8 
        foreign key (fire_id) 
        references fire;

    alter table fire_fire_attribute 
        add constraint FK79D154FCEBDE83F8 
        foreign key (fire_id) 
        references fire;

    alter table fire_fire_attribute 
        add constraint FK79D154FCF18F0F55 
        foreign key (fireAttributes_id) 
        references fire_attribute;

    alter table raw_data 
        add constraint FK1DFCC96130AB7658 
        foreign key (source_id) 
        references source;

    alter table raw_data_data_attribute 
        add constraint FK2BE11EE5864AF9ED 
        foreign key (raw_data_id) 
        references raw_data;

    alter table raw_data_data_attribute 
        add constraint FK2BE11EE5ADFC7B6D 
        foreign key (dataAttributes_id) 
        references data_attribute;

    alter table reconciliation_stream_event 
        add constraint FK9A9F24F7C212AD3C 
        foreign key (event_id) 
        references event;

    alter table reconciliation_stream_event 
        add constraint FK9A9F24F749A4621F 
        foreign key (reconciliation_stream_id) 
        references reconciliation_stream;

    alter table reconciliation_stream_exports 
        add constraint FK736EF25C63CD2861 
        foreign key (export_format_id) 
        references export_format;

    alter table reconciliation_stream_exports 
        add constraint FK736EF25C49A4621F 
        foreign key (reconciliation_stream_id) 
        references reconciliation_stream;

    alter table reconciliation_stream_formats 
        add constraint FK99149679DB547F9 
        foreign key (stream_format_id) 
        references stream_format;

    alter table reconciliation_stream_formats 
        add constraint FK9914967949A4621F 
        foreign key (reconciliation_stream_id) 
        references reconciliation_stream;

    alter table reconciliation_stream_reconciliation_weighting 
        add constraint FK2FC35E9149A4621F 
        foreign key (reconciliation_stream_id) 
        references reconciliation_stream;

    alter table reconciliation_stream_reconciliation_weighting 
        add constraint FK2FC35E916182217C 
        foreign key (reconciliationWeighting_id) 
        references reconciliation_weighting;

    alter table reconciliation_weighting 
        add constraint FKB6C2700E30AB7658 
        foreign key (source_id) 
        references source;

    alter table reconciliation_weighting 
        add constraint FKB6C2700E22A750F8 
        foreign key (reconciliationStream_id) 
        references reconciliation_stream;

    alter table scheduled_fetch 
        add constraint FK4E1B34A830AB7658 
        foreign key (source_id) 
        references source;

    alter table source_fire 
        add constraint FKFAEE1FDA30AB7658 
        foreign key (source_id) 
        references source;

    alter table source_fire 
        add constraint FKFAEE1FDAEBDE83F8 
        foreign key (fire_id) 
        references fire;

    alter table source_raw_data 
        add constraint FKD485810530AB7658 
        foreign key (source_id) 
        references source;

    alter table source_raw_data 
        add constraint FKD4858105708E8DDC 
        foreign key (rawData_id) 
        references raw_data;

    alter table source_reconciliation_weighting 
        add constraint FK77CF5FB230AB7658 
        foreign key (source_id) 
        references source;

    alter table source_reconciliation_weighting 
        add constraint FK77CF5FB26182217C 
        foreign key (reconciliationWeighting_id) 
        references reconciliation_weighting;

    alter table source_scheduled_fetch 
        add constraint FK31E75C8430AB7658 
        foreign key (source_id) 
        references source;

    alter table source_scheduled_fetch 
        add constraint FK31E75C84EAA216F8 
        foreign key (scheduledFetch_id) 
        references scheduled_fetch;

    alter table summary_data_layer_clump_layer_association 
        add constraint FK35D006993728CAA4 
        foreign key (summary_data_layer_id) 
        references summary_data_layer;

    alter table summary_data_layer_clump_layer_association 
        add constraint FK35D006992F6FE105 
        foreign key (clumpLayerAssociation_feature_id) 
        references clump_layer_association;

    alter table summary_data_layer_event_layer_association 
        add constraint FK602888A43728CAA4 
        foreign key (summary_data_layer_id) 
        references summary_data_layer;

    alter table summary_data_layer_event_layer_association 
        add constraint FK602888A444C90425 
        foreign key (eventLayerAssociation_feature_id) 
        references event_layer_association;

    create sequence hibernate_sequence;
