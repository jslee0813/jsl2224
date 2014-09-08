package models;

import com.socrata.api.HttpLowLevel;
import com.socrata.api.Soda2Consumer;
import com.socrata.model.soql.SoqlQuery;
import com.sun.jersey.api.client.ClientResponse;
import com.jayway.jsonpath.JsonPath;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.h2.tools.DeleteDbFiles;

public class RefuseData {
	
	private static final String endpoint = "http://data.cityofnewyork.us?";	
	private static final JSONParser parser = new JSONParser();
	
	private static final String JDBC_DRIVER = "org.h2.Driver";
    private static final String DB_URL = "jdbc:h2:~/refuse";
    private static boolean isTableCreated = false;
    
    
	public static String getRefuseAmount(String borough, int commId) throws Exception {
        InsertQuery(borough, commId);
		return processQuery("refusetonscollected", borough, commId);
	}
	
	public static String getPaperAmount(String borough, int commId) throws Exception {
        InsertQuery(borough, commId);
		return processQuery("papertonscollected", borough, commId);		
	}
	
	public static String getMGPAmount(String borough, int commId) throws Exception {
        InsertQuery(borough, commId);
		return processQuery("mgptonscollected", borough, commId);
	}
	
	public static String getTotal() throws Exception {
		String amount = "";
		Double total = 0.00;
		
		if (isTableCreated) {			
			Connection conn = DriverManager.getConnection(DB_URL);
			Statement stmt = conn.createStatement();		
			ResultSet rs = stmt.executeQuery("SELECT * FROM QueriedCommunityDistrict");
			
			while (rs.next()) {
				String borough = rs.getString("Borough");
				String commId = rs.getString("CommunityID");
				
				amount = getRefuseAmount(borough, Integer.parseInt(commId));
				total += Double.parseDouble(amount);
				
				amount = getPaperAmount(borough, Integer.parseInt(commId));
				total += Double.parseDouble(amount);
				
				amount = getMGPAmount(borough, Integer.parseInt(commId));
				total += Double.parseDouble(amount);
			}
				
			stmt.close();
			conn.close();
		}
				
		return Long.toString(Math.round(total));		
	}
	
	
	private static String processQuery(String fieldName, String borough, int commId) throws Exception {
		Soda2Consumer consumer = Soda2Consumer.newConsumer(endpoint + "$select=" + fieldName + "&$where=communitydistrict=" + commId + "&borough=" + borough);
		ClientResponse response = consumer.query("ewtv-wftx", HttpLowLevel.JSON_TYPE, SoqlQuery.SELECT_ALL);
        String entity = response.getEntity(String.class);
        
        JSONArray array = (JSONArray)parser.parse(entity);        
        String amount = JsonPath.read(array.get(0), "$." + fieldName).toString();
        return Long.toString(Math.round(Double.parseDouble(amount)));
	}
	
	private static void InsertQuery(String borough, int commId) throws Exception {
		Connection conn = DriverManager.getConnection(DB_URL);
		Statement stmt = conn.createStatement();
		
		if (!isTableCreated)
			CreateTable();
		
		ResultSet rs = stmt.executeQuery("SELECT * "
								       + "FROM QueriedCommunityDistrict "
								       + "WHERE CommunityID = " + commId
								       + "  AND Borough = '" + borough + "'");
		
		if (!rs.next())
		{					        
			stmt.execute("INSERT INTO QueriedCommunityDistrict (CommunityID, Borough) "
					   + "VALUES (" + commId + ", '" + borough + "')");
		}
		
		stmt.close();
		conn.close();
	}
	
	private static void CreateTable() throws Exception {
		DeleteDbFiles.execute("~", "refuse", true);
		
		Class.forName(JDBC_DRIVER);		
		Connection conn = DriverManager.getConnection(DB_URL);
		Statement stmt = conn.createStatement();

		stmt.execute("DROP TABLE QueriedCommunityDistrict");
		
		stmt.execute("CREATE TABLE IF NOT EXISTS QueriedCommunityDistrict ("				   
				   + "CommunityID INT,"
				   + "Borough VARCHAR(100),"
				   + "PRIMARY KEY (CommunityID, Borough))");

		isTableCreated = true;
		
		stmt.close();
		conn.close();		
	}
}
