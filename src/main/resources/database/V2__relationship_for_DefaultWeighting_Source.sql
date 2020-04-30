    create table default_weighting_source (
        default_weighting_id int4 not null,
        source_id int4 not null,
        unique (source_id)
    );

	ALTER TABLE source ADD COLUMN defaultweighting_id int4 not null;

	alter table default_weighting_source
        add constraint FK5761CDCE740D275E
        foreign key (source_id)
        references source;

    alter table default_weighting_source
        add constraint FK5761CDCE21D883FB
        foreign key (default_weighting_id)
        references default_weighting;

	alter table source
        add constraint FKCA90681BB2D87B7E
        foreign key (defaultWeighting_id)
        references default_weighting;