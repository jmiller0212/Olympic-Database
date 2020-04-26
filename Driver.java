// Jarod Miller (jcm138)

import java.sql.*;
import java.util.*;

public class Driver {

    public static void main(String args[]) throws SQLException {
        Olympic olympic_DB = new Olympic();
        String u = olympic_DB.getOracleUsername();
        String p = olympic_DB.getOraclePassword();
        String url = olympic_DB.getOracleURL();
        // create a connection with the Oracle database
        Connection connection = null;
        try {
            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            connection = DriverManager.getConnection(url, u, p);
            connection.setAutoCommit(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (Exception e) {
            System.out.println("Error connecting to database. Printing stack trace: ");
            e.printStackTrace();
        }

        // now i can login
        // test all my functions
        // logout
        // login to another person
        // test some more
        // etc.
        // ok lets break it down. develop a whole pseudocode to test all my exceptions
        // starting from the most logical place and then working along, correcting things
        // that need to be corrected. Lets go!
        Olympic.setAffectSettings(true);
        System.out.println("------------------------------------------------------------------------------------");
        System.out.println("TESTING IN PROGRESS FOR USER_ACCOUNT... login, createUser, dropUser, dropTeamMember");
        System.out.println("Test: user not in system");
        Olympic.login(connection,"matt","damon");

        Olympic.login(connection,"carlos_aurthur","Rio");   // curr_user=carlos_aurthur passkey=Rio
        System.out.println("Test: organizer creates another organizer");
        Olympic.createUser(connection,"jarod","dev",1);
        System.out.println("Test: organizer creates a coach");
        Olympic.createUser(connection,"coach.taylor","panthers",2);
        System.out.println("Test: organizer creates another organizer and then deletes the user");
        Olympic.createUser(connection,"soondelete","d",1);
        System.out.println("Deleting the user");
        Olympic.dropUser(connection,"soondelete");
        System.out.println("Test: organizer first drops a team member for an atomic sport\n"
            +"so the coach has nobody to coach, and then this organizer will drop the coach user");
        // if i drop participant id 14 then participant id 13 is the coach for them
        Olympic.dropTeamMember(connection, 14);
        // coach with participant id is coach_7 in this database
        System.out.println("Deleting the coach user");
        Olympic.dropUser(connection,"coach_7");

        System.out.println("------------------------------------------------------------------------------------");
        System.out.println("TESTING IN PROGRESS FOR Events and atomic sport functionality... createTeam, registerTeam, addTeamMember, dropTeamMember");
        // Beach Volleyball, venue 2: capacity 2, belonging to olympic_id 1 or athens 2004, currently 1 event there on 2004-08-20, female event, and on the same date as the first event
        System.out.println("Test: organizer creates a valid event");
        int e1 = Olympic.createEvent(connection, 1, 2, 1, "2004-08-15");
        // try to create another event but enforce capacity prevents it from registering raises an error
        System.out.println("Test: ENFORCE_CAPACITY raises an error for overbooking a venue");
        Olympic.createEvent(connection, 2, 1, 1, "2004-08-20");
        // now try to create another event. This time there is an exception because 
        System.out.println("Test: raises an error for event_time being out of bounds for that venue's olympic");
        Olympic.createEvent(connection, 2, 3, 1, "2008-08-20");
        Olympic.logout(connection);


        // mini schedule to test basic constraints
        Olympic.login(connection,"coach_1","c1");
        int t1 = Olympic.createTeam(connection, "Athens", 2004, "5000M", "United States", "d1");
        Olympic.registerTeam(connection, t1, e1);
        Olympic.addTeamMember(connection, t1, 2);
        Olympic.logout(connection);

        Olympic.login(connection,"coach_2","c2");
        int t2 = Olympic.createTeam(connection, "Athens", 2004, "5000M", "United States", "d2");
        Olympic.registerTeam(connection, t2, e1);
        Olympic.addTeamMember(connection, t2, 4);
        Olympic.logout(connection);

        Olympic.login(connection,"coach_3","c3");
        int t3 = Olympic.createTeam(connection, "Athens", 2004, "5000M", "United States", "d3");
        Olympic.registerTeam(connection, t3, e1);
        Olympic.addTeamMember(connection, t3, 6);
        Olympic.logout(connection);

        Olympic.login(connection,"coach_4","c4");
        int t4 = Olympic.createTeam(connection, "Athens", 2004, "5000M", "United States", "d4");
        Olympic.registerTeam(connection, t4, e1);
        Olympic.addTeamMember(connection, t4, 8);
        Olympic.logout(connection);

        Olympic.login(connection,"coach_5","c5");
        int t5 = Olympic.createTeam(connection, "Athens", 2004, "5000M", "United States", "d5");
        Olympic.registerTeam(connection, t5, e1);
        Olympic.addTeamMember(connection, t5, 10);
        Olympic.logout(connection);

        Olympic.login(connection, "jarod", "dev");
        // wrong olympics for event's venue and team
        System.out.println("Test various erroneous event outcomes:");
        Olympic.addEventOutcome(connection, 2, e1, t1, 2, 1);
        // team registered to 146 is not in event e1
        Olympic.addEventOutcome(connection, 1, e1, 146, 2, 1);
        // participant is not in team
        Olympic.addEventOutcome(connection, 1, e1, t1, 166, 1);
        System.out.println("Add correct first place outcome for event");
        Olympic.addEventOutcome(connection, 1, e1, t1, 2, 1);
        System.out.println("Error: try to add a second first place to atomic sport");
        Olympic.addEventOutcome(connection, 1, e1, t1, 4, 1);
        System.out.println("Error: try to add a coach to the scoreboard");
        Olympic.addEventOutcome(connection, 1, e1, t1, 1, 2);
        System.out.println("Add correct 2nd, 3rd, 4th, and 5th places for event");
        Olympic.addEventOutcome(connection, 1, e1, t2, 4, 2);
        Olympic.addEventOutcome(connection, 1, e1, t3, 6, 3);
        Olympic.addEventOutcome(connection, 1, e1, t4, 8, 4);
        Olympic.addEventOutcome(connection, 1, e1, t5, 10, 5);



        System.out.println("Jarod creates a second valid event to test dropTeamMember when 2 events are assigned for each team");
        System.out.println("At the end, the positions on the scoreboard for both events should be modified");
        int e2 = Olympic.createEvent(connection, 1, 2, 1, "2004-08-16");
        Olympic.registerTeam(connection, t1, e2);
        Olympic.registerTeam(connection, t2, e2);
        Olympic.registerTeam(connection, t3, e2);
        Olympic.registerTeam(connection, t4, e2);
        Olympic.registerTeam(connection, t5, e2);

        System.out.println("Testing invalid addEventOutcomes:");
        // wrong olympics for event's venue and team
        Olympic.addEventOutcome(connection, 2, e2, t1, 2, 1);
        // team registered to 146 is not in event e1
        Olympic.addEventOutcome(connection, 1, e2, 146, 2, 1);
        // participant is not in team
        Olympic.addEventOutcome(connection, 1, e2, t1, 166, 1);
        System.out.println("Add correct event outcomes for event");
        Olympic.addEventOutcome(connection, 1, e2, t1, 2, 1);
        Olympic.addEventOutcome(connection, 1, e2, t2, 4, 2);
        Olympic.addEventOutcome(connection, 1, e2, t3, 6, 3);
        Olympic.addEventOutcome(connection, 1, e2, t4, 8, 4);
        Olympic.addEventOutcome(connection, 1, e2, t5, 10, 5);
        System.out.println("test dropTeamMember on preexisting participant 2 now for multiple teams across Athens olympics");
        Olympic.dropTeamMember(connection, 2);
        Olympic.dropUser(connection, "coach_1");
        Olympic.logout(connection);

        try {
            // now that we created a valid event, we will test createTeam, addParticipant,
            // addTeamMember, and addEventOutcome
            // this all works for a team event
            System.out.println("------------------------------------------------------------------------------------");
            System.out.println("TESTING IN PROGRESS FOR Events for team sport functionality and creating test participants...");
            Olympic.login(connection,"jarod","dev");
            int eid1 = Olympic.createEvent(connection, 4, 2, 1, "2004-08-20");
            Olympic.createUser(connection, "russian.coach", "rc", 2);
            Olympic.createUser(connection, "japanese.coach", "jc", 2);
            Olympic.createUser(connection, "french.coach", "fc", 2);
            Olympic.logout(connection);

            connection.setAutoCommit(false);
            Olympic.login(connection,"coach.taylor","panthers");
            int tid1 = Olympic.createTeam(connection, "Athens", 2004, "Beach Volleyball", "United States", "driverteam1");
            Olympic.registerTeam(connection, tid1, eid1);
            int pid1 = Olympic.addParticipant(connection, "Ashley", "Tisdale", "American", "New Orleans, Louisiana", "1987-02-02");
            int pid2 = Olympic.addParticipant(connection, "Britney", "Bates", "American", "San Antonio, Texas", "1987-04-02");
            Olympic.addTeamMember(connection, tid1, pid1);
            Olympic.addTeamMember(connection, tid1, pid2);
            Olympic.logout(connection);

            Olympic.login(connection,"russian.coach", "rc");
            int tid2 = Olympic.createTeam(connection, "Athens", 2004, "Beach Volleyball", "Russia", "driverteam2");
            Olympic.registerTeam(connection, tid2, eid1);
            int pid3 = Olympic.addParticipant(connection, "Lucy", "Luck", "Russian", "Kiev, Ukraine", "1988-03-22");
            int pid4 = Olympic.addParticipant(connection, "Brienne", "Omar", "Russian", "St.Petersburgh, Russia", "1987-06-08");
            Olympic.addTeamMember(connection, tid2, pid3);
            Olympic.addTeamMember(connection, tid2, pid4);
            Olympic.logout(connection);

            Olympic.login(connection,"japanese.coach", "jc");
            int tid3 = Olympic.createTeam(connection, "Athens", 2004, "Beach Volleyball", "Japan", "driverteam3");
            Olympic.registerTeam(connection, tid3, eid1);
            int pid5 = Olympic.addParticipant(connection, "Yua", "Sakura", "Japanese", "Tokyo, Japan", "1982-08-21");
            int pid6 = Olympic.addParticipant(connection, "Hina", "Yui", "Japanese", "Kyoto, Japan", "1988-04-12");
            Olympic.addTeamMember(connection, tid3, pid5);
            Olympic.addTeamMember(connection, tid3, pid6);
            Olympic.logout(connection);

            Olympic.login(connection,"french.coach", "fc");
            int tid4 = Olympic.createTeam(connection, "Athens", 2004, "Beach Volleyball", "France", "driverteam4");
            Olympic.registerTeam(connection, tid4, eid1);
            int pid7 = Olympic.addParticipant(connection, "Anne", "Luis", "French", "Paris, France", "1984-09-22");
            int pid8 = Olympic.addParticipant(connection, "Alice", "Barielle", "French", "Paris, France", "1985-08-08");
            Olympic.addTeamMember(connection, tid4, pid7);
            Olympic.addTeamMember(connection, tid4, pid8);
            Olympic.logout(connection);

            Olympic.login(connection, "jarod", "dev");
            Olympic.addEventOutcome(connection, 1, eid1, tid1, pid1, 1);
            Olympic.addEventOutcome(connection, 1, eid1, tid1, pid2, 1);
            Olympic.addEventOutcome(connection, 1, eid1, tid2, pid3, 2);
            Olympic.addEventOutcome(connection, 1, eid1, tid2, pid4, 2);
            Olympic.addEventOutcome(connection, 1, eid1, tid3, pid5, 3);
            Olympic.addEventOutcome(connection, 1, eid1, tid3, pid6, 3);
            Olympic.addEventOutcome(connection, 1, eid1, tid4, pid7, 4);
            Olympic.addEventOutcome(connection, 1, eid1, tid4, pid8, 4);
            int eid2 = Olympic.createEvent(connection, 4, 2, 1, "2004-08-21");
            Olympic.logout(connection);

            Olympic.login(connection,"coach.taylor","panthers");
            Olympic.registerTeam(connection, tid1, eid2);
            Olympic.logout(connection);

            Olympic.login(connection,"russian.coach", "rc");
            tid2 = Olympic.createTeam(connection, "Athens", 2004, "Beach Volleyball", "Russia", "driver2team2");
            Olympic.registerTeam(connection, tid2, eid2);
            Olympic.addTeamMember(connection, tid2, pid3);
            Olympic.addTeamMember(connection, tid2, pid4);
            Olympic.logout(connection);

            Olympic.login(connection,"japanese.coach", "jc");
            tid3 = Olympic.createTeam(connection, "Athens", 2004, "Beach Volleyball", "Japan", "driver2team3");
            Olympic.registerTeam(connection, tid3, eid2);
            Olympic.addTeamMember(connection, tid3, pid5);
            Olympic.addTeamMember(connection, tid3, pid6);
            Olympic.logout(connection);

            Olympic.login(connection,"french.coach", "fc");
            tid4 = Olympic.createTeam(connection, "Athens", 2004, "Beach Volleyball", "France", "driver2team4");
            Olympic.registerTeam(connection, tid4, eid2);
            Olympic.addTeamMember(connection, tid4, pid7);
            Olympic.addTeamMember(connection, tid4, pid8);
            Olympic.logout(connection);

            // this time the americans got second and the russians got first
            Olympic.login(connection, "jarod", "dev");
            Olympic.addEventOutcome(connection, 1, eid2, tid2, pid3, 1);
            Olympic.addEventOutcome(connection, 1, eid2, tid2, pid4, 1);
            Olympic.addEventOutcome(connection, 1, eid2, tid1, pid1, 2);
            Olympic.addEventOutcome(connection, 1, eid2, tid1, pid2, 2);
            Olympic.addEventOutcome(connection, 1, eid2, tid3, pid5, 3);
            Olympic.addEventOutcome(connection, 1, eid2, tid3, pid6, 3);
            Olympic.addEventOutcome(connection, 1, eid2, tid4, pid7, 4);
            Olympic.addEventOutcome(connection, 1, eid2, tid4, pid8, 4);
            int eid3 = Olympic.createEvent(connection, 4, 9, 2, "2008-08-21");
            Olympic.logout(connection);

            Olympic.login(connection,"coach.taylor","panthers");
            tid1 = Olympic.createTeam(connection, "Beijing", 2008, "Beach Volleyball", "United States", "driver3team1");
            Olympic.registerTeam(connection, tid1, eid3);
            Olympic.addTeamMember(connection, tid1, pid1);
            Olympic.addTeamMember(connection, tid1, pid2);
            Olympic.logout(connection);

            // I should have participants who are already in the database on these other teams
            Olympic.login(connection,"russian.coach", "rc");
            tid2 = Olympic.createTeam(connection, "Beijing", 2008, "Beach Volleyball", "South Korea", "driver3team2");
            Olympic.registerTeam(connection, tid2, eid3);
            Olympic.addTeamMember(connection, tid2, pid3);
            Olympic.addTeamMember(connection, tid2, pid4);
            Olympic.logout(connection);

            Olympic.login(connection,"japanese.coach", "jc");
            tid3 = Olympic.createTeam(connection, "Beijing", 2008, "Beach Volleyball", "Japan", "driver3team3");
            Olympic.registerTeam(connection, tid3, eid3);
            Olympic.addTeamMember(connection, tid3, pid5);
            Olympic.addTeamMember(connection, tid3, pid6);
            Olympic.logout(connection);

            Olympic.login(connection,"french.coach", "fc");
            tid4 = Olympic.createTeam(connection, "Beijing", 2008, "Beach Volleyball", "France", "driver3team4");
            Olympic.registerTeam(connection, tid4, eid3);
            Olympic.addTeamMember(connection, tid4, pid7);
            Olympic.addTeamMember(connection, tid4, pid8);
            Olympic.logout(connection);

            // this time the americans got second and the russians got first
            Olympic.login(connection, "jarod", "dev");
            Olympic.addEventOutcome(connection, 2, eid3, tid2, pid3, 1);
            Olympic.addEventOutcome(connection, 2, eid3, tid2, pid4, 1);
            Olympic.addEventOutcome(connection, 2, eid3, tid3, pid5, 2);
            Olympic.addEventOutcome(connection, 2, eid3, tid3, pid6, 2);
            Olympic.addEventOutcome(connection, 2, eid3, tid1, pid1, 3);
            Olympic.addEventOutcome(connection, 2, eid3, tid1, pid2, 3);
            Olympic.addEventOutcome(connection, 2, eid3, tid4, pid7, 4);
            Olympic.addEventOutcome(connection, 2, eid3, tid4, pid8, 4);
            Olympic.dropTeamMember(connection, pid1);
            Olympic.logout(connection);
            connection.commit();
            /**************************************
             * If we got this far then we have successfully passed dropping a team member (a participant)
             * who has 2 events on 1 team in the first olympics and 1 event on another team in the second olympics
             * 
             * so now we should try adding a replacement participant
             **************************************/
            System.out.println("------------------------------------------------------------------------------------");
            System.out.println("TESTING IN PROGRESS FOR replacing a dropped team member. Should re-validate the event participation for that team");
            System.out.println("first try to addEventOutcome on a team that is ineligible to compete");

            Olympic.login(connection, "jarod", "dev");
            Olympic.addEventOutcome(connection, 2, eid3, tid1, pid2, 3);
            Olympic.printEventParticipation(connection, tid1);
            int replacement = Olympic.addParticipant(connection, "Carly", "Ash", "American", "New York City, New York", "1991-02-02");
            Olympic.addTeamMember(connection, tid1, replacement);
            Olympic.printTeamMember(connection, tid1);
            Olympic.printEventParticipation(connection, tid1);
            Olympic.addEventOutcome(connection, 2, eid3, tid1, pid2, 3);
            Olympic.addEventOutcome(connection, 2, eid3, tid1, replacement, 3);
            Olympic.logout(connection);
            connection.commit();
            // team 154 event 27 position 3
        } catch(SQLException e) {
            Olympic.printSQLExceptions(e);
            connection.rollback();
        }
        System.out.println("------------------------------------------------------------------------------------");
        System.out.println("TESTING IN PROGRESS FOR ALL Queries");
        System.out.println("first try to addEventOutcome on a team that is ineligible to compete");

        Olympic.login(connection, "jarod", "dev");
        // display each sport
        Olympic.displaySport(connection, "Coxless Four Rowing");
        Olympic.logout(connection);
        Olympic.login(connection, "coach.taylor", "panthers");
        // select all events for each olympic
        Olympic.displayEvent(connection, "Rio", 2016, 4);
        Olympic.logout(connection);
        Olympic.login(connection, "guest_account", "GUEST");
        // display all olympics
        Olympic.countryRanking(connection, 3);
        // display all olympics
        Olympic.topkAthletes(connection, 2, 20);
        // 
        Olympic.connectedAthletes(connection, 166, 3, 1);
        Olympic.logout(connection);
        connection.commit();
        connection.setAutoCommit(true);
    }
}