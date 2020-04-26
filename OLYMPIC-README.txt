Jarod Miller (jcm138)

Remember to populate the sql scripts in the order of 
1. init.sql
2. trigger.sql
3. schema.sql
4. queries.sql which has the queries

You will probably need to change the o_username and o_password to your credentials in Olympic.java
I'm not entirely sure how that works in terms of accessing oracle on my account or your account for grading purposes.

How to compile Olympic.java:
javac -cp ojdbc7.jar:. Olympic.java

How to run Olympic.java:
java -cp ojdbc7.jar:. Olympic

I have implemented a 2 level interface. The first level has the options of logging in
and exitting. Also in the Olympic level you will be asked if you want to display extra information
the database. It is always on in the driver but its purpose is to try and help testing.

The second level provides all the available functions
according to your user_account's role_id

To test organizer functions choose one of these preexisting organizers
or create a new organizer once you log in.
username:               passkey:
gianna_angelopovlos     Athens
hu_jintao               Beijing
sebastian_newbold       London
carlos_aurthur          Rio

To test the coach account you can either use one of the 146 generated preexisting coaches as
coach_1                 c1
...                     ...
coach_146               c146
OR
You can create a coach on an organizer account and then log in with those credentials

To login to the guest account 
guest_account           GUEST

The functions and parameters are straight-forward from the project description. 
createTeam checks to see if your coach account has already been registered as a coach. 
If not (i.e. you created a coach user_account) then you will be asked to input some information 
and then reselect createTeam. 

