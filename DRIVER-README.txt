Jarod Miller (jcm138)

How to compile Driver.java:
javac -cp ojdbc7.jar:. Driver.java

How to run Olympic.java:
java -cp ojdbc7.jar:. Driver

While the Olympic.java is meant for you to play around with the database,
create new users,teams, members, and events, as well as delete
users and team members, the Driver is meant to show the actual constraints
of the database.

Note: it may be helpful to redirect the system output into a file

java -cp ojdbc7.jar:. Driver > somefile

I tried to separate the different sections with a big line.

The first test is where I test out the constraints for user_account.
I connect and add some users, delete others, and print out the tables before and
after as much as I can and as neat as I can.

Then I test for events and atomic sports with coaches and participants
that are already in the database.

Then I test for events and team sports with newly created coaches and participants,
and I test assigning multiple events to the same team.
I also test the same coach's and athletes in multiple olympics.
    - This is where I test dropTeamMember on a participant (pid1) who is in 3 events,
    across 2 teams and 2 olympics.

Then I check if the status of the event is 'n' and the medals have been
revoked from the teammate and the participant has been fully removed from the system.

Then i access that disqualified team with only 1 member and i
create a new participant, bringing the team_size back up.
    - i implemented this part by reassigning the medal back to the team members
    in the places that the team originally had.

Hopefully my comments and print statements help you follow along as well.