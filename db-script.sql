create table user
(
    id       int auto_increment
        primary key,
    name     varchar(255) not null,
    password text         not null,
    constraint user_name_uindex
        unique (name)
);

create table todo
(
    id      int auto_increment
        primary key,
    content text                                                          not null,
    date    datetime                            default CURRENT_TIMESTAMP null,
    status  enum ('NOT_COMPLETED', 'COMPLETED') default 'NOT_COMPLETED'   not null,
    user_id int                                                           null,
    constraint todo_user_id_fk
        foreign key (user_id) references user (id)
            on update cascade
);


