import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;


public class Main {

    static final String apiKey = "a08f7a3181eb43d6ab179eb4ccdb9785";
    public static Connection conn;
    public static Statement stmt;

    //Database connection initializer
    static {
        try {
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/userdata",
                    "root", "1029");
        } catch (SQLException e) {
            System.out.println("Error connecting to DB: \n");
            e.printStackTrace();
        }
    }

    // 13246838
    // 4611686018459405016
    //https://www.bungie.net/Platform/User/GetMembershipsById/<ID>/254/ (returns destinyMembershipId)

    public static void main(String[] args) throws IOException, SQLException {
        //Database statement initializer
        stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        /*String url =
                "https://www.bungie.net/Platform/Destiny2/3/Account/4611686018503511625/Stats/";

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("X-API-KEY", apiKey);

        int responseCode = con.getResponseCode();
        System.out.println("\nSending request to: " + url);
        System.out.println("Response Code: " + responseCode);
        System.out.println();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        String response = "";

        while ((inputLine = in.readLine()) != null) {
            response += inputLine;
        }
        in.close();

        JsonParser parser = new JsonParser();

        JsonElement jsonElement = new JsonParser().parse(response);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        jsonObject = jsonObject.getAsJsonObject("Response");
        JsonArray jaArray = jsonObject.getAsJsonArray("characters");

        JsonObject json = (JsonObject) parser.parse(response);*/

        ResultSet rset = stmt.executeQuery("select * from userinfo");


        int dbSize = 0; //Gets size of DB
        while (rset.next()){ dbSize++; }
        rset.first();

        getClanMemberInfo(dbSize); //Checks if the database needs to be updated. If so, executes the update
        for (int i = 0; i < dbSize; i++){ //Pulls data for each ID in DB and executes populateDB
            rset = stmt.executeQuery("select * from userinfo where ID = "+i);
            rset.next();
            populateDB(rset.getLong("destinyID"), rset.getInt("membershipType"));

            wait(25); //25ms sleep timer to avoid rate limits
        }


    }

    public static void getClanMemberInfo(int dbCount) throws IOException, SQLException {

        String url =
                "https://www.bungie.net/Platform/GroupV2/4045835/Members/";

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("X-API-KEY", apiKey);

        int responseCode = con.getResponseCode();
        System.out.println("\nSending request to: " + url);
        System.out.println("Response Code: " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        String response = "";

        while ((inputLine = in.readLine()) != null) {
            response += inputLine;
        }
        in.close();

        JsonElement jsonElement = new JsonParser().parse(response);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        jsonObject = jsonObject.getAsJsonObject("Response");
        JsonArray jaArray = jsonObject.getAsJsonArray("results");

        int memberCount = jsonObject.get("totalResults").getAsInt();
        //System.out.println(memberCount);

        if(memberCount != dbCount){ //Only execute if DB size != clan size. Needs updated
            System.out.println("DB needs update. Running update");

            int[] membershipType = new int[memberCount];
            long[] destinyId = new long[memberCount];
            String[] userName = new String[memberCount];

            //Creates an array for with each members userName, membershipType, and their destinyId
            for (int var = 0; var < jaArray.size(); var++){
                JsonObject jo = jaArray.get(var).getAsJsonObject();
                JsonObject jo1 = jo.getAsJsonObject("destinyUserInfo");

                destinyId[var] = jo1.get("membershipId").getAsLong();
                membershipType[var] = jo1.get("membershipType").getAsInt();
                userName[var] = jo1.get("LastSeenDisplayName").getAsString();

                //Debug output
                //System.out.println(userName[var] + " Type: " + membershipType[var] + " ID: " + destinyId[var]);
            }

            int rowCount = 0;

            //Loops for every member of the clan, populates DB with info
            for (int var = 0; var < jaArray.size(); var++){

                //Debug. Outputs every update
                //System.out.println("Update" + var);

                if (userName[var].contains("'")){
                    userName[var] = userName[var].replaceAll("'","");} //Removes ' from usernames to bypass SQL issues.

                //Creates SQL command with 'var' users data
                String strUpdate =
                        "insert ignore userinfo (username, destinyID, membershipType) " + "values('"
                                +userName[var]+"', '"
                                +destinyId[var]+"', '"
                                +membershipType[var]+"')";

                //Debug output
                //System.out.println("Sending command: " + strUpdate + "\n");

                //Executes update
                stmt.executeUpdate(strUpdate);

                //Deprecated debug code. Might not work. Outputs each row of the Database
                /*String strSelect = "select * from userinfo";
                System.out.println("The SQL statement is: " + strSelect + "\n");  // Echo for debugging
                ResultSet rset = stmt.executeQuery(strSelect);
                while(rset.next()) {
                    int ID = rset.getInt("ID");
                    String usernameTest = rset.getString("username");
                    long destinyIDTest = rset.getLong("destinyID");
                    int  membershipTypeTest = rset.getInt("membershipType");
                    int char1, char2, char3 = 0;

                    System.out.println(ID + ", " + usernameTest + ", " + destinyIDTest + ", " + membershipType
                    ", " + char1 + ", " + char2 + ", " + char3);
                }*/
            }
        }

        //Debug: outputs the json data received from the API for the all clan members
        /*JsonObject json = (JsonObject) parser.parse(response);
        System.out.println();
        System.out.println(json);*/
    }

    public static void populateDB(long destinyId, int membershipType) throws IOException, SQLException {

        String url = "https://www.bungie.net/Platform/Destiny2/"+membershipType
                +"/Account/"+destinyId+"/Stats/";

        System.out.println("\n\nCurrent user: " + destinyId);

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("X-API-KEY", apiKey);
        int responseCode = con.getResponseCode();
        System.out.println("\nSending request to: " + url);
        System.out.println("Response Code: " + responseCode);
        System.out.println();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        String response = "";

        while ((inputLine = in.readLine()) != null) {
            response += inputLine;
        }
        in.close();

        JsonParser parser = new JsonParser();

        JsonElement jsonElement = new JsonParser().parse(response);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        jsonObject = jsonObject.getAsJsonObject("Response");
        JsonArray jaArray = jsonObject.getAsJsonArray("characters");

        //Debug, dumps json data received from API
        /*JsonObject json = (JsonObject) parser.parse(response);
        System.out.println();
        System.out.println(json);*/

        long[] charIds = new long[3]; //Length three as there is max 3 characters

        int j = 0;
        for (int i = 0; i < jaArray.size(); i++) {

            jsonObject = jaArray.get(i).getAsJsonObject();

            //If character is deleted, drop data
            if (!jsonObject.get("deleted").getAsBoolean()){
                charIds[j] = jsonObject.get("characterId").getAsLong();
                j++;
            }
            //Debug output
            //System.out.println(jsonObject.get("characterId").getAsString());
        }

        int rowCount = 0;

        for (int var = 0; var < charIds.length; var++){ //Loops based on the number of characters a member has

            String strUpdate =
                    "update userinfo set char"+var+" = "+charIds[var]+" where destinyID = "+destinyId;
            System.out.println("Sending command: " + strUpdate);

            int countUpdated = stmt.executeUpdate(strUpdate);
            //Debug. Outputs affected rows per run
            //System.out.println(countUpdated + " records affected.\n");

            //Dumps character IDs from the database. Needs to be moved.
            /*String strSelect = "select * from userinfo";
            System.out.println("The SQL statement is: " + strSelect + "\n");  // Echo for debugging
            ResultSet rset = stmt.executeQuery(strSelect);
            while(rset.next()) {
                int ID = rset.getInt("ID");
                String char0Test = rset.getString("char0");
                long char1Test = rset.getLong("char1");
                long  char2Test = rset.getLong("char2");

                System.out.println(destinyId + ", " + char0Test + ", " + char1Test + ", " + char2Test);
                ++rowCount;
            }*/
        }

    }
    public static int getClearByRaid(long playerID, int raid){





        return 0;
    }

    public static void wait(int ms){
        //Simple sleep clock to avoid API rate limit
        try { Thread.sleep(ms); }
        catch(InterruptedException ex) { Thread.currentThread().interrupt(); }

    }
}
