// Jarod Miller (jcm138)

import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;

public class Olympic {
    private static final String o_username = "jcm138";
    private static final String o_password = "4171808";
    private static final String url = "jdbc:oracle:thin:@class3.cs.pitt.edu:1521:dbclass";
    private static String username;
    private static int role;
    private static boolean loggedIn;
    private static boolean print_affects;
    private static boolean organizerDeletedThemselves;

    public String getOracleUsername() {
        return o_username;
    }
    public String getOraclePassword() {
        return o_password;
    }
    public String getOracleURL() {
        return url;
    }
    public static void setAffectSettings(boolean b) {
        print_affects = b;
    }

    public static void printSQLExceptions(SQLException e) {
        System.out.println("SQL Error");
        while (e != null) {
            System.out.println("Message = " + e.getMessage());
            System.out.println("SQLState = " + e.getSQLState());
            System.out.println("ErrorCode = " + e.getErrorCode());
            e = e.getNextException();
        }
    }

    public static void printResultSet(ResultSet result) {
        try {
            ResultSetMetaData rsmd = result.getMetaData();
            // remember indices start at 1 to <= column count
            int[] lengthOfEachCol = new int[rsmd.getColumnCount()];
            // do a single pass over the results to get the max length
            ArrayList<ArrayList<String>> table = new ArrayList<ArrayList<String>>();
            ArrayList<String> cols = new ArrayList<String>();

            for(int i = 1; i <= rsmd.getColumnCount(); i++) {
                lengthOfEachCol[i-1] = rsmd.getColumnName(i).length();
                cols.add(rsmd.getColumnName(i));
            }

            while(result.next()) {
                ArrayList<String> row = new ArrayList<String>();
                for(int i = 1; i <= rsmd.getColumnCount(); i++) {
                    // some values are null (like not medaling in scoreboard)
                    // so we must handle that. We can't get the length of null
                    if(result.getString(i) == null) {
                        row.add(result.getString(i));
                        continue;
                    }
                    if(lengthOfEachCol[i-1] < result.getString(i).length()) {
                        lengthOfEachCol[i-1] = result.getString(i).length();
                    }
                    row.add(result.getString(i));
                }
                table.add(row);
            }

            // print out the cols
            for(int i = 0; i < cols.size(); i++) {
                System.out.printf("%-"+lengthOfEachCol[i]+"s ",cols.get(i));
            }
            System.out.println("");

            // print out the rows
            for(int i = 0; i < table.size(); i++) {
                ArrayList<String> row = table.get(i);
                for(int j = 0; j < row.size(); j++) {
                    System.out.printf("%-"+lengthOfEachCol[j]+"s ",row.get(j));
                }
                System.out.println("");
            }
        } catch(SQLException e) {
            printSQLExceptions(e);
        }
    }

    public static void printEventParticipation(Connection connection, int team_id) {
        try {
            PreparedStatement ps = connection.prepareStatement("select * from EVENT_PARTICIPATION where team_id = "+team_id);
            ResultSet rs = ps.executeQuery();
            printResultSet(rs);
            ps.close();
        } catch(SQLException e) {
            printSQLExceptions(e);
        }
    }
    public static void printTeamMember(Connection connection, int team_id) {
        try {
            PreparedStatement ps = connection.prepareStatement("select * from TEAM_MEMBER where team_id = "+team_id);
            ResultSet rs = ps.executeQuery();
            printResultSet(rs);
            ps.close();
        } catch(SQLException e) {
            printSQLExceptions(e);
        }
    }
    public static void printTeamInScoreboard(Connection connection, int team_id) {
        try {
            PreparedStatement ps = connection.prepareStatement("select * from SCOREBOARD where team_id = "+team_id);
            ResultSet rs = ps.executeQuery();
            printResultSet(rs);
            ps.close();
        } catch(SQLException e) {
            printSQLExceptions(e);
        }
    }
    
    public static void exit() {
        System.exit(0);
    }

    public static void startup(Connection connection) {
        Scanner in = new Scanner(System.in);
        int option;
        while(true) {
            System.out.println("Welcome to the Olympic Database");
            System.out.println("Options:\n(1) login\n(2) exit");
            option = Integer.parseInt(in.nextLine());
            if(option == 1) {
                try {
                    loginUI(connection, in);
                    // when the user returns from login connection they have to logout
                    logout(connection);
                } catch(SQLException e) {
                    printSQLExceptions(e);
                }
            }
            else if(option == 2)
                break;
            else
                continue;
        }
        in.close();
        exit();
    }

    public static void login(Connection connection, String user, String pass) throws SQLException {
        if(!loggedIn) {
            PreparedStatement ps = connection.prepareStatement("select * from USER_ACCOUNT natural join USER_ROLE where username=? and passkey=?");
            ps.setString(1, user); // first question mark
            ps.setString(2, pass);   // second question mark
            try {
                ResultSet rs = ps.executeQuery();
                // if there is a tuple in the ResultSet -- this is a cursor
                if(rs.next()) {
                    loggedIn = true;
                    System.out.println("Successfully logged in...");
                    username = user;
                    role = rs.getInt("role_id");
                    System.out.printf("username: %s\nrole: %s\nlast login: %s\n",rs.getString("username"),rs.getString("role_name"),rs.getDate("last_login").toString());
                }
                else {
                    System.out.println(user+" "+pass+" invalid");
                    System.out.println("Username and/or passkey not recognized");
                }
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                ps.close();
            }
        } else {
            System.out.println("We are already logged in");
        }
    }
    // login needs a UI function because it takes us to the second level of the user interface
    public static void loginUI(Connection connection, Scanner in) throws SQLException {
        while(!loggedIn) {
            System.out.print("\nWhat is your username? ");
            String user = in.nextLine();
            System.out.print("\nWhat is your passkey? ");
            String pass = in.nextLine();
            login(connection, user, pass);
            System.out.print("\nWould you like to see a more extensive effect on the db? (y/n)");
            String resp = in.nextLine();
            if(resp.equals("y")) {
                setAffectSettings(true);
            } else {
                setAffectSettings(false);
            }
        }
        while(loggedIn) {
            if(role == 1)
                organizerFunctions(connection, in);
            else if(role == 2)
                coachFunctions(connection, in);
            else if(role == 3)
                guestFunctions(connection, in);
            else {
                System.out.println("What the heck? How is your role_id: "+role);
                exit();
            }
            return;
        }
    }

    public static void logout(Connection connection) throws SQLException {
        if(organizerDeletedThemselves) {
            loggedIn = false;
            organizerDeletedThemselves = false;
            return;
        }
        PreparedStatement ps = connection.prepareStatement("update USER_ACCOUNT set last_login=? where username=?");
        ps.setDate(1, java.sql.Date.valueOf(java.time.LocalDate.now()));
        ps.setString(2, username);
        try {
            int rows = ps.executeUpdate();
            System.out.println(rows+" rows affected");
            ps = connection.prepareStatement("select * from USER_ACCOUNT where username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) 
                System.out.println(rs.getString("username")+" logged out at "+rs.getDate("last_login").toString());
        } catch(SQLException e) {
            printSQLExceptions(e);
        } finally {
            ps.close();
        }
        loggedIn = false;
    }

    public static void organizerFunctions(Connection connection, Scanner in) throws SQLException {
        while(true) {
            if(organizerDeletedThemselves)
                break;
            System.out.println("What would you like to do?");
            System.out.println("(1) createUser");
            System.out.println("(2) dropUser");
            System.out.println("(3) createEvent");
            System.out.println("(4) addEventOutcome");
            System.out.println("(5) registerTeam");
            System.out.println("(6) addParticipant");
            System.out.println("(7) addTeamMember");
            System.out.println("(8) dropTeamMember");
            System.out.println("(9) displaySport");
            System.out.println("(10) displayEvent");
            System.out.println("(11) countryRanking");
            System.out.println("(12) topkAthletes");
            System.out.println("(13) connectedAthletes");
            System.out.println("(14) logout");
            int option = Integer.parseInt(in.nextLine());
            if(option == 1)
                createUserUI(connection, in);
            else if(option == 2)
                dropUserUI(connection, in);
            else if(option == 3)
                createEventUI(connection, in);
            else if(option == 4) 
                addEventOutcomeUI(connection, in);
            else if(option == 5)
                registerTeamUI(connection, in);
            else if(option == 6)
                addParticipantUI(connection, in);
            else if(option == 7)
                addTeamMemberUI(connection, in);
            else if(option == 8)
                dropTeamMemberUI(connection, in);
            else if(option == 9)
                displaySportUI(connection, in);
            else if(option == 10)
                displayEventUI(connection, in);
            else if(option == 11)
                countryRankingUI(connection, in);
            else if(option == 12)
                topkAthletesUI(connection, in);
            else if(option == 13)
                connectedAthletesUI(connection, in);
            else if(option == 14)
                return;
            else
                continue;
        }
    }

    public static int createUser(Connection connection, String user, String pass, int role_id) throws SQLException {
        int user_id = 0;
        if(loggedIn && role == 1) {
            CallableStatement cs = connection.prepareCall("{? = call createUser(?,?,?,?)}");
            cs.registerOutParameter(1,Types.INTEGER);
            cs.setString(2,user);
            cs.setString(3,pass);
            cs.setInt(4,role_id);
            cs.setDate(5,java.sql.Date.valueOf(java.time.LocalDate.now()));
            try {
                cs.execute();
                user_id = cs.getInt(1);
                if(print_affects) {
                    PreparedStatement ps = connection.prepareStatement("select * from USER_ACCOUNT where username=?");
                    ps.setString(1,user);
                    ResultSet rs = ps.executeQuery();
                    printResultSet(rs);
                    if(role_id == 2) {
                        ps = connection.prepareStatement("select * from PARTICIPANT where fname=?");
                        ps.setString(1,user);
                        rs = ps.executeQuery();
                        printResultSet(rs);
                    }
                    ps.close();
                }
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                cs.close();
            }
        } else {
            System.out.println("User "+username+" not authorized to use this function");
        }
        return user_id;
    }
    public static void createUserUI(Connection connection, Scanner in) throws SQLException {
        // if the username is 'coach', user receives an error as coach is not a valid username
        System.out.print("\nusername (note if you're a coach your username is assigned your first name): ");
        String user = in.nextLine();
        System.out.print("\npasskey: ");
        String pass = in.nextLine();
        System.out.print("\nrole_id: ");
        int role_id = Integer.parseInt(in.nextLine());
        createUser(connection, user, pass, role_id);
    }

    // note that in dropUser, the coach as a participant in the olympics is not deleted from the system
    public static void dropUser(Connection connection, String user) throws SQLException {
        if(loggedIn && role == 1) {
            if(username.equals(user))
                organizerDeletedThemselves = true;
            int role_id = 0;
            // print the user in user_account before deletion
            if(print_affects) {
                System.out.println("Before dropping user:");
                PreparedStatement ps = connection.prepareStatement("select * from USER_ACCOUNT where username=?");
                ps.setString(1, user);
                try {
                    ResultSet rs = ps.executeQuery();
                    if(rs.next())
                        role_id = rs.getInt("role_id");
                    rs = ps.executeQuery();
                    printResultSet(rs);
                } catch(SQLException e) {
                    printSQLExceptions(e);
                } finally {
                    ps.close();
                }
            }
            PreparedStatement ps = connection.prepareStatement("delete from USER_ACCOUNT where username=?");
            ps.setString(1, user);
            try {
                ps.executeUpdate();
                if(print_affects) {
                    System.out.println("After dropping user:");
                    System.out.println("usernames:");
                    ps = connection.prepareStatement("select * from USER_ACCOUNT");
                    ResultSet rs = ps.executeQuery();
                    int i = 0;
                    while(rs.next()) {
                        if(i % 10 == 0) {
                            System.out.printf("\n%s",rs.getString(2));
                        }
                        else
                            System.out.printf(", %s",rs.getString(2));
                        i++;
                    }
                    System.out.println("");
                    if(role_id == 2) {
                        System.out.println("coach's participant_id is no longer in participant if they have no teams/team members");
                        ps = connection.prepareStatement("select * from PARTICIPANT");
                        rs = ps.executeQuery();
                        i = 0;
                        while(rs.next()) {
                            if(i % 20 == 0) {
                                System.out.printf("\n%s",rs.getString(1));
                            }
                            else
                                System.out.printf(", %s",rs.getString(1));
                            i++;
                        }
                        System.out.println("");
                    }
                }
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                ps.close();
            }
        } else {
            System.out.println("User "+username+" not authorized to use this function");
        }
    }
    public static void dropUserUI(Connection connection, Scanner in) throws SQLException {
        System.out.print("\nWhat is the username? ");
        String user = in.nextLine();
        dropUser(connection, user);
    }

    public static int createEvent(Connection connection, int sport_id, int venue_id, int gender, String event_time) throws SQLException {
        int event_id = 0;
        if(loggedIn && role == 1) {
            CallableStatement cs = connection.prepareCall("{? = call createEvent(?,?,?,?)}");
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2,sport_id);
            cs.setInt(3,venue_id);
            cs.setInt(4,gender);
            cs.setDate(5,java.sql.Date.valueOf(event_time));
            try {
                cs.execute();
                event_id = cs.getInt(1);
                if(print_affects) {
                    PreparedStatement ps = connection.prepareStatement("select * from EVENT where event_id = ?");
                    ps.setInt(1,event_id);
                    ResultSet rs = ps.executeQuery();
                    printResultSet(rs);
                    ps.close();
                }
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                cs.close();
            }
        } else {
            System.out.println("User "+username+" not authorized to use this function");
        }
        return event_id;
    }
    public static void createEventUI(Connection connection, Scanner in) throws SQLException {
        System.out.print("\nsport_id: ");
        int sport_id = Integer.parseInt(in.nextLine());
        System.out.print("\nvenue_id: ");
        int venue_id = Integer.parseInt(in.nextLine());
        System.out.print("\nmen's or woman's event (1/2): ");
        int gender = Integer.parseInt(in.nextLine());
        System.out.print("\ndate of event (yyyy-mm-dd): ");
        String date = in.nextLine();
        createEvent(connection, sport_id, venue_id, gender, date);
    }

    public static void addEventOutcome(Connection connection, int olympic_id, int event_id, int team_id, int participant_id, int position) throws SQLException {
        if(loggedIn && role == 1) {
            CallableStatement cs = connection.prepareCall("{call addEventOutcome(?,?,?,?,?)");
            cs.setInt(1,olympic_id);
            cs.setInt(2,event_id);
            cs.setInt(3,team_id);
            cs.setInt(4,participant_id);
            cs.setInt(5,position);
            try {
                cs.execute();
                if(print_affects) {
                    PreparedStatement ps = connection.prepareStatement("select * from SCOREBOARD where olympic_id = ? and event_id = ? and team_id = ? and participant_id = ?");
                    ps.setInt(1,olympic_id);
                    ps.setInt(2,event_id);
                    ps.setInt(3,team_id);
                    ps.setInt(4,participant_id);
                    ResultSet rs = ps.executeQuery();
                    printResultSet(rs);
                }
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                cs.close();
            }
        } else {
            System.out.println("User "+username+" not authorized to use this function");
        }
    }
    public static void addEventOutcomeUI(Connection connection, Scanner in) throws SQLException {
        System.out.print("\nolympic_id: ");
        int olympic_id = Integer.parseInt(in.nextLine());
        System.out.print("\nevent_id: ");
        int event_id = Integer.parseInt(in.nextLine());
        System.out.print("\nteam_id: ");
        int team_id = Integer.parseInt(in.nextLine());
        System.out.print("\nparticipant_id: ");
        int participant_id = Integer.parseInt(in.nextLine());
        System.out.print("\nfinishing position: ");
        int position = Integer.parseInt(in.nextLine());
        addEventOutcome(connection, olympic_id, event_id, team_id, participant_id, position);
    }

    public static void coachFunctions(Connection connection, Scanner in) throws SQLException {
        while(true) {
            System.out.println("What would you like to do?");
            System.out.println("(1) createTeam");
            System.out.println("(2) registerTeam");
            System.out.println("(3) addParticipant");
            System.out.println("(4) addTeamMember");
            System.out.println("(5) dropTeamMember");
            System.out.println("(6) displaySport");
            System.out.println("(7) displayEvent");
            System.out.println("(8) countryRanking");
            System.out.println("(9) topkAthletes");
            System.out.println("(10) connectedAthletes");
            System.out.println("(11) logout");
            int option = Integer.parseInt(in.nextLine());
            if(option == 1)
                createTeamUI(connection, in);
            else if(option == 2)
                registerTeamUI(connection, in);
            else if(option == 3)
                addParticipantUI(connection, in);
            else if(option == 4)
                addTeamMemberUI(connection, in);
            else if(option == 5)
                dropTeamMemberUI(connection, in);
            else if(option == 6)
                displaySportUI(connection, in);
            else if(option == 7)
                displayEventUI(connection, in);
            else if(option == 8)
                countryRankingUI(connection, in);
            else if(option == 9)
                topkAthletesUI(connection, in);
            else if(option == 10)
                connectedAthletesUI(connection, in);
            else if(option == 11)
                return;
            else
                continue;
        }
    }
    // this gets the coach's participant_id to be used in createTeam. Note that this handles if my coach user_account is auto-generated when
    // the db is initialized and when a coach is added by an organizer in which case the lname will be coach by default,
    // since I see no other way to connect the coaches in user_account with their participant_ids in the olympics
    private static int getCoachID(Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("select participant_id from PARTICIPANT where fname=? and lname=?");
        if(username.contains("coach_")) {
            ps.setString(1, "coach");
            ps.setString(2, username.substring(6));
        }
        else {
            ps.setString(1, username);
            ps.setString(2, "coach");
        }
        int id = 0;
        try {
            ResultSet rs = ps.executeQuery();
            // if there is a tuple in the ResultSet -- this is a cursor
            if(rs.next()) {
                id = rs.getInt("participant_id");
                System.out.println("Coach is registered in the system. ID: "+id);
                return id;
            }
            else
                System.out.println("Coach is not in the system...");
                
        } catch(SQLException e) {
            printSQLExceptions(e);
        } finally {
            ps.close();
        }
        // if we couldn't find the coach in our first query AND if we failed the creation of our coach participant
        // this will be 0
        return id;
    }
    public static int createTeam(Connection connection, String game, int year, String sport, String country, String team_name) throws SQLException {
        int team_id = 0;
        if(loggedIn && role == 2) {
            int coach_id = getCoachID(connection);
            if(coach_id == 0) {
                System.out.println("coach from user_account's participant_id is not found. Bad");
            }
            CallableStatement cs = connection.prepareCall("{? = call createTeam(?,?,?,?,?,?)}");
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2,game);
            cs.setInt(3,year);
            cs.setString(4,sport);
            cs.setString(5,country);
            cs.setString(6,team_name);
            cs.setInt(7,coach_id);
            try {
                cs.execute();
                if(print_affects) {
                    PreparedStatement ps = connection.prepareStatement("select * from TEAM where team_id=?");
                    team_id = cs.getInt(1);
                    ps.setInt(1,team_id);
                    ResultSet rs = ps.executeQuery();
                    printResultSet(rs);
                    ps.close();
                }
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                cs.close();
            }
        } else {
            System.out.println("User "+username+" not authorized to use this function");
        }
        return team_id;
    }
    public static void createTeamUI(Connection connection, Scanner in) throws SQLException {
        System.out.print("\nolympic game (host city): ");
        String game = in.nextLine();
        System.out.print("\nyear of game: ");
        int year = Integer.parseInt(in.nextLine());
        System.out.print("\nsport name: ");
        String sport = in.nextLine();
        System.out.print("\ncountry: ");
        String country = in.nextLine();
        System.out.print("\nwhat is your team name? ");
        String team_name = in.nextLine();
        createTeam(connection, game, year, sport, country, team_name);
    }

    public static void registerTeam(Connection connection, int team_id, int event_id) throws SQLException {
        if(loggedIn && (role == 1 || role == 2)) {
            CallableStatement cs = connection.prepareCall("{call registerTeam(?,?)}");
            cs.setInt(1,team_id);
            cs.setInt(2,event_id);
            try {
                cs.execute();
                if(print_affects) {
                    PreparedStatement ps = connection.prepareStatement("select * from EVENT_PARTICIPATION where team_id=? and event_id=?");
                    ps.setInt(1,team_id);
                    ps.setInt(2,event_id);
                    ResultSet rs = ps.executeQuery();
                    printResultSet(rs);
                    ps.close();
                }
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                cs.close();
            }
        } else {
            System.out.println("User "+username+" not authorized to use this function");
        }
    }
    public static void registerTeamUI(Connection connection, Scanner in) throws SQLException {
        System.out.print("\nteam_id: ");
        int team_id = Integer.parseInt(in.nextLine());
        System.out.print("\nevent_id: ");
        int event_id = Integer.parseInt(in.nextLine());
        registerTeam(connection, team_id, event_id);
    }

    public static int addParticipant(Connection connection, String fname, String lname, String nationality, String birth_place, String dob) throws SQLException {
        int participant_id = 0;
        if(loggedIn && (role == 1 || role == 2)) {
            CallableStatement cs = connection.prepareCall("{? = call addParticipant(?,?,?,?,?)}");
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2, fname);
            cs.setString(3, lname);
            cs.setString(4, nationality);
            cs.setString(5, birth_place);
            cs.setDate(6,java.sql.Date.valueOf(dob));
            try {
                cs.execute();
                if(print_affects) {
                    PreparedStatement ps = connection.prepareStatement("select * from PARTICIPANT where participant_id=?");
                    participant_id = cs.getInt(1);
                    ps.setInt(1, participant_id);
                    ResultSet rs = ps.executeQuery();
                    printResultSet(rs);
                    ps.close();
                }
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                cs.close();
            }
        } else {
            System.out.println("User "+username+" not authorized to use this function");
        }
        return participant_id;
    }
    public static void addParticipantUI(Connection connection, Scanner in) throws SQLException {
        System.out.print("\nfirst name: ");
        String fname = in.nextLine();
        System.out.print("\nlast name: ");
        String lname = in.nextLine();
        System.out.print("\nnationality: ");
        String nationality = in.nextLine();
        System.out.print("\nbirth place: ");
        String birth_place = in.nextLine();
        System.out.print("\ndate of birth (yyyy-mm-dd): ");
        String dob = in.nextLine();
        addParticipant(connection, fname, lname, nationality, birth_place, dob);
    }

    public static void addTeamMember(Connection connection, int team_id, int participant_id) throws SQLException {
        if(loggedIn && (role == 1 || role == 2)) {
            CallableStatement cs = connection.prepareCall("{call addTeamMember(?,?)}");
            cs.setInt(1,team_id);
            cs.setInt(2,participant_id);
            try {
                cs.execute();
                if(print_affects) {
                    PreparedStatement ps = connection.prepareStatement("select * from TEAM_MEMBER where team_id=? and participant_id=?");
                    ps.setInt(1,team_id);
                    ps.setInt(2,participant_id);
                    ResultSet rs = ps.executeQuery();
                    printResultSet(rs);
                    ps.close();
                }
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                cs.close();
            }
        } else {
            System.out.println("User "+username+" not authorized to use this function");
        }   
    }
    public static void addTeamMemberUI(Connection connection, Scanner in) throws SQLException {
        System.out.print("\nteam_id: ");
        int team_id = Integer.parseInt(in.nextLine());
        System.out.print("\nparticipant_id: ");
        int participant_id = Integer.parseInt(in.nextLine());
        addTeamMember(connection, team_id, participant_id);
    }

    // prints out a subset of the tables affected by dropping the participant by only printing out the rows in the same event(s) as them
    // however, since removing a participant from an atomic sport removes the event_participation we can't show how the coach remains in the participant_id
    // I did this because then if a coach is coaching multiple teams with different athletes there would be no foreign key integrity constraint issues
    private static void printDropTeamMemberAffectedTables(Connection connection,ArrayList<Integer> event_ids) throws SQLException {
        String event_id_set = "";
        for(int i = 0; i < event_ids.size(); i++) {
            if(i == 0) {
                event_id_set += "("+event_ids.get(i);
            }
            event_id_set += ","+event_ids.get(i);
        }
        event_id_set += ")";

        PreparedStatement ps = connection.prepareStatement("select * from SCOREBOARD where event_id in "+event_id_set);
        try {
            ResultSet rs = ps.executeQuery();
            printResultSet(rs);

            ps = connection.prepareStatement("select * from EVENT_PARTICIPATION where team_id in (select team_id from SCOREBOARD where event_id in "+event_id_set+")");
            rs = ps.executeQuery();
            printResultSet(rs);

            ps = connection.prepareStatement("select * from TEAM_MEMBER where team_id in (select team_id from SCOREBOARD where event_id in "+event_id_set+")");
            rs = ps.executeQuery();
            printResultSet(rs);
            // this does not print out the coach's id for atomic sports because we remove the event_id from event_participation
            ps = connection.prepareStatement("select * from PARTICIPANT where participant_id in (select participant_id from SCOREBOARD where event_id in "+event_id_set+") or participant_id in (select coach_id from TEAM where team_id in (select team_id from EVENT_PARTICIPATION where event_id in "+event_id_set+"))");
            rs = ps.executeQuery();
            printResultSet(rs);

            ps = connection.prepareStatement("select * from TEAM where team_id in (select team_id from EVENT_PARTICIPATION where event_id in "+event_id_set+")");
            rs = ps.executeQuery();
            printResultSet(rs);

        } catch(SQLException e) {
            printSQLExceptions(e);
        } finally {
            ps.close();
        }
    }
    private static ArrayList<Integer> getParticipantsEvents(Connection connection,int participant_id) throws SQLException {
        ArrayList<Integer> events = new ArrayList<Integer>();
        PreparedStatement ps = connection.prepareStatement("select event_id from event where event_id in (select event_id from EVENT_PARTICIPATION where team_id in (select team_id from TEAM_MEMBER where participant_id=?))");
        ps.setInt(1,participant_id);
        try {
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                events.add(rs.getInt("event_id"));
            }
        } catch(SQLException e) {
            printSQLExceptions(e);
        } finally {
            ps.close();
        }
        return events;
    }
    public static void dropTeamMember(Connection connection, int participant_id) throws SQLException {
        if(loggedIn && (role == 1 || role == 2)) {
            // let's get the events from scoreboard that the participant was in and from there we can get every affected table
            // before and after dropTeamMember.
            ArrayList<Integer> events = getParticipantsEvents(connection, participant_id);
            if(print_affects) {
                System.out.println("Tables before participant deletion:");
                printDropTeamMemberAffectedTables(connection, events);
            }
            CallableStatement cs = connection.prepareCall("{call dropTeamMember(?)}");
            cs.setInt(1,participant_id);
            try {
                cs.execute();
                if(print_affects) {
                    System.out.println("Tables after participant deletion:");
                    printDropTeamMemberAffectedTables(connection, events);
                }
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                cs.close();
            }
        } else {
            System.out.println("User "+username+" not authorized to use this function");
        }
    }
    public static void dropTeamMemberUI(Connection connection, Scanner in) throws SQLException {
        System.out.print("\nparticipant_id: ");
        int participant_id = Integer.parseInt(in.nextLine());
        dropTeamMember(connection, participant_id);
    }
    
    public static void guestFunctions(Connection connection, Scanner in) throws SQLException {
        while(true) {
            System.out.println("What would you like to do?");
            System.out.println("(1) displaySport");
            System.out.println("(2) displayEvent");
            System.out.println("(3) countryRanking");
            System.out.println("(4) topkAthletes");
            System.out.println("(5) connectedAthletes");
            System.out.println("(6) logout");
            int option = Integer.parseInt(in.nextLine());
            if(option == 1)
                displaySportUI(connection, in);
            else if(option == 2)
                displayEventUI(connection, in);
            else if(option == 3)
                countryRankingUI(connection, in);
            else if(option == 4)
                topkAthletesUI(connection, in);
            else if(option == 5)
                connectedAthletesUI(connection, in);
            else if(option == 6)
                return;
            else
                continue;
        }
    }

    public static void displaySport(Connection connection, String sport_name) throws SQLException {
        if(loggedIn) {
            PreparedStatement ps = connection.prepareStatement("select sport_name,dob as year_added,host_city,olympic_num,event_id,gender,fname,lname,medal_title,country from (select * from (select * from (select sport_id,sport_name,dob,event_id,gender from SPORT natural join EVENT where sport_name = ?) natural join (select olympic_id,event_id,team_id,SCOREBOARD.participant_id,medal_id,fname,lname from SCOREBOARD,PARTICIPANT where SCOREBOARD.participant_id = PARTICIPANT.participant_id)) natural join MEDAL natural join (select OLYMPICS.olympic_id,olympic_num,host_city,team_id,country_id from TEAM, OLYMPICS where TEAM.olympic_id = OLYMPICS.olympic_id)) natural join COUNTRY order by olympic_num asc,medal_id");
            ps.setString(1,sport_name);
            try {
                ResultSet rs = ps.executeQuery();
                printResultSet(rs);
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                ps.close();
            }
        } else {
            System.out.println("There is no user is logged into the system.");
        }
    }
    private static void displaySportOptions(Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("select sport_name from SPORT");
        try {
            ResultSet rs = ps.executeQuery();
            System.out.println("Display Sport Options:\n");
            while(rs.next()) {
                if(rs.isFirst())
                    System.out.printf("%s",rs.getString(1));
                else
                    System.out.printf(", %s",rs.getString(1));
            }
            System.out.println("");
        } catch(SQLException e) {
            printSQLExceptions(e);
        } finally {
            ps.close();
        }
    }
    public static void displaySportUI(Connection connection, Scanner in) throws SQLException {
        displaySportOptions(connection);
        System.out.print("\nsport: ");
        String sport_name = in.nextLine();
        displaySport(connection, sport_name);
    }

    public static void displayEvent(Connection connection, String game, int year, int event_id) throws SQLException {
        if(loggedIn) {
            int oid = 0;
            CallableStatement cs = connection.prepareCall("{? = call getOlympics(?,?)}");
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2,game);
            cs.setInt(3,year);
            try {
                cs.execute();
                oid = cs.getInt(1);
                PreparedStatement ps = connection.prepareStatement("select olympic_num,host_city,event_id,fname,lname,position,medal_id from (select * from (select olympic_id,olympic_num,host_city from OLYMPICS) natural join (select * from VENUE natural join EVENT where olympic_id = ? and event_id = ?)) NATURAL join (select * from (select * from SCOREBOARD natural join PARTICIPANT) natural left join MEDAL) order by position asc");
                ps.setInt(1,oid);
                ps.setInt(2,event_id);
                ResultSet rs = ps.executeQuery();
                printResultSet(rs);
                ps.close();
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                cs.close();
            }
        } else {
            System.out.println("There is no user is logged into the system.");
        }
    }
    private static void displayEventOptions(Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("select host_city,EXTRACT(YEAR FROM opening_date) as year,event_id from OLYMPICS natural join VENUE natural join EVENT order by year,event_id asc");
        try {
            ResultSet rs = ps.executeQuery();
            String city = "none";
            String year = "none";
            System.out.println("Display Event Options:");
            while(rs.next()) {
                if(!city.equals(rs.getString(1))) {
                    city = rs.getString(1);
                    year = rs.getString(2);
                    System.out.printf("\n%s %s: %s",city,year,rs.getString(3));
                } else {
                    System.out.printf(",%s",rs.getString(3));
                }
            }
            System.out.println("");
        } catch(SQLException e) {
            printSQLExceptions(e);
        } finally {
            ps.close();
        }
    }
    public static void displayEventUI(Connection connection, Scanner in) throws SQLException {
        displayEventOptions(connection);
        System.out.print("\nolympic game (host city): ");
        String game = in.nextLine();
        System.out.print("\nyear of game: ");
        int year = Integer.parseInt(in.nextLine());
        System.out.print("\nevent_id: ");
        int event_id = Integer.parseInt(in.nextLine());
        displayEvent(connection, game, year, event_id);
    }

    public static void countryRanking(Connection connection, int olympic_id) throws SQLException {
        if(loggedIn) {
            PreparedStatement ps = connection.prepareStatement("select olympic_id,country_code,findRegistrationDate(country_id) as first_olympic_appearance,COUNT(case when medal_title = 'gold' then 1 end) as gold,COUNT(case when medal_title = 'silver' then 1 end) as silver,COUNT(case when medal_title = 'bronze' then 1 end) as bronze,(case when SUM(points) is null then 0 else SUM(points)end) as total_points from (select * from SCOREBOARD natural left join MEDAL where olympic_id = ?) natural join (select * from (select * from TEAM natural join COUNTRY) natural join EVENT) group by olympic_id, country_code,country_id order by total_points desc");
            ps.setInt(1,olympic_id);
            try {
                ResultSet rs = ps.executeQuery();
                printResultSet(rs);
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                ps.close();
            }
        } else {
            System.out.println("There is no user is logged into the system.");
        }
    }
    private static void countryRankingOptions(Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("select olympic_id from OLYMPICS order by olympic_id asc");
        try {
            ResultSet rs = ps.executeQuery();
            System.out.println("Country Ranking Options:\n");
            while(rs.next()) {
                if(rs.isFirst())
                    System.out.printf("%s",rs.getString(1));
                else
                    System.out.printf(", %s",rs.getString(1));
            }
            System.out.println("");
        } catch(SQLException e) {
            printSQLExceptions(e);
        } finally {
            ps.close();
        }
    }
    public static void countryRankingUI(Connection connection, Scanner in) throws SQLException {
        countryRankingOptions(connection);
        System.out.print("\nolympic_id: ");
        int olympic_id = Integer.parseInt(in.nextLine());
        countryRanking(connection, olympic_id);
    }

    public static void topkAthletes(Connection connection, int olympic_id, int k) throws SQLException {
        if(loggedIn) {
            PreparedStatement ps = connection.prepareStatement("select olympic_id,fname,lname,COUNT(case when medal_title = 'gold' then 1 end) as gold,COUNT(case when medal_title = 'silver' then 1 end) as silver,COUNT(case when medal_title = 'bronze' then 1 end) as bronze,(case when SUM(points) is null then 0 else SUM(points)end) as total_points from (select * from PARTICIPANT natural join (select * from SCOREBOARD natural left join MEDAL where olympic_id = ?)) natural join (select * from TEAM natural join EVENT) group by olympic_id, fname,lname,points order by COUNT(points) desc,bronze,silver,gold fetch first ? rows only");
            ps.setInt(1,olympic_id);
            ps.setInt(2,k);
            try {
                ResultSet rs = ps.executeQuery();
                printResultSet(rs);
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                ps.close();
            }
        } else {
            System.out.println("There is no user is logged into the system.");
        }
    }
    private static void topkAthletesOptions(Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("select olympic_id from OLYMPICS order by olympic_id asc");
        try {
            ResultSet rs = ps.executeQuery();
            System.out.println("top-k Athletes Options:\n");
            while(rs.next()) {
                if(rs.isFirst())
                    System.out.printf("%s",rs.getString(1));
                else
                    System.out.printf(", %s",rs.getString(1));
            }
            System.out.println("");
        } catch(SQLException e) {
            printSQLExceptions(e);
        } finally {
            ps.close();
        }
    }
    public static void topkAthletesUI(Connection connection, Scanner in) throws SQLException {
        topkAthletesOptions(connection);
        System.out.print("\nolympic_id: ");
        int olympic_id = Integer.parseInt(in.nextLine());
        System.out.print("\nk: ");
        int k = Integer.parseInt(in.nextLine());
        topkAthletes(connection, olympic_id, k);
    }

    public static void connectedAthletes(Connection connection, int participant_id, int olympic_id, int n) throws SQLException {
        if(loggedIn) {
            connection.setAutoCommit(false);
            CallableStatement cs = connection.prepareCall("{call connectedAthletes(?,?,?)}");
            cs.setInt(1,participant_id);
            cs.setInt(2,olympic_id);
            cs.setInt(3,n);
            try {
                cs.execute();
                cs = connection.prepareCall("{call getCAResults(?)}");
                cs.setInt(1,participant_id);
                cs.execute();
                Statement ps = connection.createStatement();
                ResultSet rs = ps.executeQuery("select * from ca_result where pid <> connid");
                printResultSet(rs);
                connection.setAutoCommit(true);
                ps = connection.createStatement();
                rs = ps.executeQuery("delete from ca_result");
                ps = connection.createStatement();
                rs = ps.executeQuery("delete from connected");
                ps.close();
            } catch(SQLException e) {
                printSQLExceptions(e);
            } finally {
                cs.close();
            }
        } else {
            System.out.println("There is no user is logged into the system.");
        }
    }
    private static void connectedAthletesOptionPartOne(Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("select unique participant_id from SCOREBOARD order by participant_id asc");
        try {
            ResultSet rs = ps.executeQuery();
            System.out.println("Connected Athletes Options:");
            int i = 0;
            while(rs.next()) {
                if(i % 20 == 0) {
                    System.out.printf("\n%s",rs.getString(1));
                }
                else
                    System.out.printf(", %s",rs.getString(1));
                i++;
            }
            System.out.println("");
        } catch(SQLException e) {
            printSQLExceptions(e);
        } finally {
            ps.close();
        }
    }
    private static void connectedAthletesOptionPartTwo(Connection connection, int participant_id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("select unique olympic_id from SCOREBOARD where participant_id="+participant_id+" order by olympic_id asc");
        try {
            ResultSet rs = ps.executeQuery();
            System.out.println("Connected Athletes Options:\n");
            while(rs.next()) {
                if(rs.isFirst())
                    System.out.printf("%s",rs.getString(1));
                else
                    System.out.printf(", %s",rs.getString(1));
            }
            System.out.println("");
        } catch(SQLException e) {
            printSQLExceptions(e);
        } finally {
            ps.close();
        }
    }
    public static void connectedAthletesUI(Connection connection, Scanner in) throws SQLException {
        connectedAthletesOptionPartOne(connection);
        System.out.print("\nparticipant_id: ");
        int participant_id = Integer.parseInt(in.nextLine());
        connectedAthletesOptionPartTwo(connection,participant_id);
        System.out.print("\nolympic_id: ");
        int olympic_id = Integer.parseInt(in.nextLine());
        System.out.print("\nn (number of hops must be less than olympic_id): ");
        int n = Integer.parseInt(in.nextLine());
        connectedAthletes(connection, participant_id, olympic_id, n);
    }

    public static void main(String args[]) throws SQLException {
        // create a connection with the Oracle database
        Connection connection = null;
        try {
            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            connection = DriverManager.getConnection(url, o_username, o_password);
            connection.setAutoCommit(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (Exception e) {
            System.out.println(
                    "Error connecting to database. Printing stack trace: ");
            e.printStackTrace();
        }
        startup(connection);
    }
}