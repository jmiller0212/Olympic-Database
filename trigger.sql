-- Jarod Miller (jcm138)

CREATE SEQUENCE user_id_seq start with 1 increment by 1;
CREATE SEQUENCE olympic_id_seq start with 1 increment by 1;
CREATE SEQUENCE sport_id_seq start with 1 increment by 1;
CREATE SEQUENCE participant_id_seq start with 1 increment by 1;
CREATE SEQUENCE country_id_seq start with 1 increment by 1;
CREATE SEQUENCE team_id_seq start with 1 increment by 1;
CREATE SEQUENCE venue_id_seq start with 1 increment by 1;
CREATE SEQUENCE event_id_seq start with 1 increment by 1;
CREATE SEQUENCE coach_id_seq start with 1 increment by 1;
commit;

create or replace trigger user_id_trig before insert on USER_ACCOUNT
for each row
when(new.user_id is null)
declare
    username_invalid exception;
begin
    if :new.username = 'coach' then raise username_invalid;
    end if;
    select user_id_seq.nextval into :new.user_id from dual;
end;
/
create or replace trigger olympic_id_trig before insert on OLYMPICS
for each row
when(new.olympic_id is null)
begin
    select olympic_id_seq.nextval into :new.olympic_id from dual;
end;
/
create or replace trigger sport_id_trig before insert on SPORT
for each row
when(new.sport_id is null)
begin
    select sport_id_seq.nextval into :new.sport_id from dual;
end;
/
create or replace trigger country_id_trig before insert on COUNTRY
for each row
when(new.country_id is null)
begin
    select country_id_seq.nextval into :new.country_id from dual;
end;
/
create or replace trigger team_id_trig before insert on TEAM
for each row
when(new.team_id is null)
begin
    if :new.team_name is null then
        select team_id_seq.nextval, 'team '||team_id_seq.currval into :new.team_id, :new.team_name from dual;
    else
        select team_id_seq.nextval into :new.team_id from dual;
    end if;
end;
/
create or replace trigger venue_id_trig before insert on VENUE
for each row
when(new.venue_id is null)
begin
    -- if the venue_name is 'Olympic Stadium' then assign it host_city + 'Olympic Stadium'
    if :new.venue_name LIKE 'Olympic Stadium' then
        select host_city || ' ' || :new.venue_name into :new.venue_name from OLYMPICS where olympic_id = :new.olympic_id;
    end if;
    select venue_id_seq.nextval into :new.venue_id from dual;
end;
/
create or replace trigger event_id_trig before insert on EVENT
for each row
when(new.event_id is null)
begin
    select event_id_seq.nextval into :new.event_id from dual;
end;
/
create or replace trigger participant_id_trig before insert on PARTICIPANT
for each row
when(new.participant_id is null)
begin
    if :new.fname is null then
        select participant_id_seq.nextval,'coach',coach_id_seq.nextval into :new.participant_id,:new.fname,:new.lname from dual;
    else
        select participant_id_seq.nextval into :new.participant_id from dual;
    end if;
end;
/
-- we have to check the capacity (number of events for that game on that day)
create or replace trigger ENFORCE_CAPACITY before insert on EVENT
for each row
declare
    cap VENUE.capacity%TYPE;
    num_sched_events integer;
    capacity_reached exception;
begin
    select capacity into cap from VENUE where venue_id = :new.venue_id;
    select count(*) into num_sched_events from EVENT where venue_id = :new.venue_id
        and event_time = :new.event_time;
    if num_sched_events = cap then raise capacity_reached;
    end if;
end;
/

create or replace function createUser(
    username_in in USER_ACCOUNT.username%TYPE,
    passkey_in in USER_ACCOUNT.passkey%TYPE,
    role_id_in in USER_ACCOUNT.role_id%TYPE,
    last_login_in in USER_ACCOUNT.last_login%TYPE)
return integer
is
begin
    insert into USER_ACCOUNT(username,passkey,role_id,last_login) values (username_in,passkey_in,role_id_in,last_login_in);
    return user_id_seq.currval;
end;
/

create or replace function createEvent(
    sport_id_in in EVENT.sport_id%TYPE,
    venue_id_in in EVENT.event_id%TYPE,
    gender_in in EVENT.gender%TYPE,
    event_time_in in EVENT.event_time%TYPE)
return integer
is
    date_not_during_olympics exception;
    open date;
    close date;
begin
    select opening_date,closing_date into open,close from OLYMPICS where olympic_id =
        (select olympic_id from VENUE where venue_id = venue_id_in);
    if event_time_in < open or event_time_in > close then raise date_not_during_olympics;
    end if;
    insert into EVENT(sport_id, venue_id, gender, event_time) values (sport_id_in,venue_id_in,gender_in,event_time_in);
    return event_id_seq.currval;
end;
/

create or replace procedure addEventOutcome(
    olympic_id_in in SCOREBOARD.olympic_id%TYPE,
    event_id_in in SCOREBOARD.event_id%TYPE,
    team_id_in in SCOREBOARD.team_id%TYPE,
    participant_id_in in SCOREBOARD.participant_id%TYPE,
    position_in in SCOREBOARD.position%TYPE)
as
    coach_in_scoreboard exception;
    event_not_for_olympic exception;
    f PARTICIPANT.fname%TYPE;
    l PARTICIPANT.lname%TYPE;
    date_not_during_olympics exception;
    team_did_not_participate exception;
    team_not_in_olympics exception;
    too_many_entries exception;
    participant_not_on_team exception;
    participant_on_team integer;
    open date;
    close date;
    e_time date;
    oid integer;
    event_found boolean;
    position_count_for_sport integer;
    sport_size integer;
    duplicate_record integer;
begin
    select opening_date,closing_date into open,close from OLYMPICS where olympic_id = olympic_id_in;
    select event_time into e_time from EVENT where event_id = event_id_in;
    if e_time < open or e_time > close then raise date_not_during_olympics;
    end if;

    select olympic_id into oid from TEAM where team_id = team_id_in;
    if olympic_id_in <> oid then
        raise team_not_in_olympics;
    end if;

    event_found := false;
    -- if the team is not participating in the event or they are not eligible raise an exception
    for event_rec in (select event_id from EVENT_PARTICIPATION where team_id = team_id_in and status = 'e')
    loop
        if event_id_in = event_rec.event_id then event_found := true;
        end if;
    end loop;
    if event_found = false then
        raise team_did_not_participate;
    end if;

    select count(*) into position_count_for_sport from SCOREBOARD where event_id = event_id_in and position = position_in;
    select team_size into sport_size from SPORT where sport_id = (select sport_id from TEAM where team_id = team_id_in);
    if position_count_for_sport >= sport_size then raise too_many_entries;
    end if;

    -- raise an exception if we try to insert a coach into the scoreboard!
    select fname,lname into f,l from PARTICIPANT where participant_id = participant_id_in;
    if f = 'coach' or l = 'coach' then raise coach_in_scoreboard;
    end if;

    select count(*) into participant_on_team from TEAM_MEMBER where team_id = team_id_in and participant_id = participant_id_in;
    if participant_on_team = 0 then raise participant_not_on_team;
    end if;


    select count(*) into duplicate_record from SCOREBOARD
    where olympic_id = olympic_id_in and event_id = event_id_in and team_id = team_id_in
            and participant_id = participant_id_in and ROWNUM = 1;
    -- if this participant is already in our database
    if duplicate_record = 1 then
        update SCOREBOARD set medal_id = position_in
            where olympic_id = olympic_id_in and event_id = event_id_in and team_id = team_id_in
            and participant_id = participant_id_in;
    else
        insert into SCOREBOARD(olympic_id, event_id, team_id, participant_id, position) values (olympic_id_in,event_id_in,team_id_in,participant_id_in,position_in);
    end if;
end;
/

-- createCoach (Organizer) function: manipulates user_account and participant
-- basically creates the generated coaches
create or replace function createCoach(
    fname_in in PARTICIPANT.fname%TYPE,
    lname_in in PARTICIPANT.lname%TYPE)
    return integer
is
begin
    insert into PARTICIPANT(fname, lname, nationality, birth_place, dob)
        values(fname_in,lname_in,'none','none',TO_DATE('1960-01-01','yyyy-mm-dd'));
    return participant_id_seq.currval;
end;
/

create or replace trigger addUserAccount after insert on PARTICIPANT
for each row
when(new.fname = 'coach')
begin
    insert into USER_ACCOUNT(username, passkey, role_id)
        values(:new.fname||'_'||:new.lname,SUBSTR(:new.fname,1,1)||:new.lname,2);
end;
/

create or replace trigger addCoachFromUserAccount after insert on USER_ACCOUNT
for each row
when(new.role_id = 2)
begin
    -- if the username does not contain the substring 'coach_' (its a coach user from the UI)
    -- then we add that participant with the username as their first name
    -- since that is the only way in this schema to connect the user_account to a coach
    -- so coach users can createTeams with their own participant_id
    if INSTR(:new.username,'coach_') = 0 then
        insert into PARTICIPANT(fname, lname, nationality, birth_place, dob)
            values(:new.username,'coach','none','none',TO_DATE('1960-01-01','yyyy-mm-dd'));
    end if;
end;
/

create or replace function createTeam(
    city_in in OLYMPICS.host_city%TYPE,
    year_in in integer,
    sport_in in SPORT.sport_name%TYPE,
    country_in in COUNTRY.country%TYPE,
    team_name_in in TEAM.team_name%TYPE,
    coach_id_in in PARTICIPANT.participant_id%TYPE)
    return integer
is
    oid integer; -- to retrieve olympic_id for team insert
    countryid COUNTRY.country_id%TYPE;
    sportid SPORT.sport_id%TYPE;
begin
    select olympic_id into oid from OLYMPICS
    where city_in = host_city and year_in = (EXTRACT(YEAR FROM OLYMPICS.opening_date));
    select country_id into countryid from COUNTRY where country_in = country;
    select sport_id into sportid from SPORT where sport_in = sport_name;
    insert into TEAM(olympic_id, team_name, country_id, sport_id, coach_id)
        values(oid,team_name_in,countryid,sportid,coach_id_in);
    -- should return the value of the team_id for the tuple we just inserted
    return team_id_seq.currval;
end;
/

create or replace procedure registerTeam(
    team_id_in in TEAM.team_id%TYPE,
    event_id_in in EVENT.event_id%TYPE) as
begin
    insert into EVENT_PARTICIPATION(event_id, team_id)
        values(event_id_in,team_id_in);
end;
/

create or replace function addParticipant(
    fname_in in PARTICIPANT.fname%TYPE,
    lname_in in PARTICIPANT.lname%TYPE,
    nationality_in in PARTICIPANT.nationality%TYPE,
    birth_place_in in PARTICIPANT.birth_place%TYPE,
    dob_in in PARTICIPANT.dob%TYPE)
    return integer
is
    pid integer;
    duplicate_record integer;
begin
    select count(*) into duplicate_record from PARTICIPANT
    where fname = fname_in and lname = lname_in and nationality = nationality_in
            and birth_place = birth_place_in and dob = dob_in and ROWNUM = 1;
    -- if this participant is already in our database
    if duplicate_record = 1 then
        select participant_id into pid from PARTICIPANT
            where fname = fname_in and lname = lname_in and nationality = nationality_in
            and birth_place = birth_place_in and dob = dob_in;
            -- we just have to return their participant_id
            return pid;
    else
        insert into PARTICIPANT(fname, lname, nationality, birth_place, dob)
        values(fname_in,lname_in,nationality_in,birth_place_in,dob_in);
            return participant_id_seq.currval;
    end if;
end;
/
-- must test this trigger
-- also check if the event participation is n, if so
-- see if this additional team member will qualify them (i.e. bring the team to have the max number of team members)
create or replace trigger maxTeamSize before insert on TEAM_MEMBER
for each row
declare
    curr_team_size integer;
    max_team_size integer;
    max_size_reached exception;
    curr_status char;
begin
    select count(*) into curr_team_size from TEAM_MEMBER where team_id = :new.team_id;
    if curr_team_size > 0 then
        select team_size into max_team_size from SPORT natural join
            (select * from TEAM where TEAM.team_id = :new.team_id);
        if curr_team_size >= max_team_size then raise max_size_reached;
        end if;
        -- if this team member brings us back up to the competing team size
        -- then change our event status to eligible
    end if;
    if curr_team_size+1 = max_team_size then
        select status into curr_status from EVENT_PARTICIPATION where team_id = :new.team_id;
        if curr_status = 'n' then
            update EVENT_PARTICIPATION set status = 'e' where team_id = :new.team_id;
        end if;
    end if;
end;
/
-- when we addTeamMember check if the EVENT_PARTICIPATION status is 'n'
-- that means we are trying to replace a disqualified member
-- if, after adding another team member we reach the max team size (remember the above trigger is a before insert)
-- then we should change that status to 'e'ligible
create or replace procedure addTeamMember(
    team_id_in in TEAM.team_id%TYPE,
    participant_id_in in PARTICIPANT.participant_id%TYPE) as
begin
    insert into TEAM_MEMBER(team_id, participant_id)
        values(team_id_in,participant_id_in);
end;
/

create or replace function getOlympics(
    city_in in OLYMPICS.host_city%TYPE,
    year_in in integer)
    return OLYMPICS.olympic_id%TYPE
is oid OLYMPICS.olympic_id%TYPE;
begin
    select olympic_id into oid from OLYMPICS
    where city_in = host_city and year_in = (EXTRACT(YEAR FROM OLYMPICS.opening_date));
    return oid;
end;
/

create or replace trigger ASSIGN_MEDAL before insert or update on SCOREBOARD
for each row
-- initialization
-- old values contains null in a before insert
begin
    if :new.position < 4 then --and :old.position <> :new.position then-- and :old.medal_id is null then
        if :old.position is null then
            select :new.position into :new.medal_id from dual;
        end if;
        if :old.position <> :new.position then
            select :new.position into :new.medal_id from dual;
        end if;
        if :old.medal_id is null and :new.medal_id is not null then
            select :new.position into :new.medal_id from dual;
        end if;
    end if;
end;
/

create or replace procedure addDefaultEventOutcome(
    oid in OLYMPICS.olympic_id%TYPE,
    eid in EVENT.event_id%TYPE,
    pid in PARTICIPANT.participant_id%TYPE,
    part_position in SCOREBOARD.position%TYPE)
as
begin
    insert into SCOREBOARD(olympic_id, event_id, team_id, participant_id, position) values (oid,eid,team_id_seq.currval,pid,part_position);
end;
/

-- for each team
create or replace procedure updateScoreboard(pid SCOREBOARD.participant_id%TYPE, size_of_team SPORT.team_size%TYPE, tid SCOREBOARD.team_id%TYPE) as
    i integer;
begin
    if size_of_team = 1 then
        i := 1;
        for events_team_is_in in (select * from SCOREBOARD where event_id in (select event_id from EVENT_PARTICIPATION where team_id = (select team_id from TEAM where team_id = tid)) and participant_id = pid)
        LOOP
            delete from SCOREBOARD where participant_id = pid and event_id = events_team_is_in.event_id;
            for scoreboard_rec in (select * from SCOREBOARD where event_id = events_team_is_in.event_id)
            LOOP
                if scoreboard_rec.position > i then
                    update SCOREBOARD set position = i where event_id = scoreboard_rec.event_id
                        and participant_id = scoreboard_rec.participant_id;
                end if;
                i := i + 1;
            end loop;
            i := 1;
        end loop;
    else -- here we have to revoke the medals of the teammates
        update SCOREBOARD set medal_id = null where team_id in (select team_id from TEAM_MEMBER where participant_id in pid);
        delete from SCOREBOARD where team_id in (select team_id from TEAM_MEMBER where TEAM_MEMBER.participant_id = pid) and participant_id = pid;
    end if;
end;
/
create or replace trigger ATHLETE_DISMISSAL before delete on PARTICIPANT
for each row
begin
    -- this query's cardinality should be each team the participant is a part of
    -- and have other information as attributes for updates/deletions
    for participants_teams in (select team_id,team_size,coach_id,participant_id from
        team natural join (select team_id,participant_id from TEAM_MEMBER where participant_id = :old.participant_id)
        natural join sport)
    LOOP
        if participants_teams.team_size = 1 then
            -- first do scoreboard - ORDER MATTERS
            updateScoreboard(participants_teams.participant_id, participants_teams.team_size,participants_teams.team_id);
            -- this will be different when team_size > 1
            -- if the team size is 1 then just delete the team from event participation
            delete from EVENT_PARTICIPATION where team_id = participants_teams.team_id;
            -- and the participant from team member
            delete from TEAM_MEMBER where team_id = participants_teams.team_id and participant_id = :old.participant_id;
            -- and finally the team (in the order of not violating integrity constraints)
            delete from TEAM where team_id = participants_teams.team_id;
        else
            updateScoreboard(participants_teams.participant_id, participants_teams.team_size, participants_teams.team_id);
            -- set that status for that team
            update EVENT_PARTICIPATION set status = 'n' where team_id = participants_teams.team_id;
            -- delete that team member
            delete from TEAM_MEMBER where team_id = participants_teams.team_id and participant_id = :old.participant_id;
        end if;
    end loop;
end;
/
-- cleanup.
create or replace trigger dropCoach before delete on USER_ACCOUNT
for each row
when(old.role_id = 2)
declare
    cid integer;
    num_team_members integer;
    all_teams_empty boolean;
begin
    -- FIRST THINGS FIRST, we need to determine what the participant_id is for this coach based off the first and last name
    if INSTR(:old.username,'coach_') = 0 then
        select participant_id into cid from PARTICIPANT where fname = :old.username;
    else
        select participant_id into cid from PARTICIPANT where fname = 'coach' and lname = SUBSTR(:old.username,INSTR(:old.username,'_')+1);
    end if;
    all_teams_empty := true;
    -- for each team that the coach is the coach of, check if there are team members
    -- and if so don't delete but if there are delete
    for t_rex in (select * from TEAM where coach_id = cid)
    loop
        select count(*) into num_team_members from TEAM where team_id = t_rex.team_id;
        if num_team_members > 0 then
            all_teams_empty := false;
        else
            -- deleting from scoreboard is probably redundant but we're doing it anyway
            delete from SCOREBOARD where team_id = t_rex.team_id;
            delete from EVENT_PARTICIPATION where team_id = t_rex.team_id;
            delete from TEAM where team_id = t_rex.team_id;
        end if;
    end loop;
    -- if all teams are in fact empty we can get rid of coach from participant
    if all_teams_empty then
        delete from PARTICIPANT where participant_id = cid;
    end if;
end;
/
create or replace procedure dropTeamMember(participant_id_in in PARTICIPANT.participant_id%TYPE)
as
begin
    delete from PARTICIPANT where participant_id = participant_id_in;
end;
/

create or replace function findRegistrationDate(country_id_in in COUNTRY.country_id%TYPE)
    return date
is
    d date;
begin
    select MIN(event_time) into d from (select * from (select * from TEAM natural join COUNTRY where country_id = country_id_in) natural join EVENT_PARTICIPATION) natural join EVENT;
    return d;
end;
/

create or replace procedure connectedAthletes(pid PARTICIPANT.participant_id%TYPE, oid OLYMPICS.olympic_id%TYPE, n integer)
as
    not_enough_past_olympics exception;
begin
    -- this means that the query is invalid, impossible
    if n >= oid then
        raise not_enough_past_olympics;
    end if;
    -- level 1
    for competitor_rec0 in (select participant_id from TEAM_MEMBER where team_id in (select team_id from TEAM where olympic_id = oid and team_id in
        (select team_id from EVENT_PARTICIPATION where event_id in (select event_id from (select * from TEAM_MEMBER where participant_id = pid) natural join (select * from TEAM natural join EVENT_PARTICIPATION where olympic_id = oid))))
        and participant_id <> pid)
    LOOP
        if n = 0 then
            insert into connected values(pid,competitor_rec0.participant_id);
        else
            -- level 2
            for competitor_rec1 in (select participant_id from TEAM_MEMBER where team_id in (select team_id from TEAM where olympic_id = oid-1 and team_id in
                (select team_id from EVENT_PARTICIPATION where event_id in (select event_id from (select * from TEAM_MEMBER where participant_id = competitor_rec0.participant_id) natural join (select * from TEAM natural join EVENT_PARTICIPATION where olympic_id = oid-1))))
                and participant_id <> competitor_rec0.participant_id)
            LOOP
                if n = 1 then
                    insert into connected values(competitor_rec0.participant_id,competitor_rec1.participant_id);
                else
                    -- level 3
                    for competitor_rec2 in (select participant_id from TEAM_MEMBER where team_id in (select team_id from TEAM where olympic_id = oid-2 and team_id in
                        (select team_id from EVENT_PARTICIPATION where event_id in (select event_id from (select * from TEAM_MEMBER where participant_id = competitor_rec1.participant_id) natural join (select * from TEAM natural join EVENT_PARTICIPATION where olympic_id = oid-2))))
                        and participant_id <> competitor_rec1.participant_id)
                    LOOP
                        if n = 2 then
                            insert into connected values(competitor_rec1.participant_id,competitor_rec2.participant_id);
                        else
                            -- level 4
                            for competitor_rec3 in (select participant_id from TEAM_MEMBER where team_id in (select team_id from TEAM where olympic_id = oid-3 and team_id in
                                (select team_id from EVENT_PARTICIPATION where event_id in (select event_id from (select * from TEAM_MEMBER where participant_id = competitor_rec2.participant_id) natural join (select * from TEAM natural join EVENT_PARTICIPATION where olympic_id = oid-3))))
                                and participant_id <> competitor_rec2.participant_id)
                            LOOP
                                if n = 3 then
                                    insert into connected values(competitor_rec2.participant_id,competitor_rec3.participant_id);
                                end if;
                            end loop;
                        end if;
                    end loop;
                end if;
            end loop;
        end if;
    end loop;
end;
/

create or replace procedure getCAResults(pid PARTICIPANT.participant_id%TYPE)
as
begin
    for res_rec in (select unique connid from connected order by connid asc)
    LOOP
        insert into ca_result values(pid,res_rec.connid);
    end loop;
end;
/
-- ORA-04091: table PARTICIPANT is mutating, trigger/function may not see it
-- this means i'm querying the table that invoked the trigger which is a big no no

-- ORA-01427: single-row subquery returns more than one row
-- this is self explanatory but when i use "where blah = (select achoo...)"
-- i should use "where blah in (select achoo...) to allow multirow subqueries