-- Jarod Miller (jcm138)

declare
    userperson integer;
    eid integer;
    tid integer;
    pid integer;
begin
-- createUser
userperson := createUser('queryuser1','q',1,sysdate);

-- dropUser
delete from USER_ACCOUNT where username='queryuser1';

-- createEvent
eid := createEvent(3,14,2,TO_DATE('2012-08-09','yyyy-mm-dd'));

-- createTeam
tid := createTeam('London',2012,'Individual 70M Archery','United States','demoteam',268);

-- registerTeam
registerTeam(tid,eid);

-- addParticipant
pid := addParticipant('FakePerson','McDunkin','American','Wendys, California',TO_DATE('1989-02-09','yyyy-mm-dd'));

-- addTeamMember
addTeamMember(tid,pid);

-- addEventOutcome -- oid eid tid pid position
addEventOutcome(3,eid,tid,pid,1);

-- dropTeamMember
dropTeamMember(355);
end;

-- displaySport
select sport_name,dob as year_added,host_city,olympic_num,event_id,gender,fname,lname,medal_title,country from
    (select * from
        (select * from
            (select sport_id,sport_name,dob,event_id,gender from SPORT natural join EVENT where sport_name = '5000M')
            natural join (select olympic_id,event_id,team_id,SCOREBOARD.participant_id,medal_id,fname,lname
        from SCOREBOARD,PARTICIPANT where SCOREBOARD.participant_id = PARTICIPANT.participant_id)) natural join MEDAL
    natural join (select OLYMPICS.olympic_id,olympic_num,host_city,team_id,country_id from TEAM, OLYMPICS where TEAM.olympic_id = OLYMPICS.olympic_id)
    ) natural join COUNTRY
order by olympic_num asc,medal_id;

-- displayEvent
select olympic_num,host_city,event_id,fname,lname,position,medal_id from
    (select * from (select olympic_id,olympic_num,host_city from OLYMPICS)
        natural join (select * from VENUE natural join EVENT where olympic_id = 1 and event_id = 13))
    natural join (select * from (select * from SCOREBOARD natural join PARTICIPANT) natural left join MEDAL)
order by position asc;

-- countryRanking
select olympic_id,country_code,findRegistrationDate(country_id) as first_olympic_appearance,
       COUNT(case when medal_title = 'gold' then 1 end) as gold,
       COUNT(case when medal_title = 'silver' then 1 end) as silver,
       COUNT(case when medal_title = 'bronze' then 1 end) as bronze,
       (case when SUM(points) is null then 0 else SUM(points)end) as total_points
    from (select * from SCOREBOARD natural left join MEDAL where olympic_id = 4)
    natural join (select * from (select * from TEAM natural join COUNTRY) natural join EVENT)
group by olympic_id, country_code,country_id order by total_points desc;

-- topkAthletes
select olympic_id,fname,lname,COUNT(case when medal_title = 'gold' then 1 end) as gold,
    COUNT(case when medal_title = 'silver' then 1 end) as silver,
    COUNT(case when medal_title = 'bronze' then 1 end) as bronze,
    (case when SUM(points) is null then 0 else SUM(points)end) as total_points
from (select * from PARTICIPANT natural join (select * from SCOREBOARD natural left join MEDAL where olympic_id = 3))
natural join (select * from TEAM natural join EVENT)
group by olympic_id, fname,lname,points
order by total_points desc,bronze,silver,gold
fetch first 35 rows only;

-- connectedAthletes
-- I used a function and inserted connectedAthletes into a global temporary table and then selected the connected people
-- this is a good example (the participant_id is misty may's volleyball teammmate from Rio and misty medaled in all 4 of the past olympics)
call connectedAthletes(190,4,3);
call getCAResults(44);
select * from ca_result where pid <> connid;