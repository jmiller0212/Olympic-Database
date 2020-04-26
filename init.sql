-- Jarod Miller (jcm138)

-- drop our standard tables
drop table USER_ROLE cascade constraints;
drop table USER_ACCOUNT cascade constraints;
drop table OLYMPICS cascade constraints;
drop table SPORT cascade constraints;
drop table PARTICIPANT cascade constraints;
drop table COUNTRY cascade constraints;
drop table TEAM cascade constraints;
drop table TEAM_MEMBER cascade constraints;
drop table MEDAL cascade constraints;
drop table SCOREBOARD cascade constraints;
drop table VENUE cascade constraints;
drop table EVENT cascade constraints;
drop table EVENT_PARTICIPATION cascade constraints;
-- drop our global temporary tables
drop table connected purge;
drop table ca_result purge;
-- drop those sequences
DROP SEQUENCE user_id_seq;
DROP SEQUENCE olympic_id_seq;
DROP SEQUENCE sport_id_seq;
DROP SEQUENCE participant_id_seq;
DROP SEQUENCE country_id_seq;
DROP SEQUENCE team_id_seq;
DROP SEQUENCE venue_id_seq;
DROP SEQUENCE event_id_seq;
DROP SEQUENCE coach_id_seq;
-- Stores the roles as an enumeration. The roles are Organizer, Coach, and Guest
-- (the password for the guest user is GUEST)
create table USER_ROLE(
    role_id integer,
    role_name varchar2(20) not null,
    constraint user_role_PK primary key (role_id),
    constraint role_id_check check (role_id in (1,2,3)),
    constraint role_name_check check (role_name in ('Organizer','Coach','Guest'))
);
-- Stores the credentials for each user registered in the system.
create table USER_ACCOUNT(
    user_id integer not null,
    username varchar2(20) not null,
    passkey varchar2(20) not null,
    role_id integer not null,
    last_login date default SYSDATE not null,
    constraint user_account_PK primary key (user_id) using index,
    constraint user_account_UQ unique (username),
    constraint user_account_FK foreign key (role_id)
        references USER_ROLE(role_id)
);
create table OLYMPICS(
    olympic_id integer,
    olympic_num varchar2(30) not null,
    host_city varchar2(30) not null,
    opening_date date not null,
    closing_date date not null,
    official_website varchar2(50),
    constraint olympics_PK primary key (olympic_id),
    constraint olympics_UQ unique (olympic_num),
    constraint date_check check (opening_date < closing_date)
);
create table SPORT(
    sport_id integer,
    sport_name varchar2(30) not null,
    description varchar2(80) not null,
    dob date,
    team_size integer not null,
    constraint sport_PK primary key (sport_id),
    constraint sport_UQ unique (sport_name),
    constraint team_size_check check (team_size >= 1)
);
-- both coaches and competitors should be participants
create table PARTICIPANT(
    participant_id integer,
    fname varchar2(30) not null,
    lname varchar2(30) not null,
    nationality varchar2(20) not null,
    birth_place varchar2(40) not null,
    dob date not null,
    constraint participant_PK primary key (participant_id),
    constraint participant_UQ unique (fname,lname,nationality,birth_place,dob)
);
create table COUNTRY(
    country_id integer,
    country varchar2(20) not null,
    country_code varchar2(3),
    constraint country_PK primary key (country_id),
    constraint country_UQ1 unique (country),
    constraint country_UQ2 unique (country_code)
);
create table TEAM(
    team_id integer,
    olympic_id integer not null,
    team_name varchar2(50) not null,
    country_id integer not null,
    sport_id integer not null,
    coach_id integer not null,
    constraint team_PK primary key (team_id),
    constraint team_olympics_FK foreign key (olympic_id)
        references OLYMPICS(olympic_id),
    constraint team_country_FK foreign key (country_id)
        references COUNTRY(country_id),
    constraint team_sport_FK foreign key (sport_id)
        references SPORT(sport_id),
    constraint team_coach_FK foreign key (coach_id)
        references PARTICIPANT(participant_id),
    constraint team_UQ unique (olympic_id, team_name, country_id, sport_id, coach_id)
);
create table TEAM_MEMBER(
    team_id integer,
    participant_id integer,
    constraint team_member_PK primary key (team_id,participant_id),
    constraint team_member_team_FK foreign key (team_id)
        references TEAM(team_id),
    constraint team_member_participant_FK foreign key (participant_id)
        references PARTICIPANT(participant_id)
);
create table MEDAL(
    medal_id integer,
    medal_title varchar2(6) not null,
    points integer default 0,
    constraint medal_PK primary key (medal_id),
    constraint medal_types check (medal_title in ('gold','silver','bronze'))
);
-- capacity refers to the number of events (max event capacity),
-- not the max number of attendees.
create table VENUE(
    venue_id integer,
    olympic_id integer not null,
    venue_name varchar2(30) not null,
    capacity integer not null,
    constraint venue_PK primary key (venue_id),
    constraint venue_UQ unique (venue_name),
    constraint venue_FK foreign key (olympic_id)
        references OLYMPICS(olympic_id),
    constraint venue_check check(capacity >= 1)
);
create table EVENT(
    event_id integer,
    sport_id integer not null,
    venue_id integer not null,
    gender integer not null,
    event_time date not null,
    constraint event_PK primary key (event_id),
    constraint event_sport_FK foreign key (sport_id)
        references SPORT(sport_id),
    constraint event_venue_FK foreign key (venue_id)
        references VENUE(venue_id),
    constraint gender_check check(gender in (1,2)), -- 1 male 2 female (based off xy (male) and xx (female) chromosomes)
    constraint event_UK unique (sport_id,venue_id,gender,event_time)
);
create table EVENT_PARTICIPATION(
    event_id integer,
    team_id integer,
    status char default 'e' not null,
    constraint event_participation_PK primary key (event_id,team_id),
    constraint event_participation_event_FK foreign key (event_id)
        references EVENT(event_id),
    constraint event_participation_team_FK foreign key (team_id)
        references TEAM(team_id),
    constraint status_check check (status in ('e','n'))
);
create table SCOREBOARD(
    olympic_id integer,
    event_id integer,
    team_id integer,
    participant_id integer,
    position integer,
    medal_id integer,
    constraint scoreboard_PK primary key (olympic_id,event_id,team_id,participant_id),
    constraint scoreboard_olympics_FK foreign key (olympic_id)
        references OLYMPICS(olympic_id),
    constraint scoreboard_event_FK foreign key (event_id)
        references EVENT(event_id),
    constraint scoreboard_team_FK foreign key (team_id)
        references TEAM(team_id) on delete cascade,
    constraint scoreboard_participant_FK foreign key (participant_id)
        references PARTICIPANT(participant_id),
    constraint scoreboard_medal_FK foreign key (medal_id)
        references MEDAL(medal_id)
);

create global temporary table connected(
    pid integer,
    connid integer
);
create global temporary table ca_result(
    pid integer,
    connid integer
);