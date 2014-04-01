package edu.upc.eetac.dsa.cartigas.beeter.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.linking.Binding;
import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLinks;
import org.glassfish.jersey.linking.InjectLink.Style;

import edu.upc.eetac.dsa.cartigas.beeter.api.model.Sting;
import edu.upc.eetac.dsa.cartigas.beeter.api.model.StingCollection;
import edu.upc.eetac.dsa.cartigas.beeter.api.model.User;
import edu.upc.eetac.dsa.cartigas.beeter.api.model.UserCollection;



@Path("/users/{username}")
public class UserResource {
	@Context
	private SecurityContext security;

	private DataSource ds = DataSourceSPA.getInstance().getDataSource();

	private String buildHashCode(User user)
	{
		String s = user.getName() + "" + user.getEmail();
		
		return Long.toString(s.hashCode());
	}

	@GET
	@Produces(MediaType.BEETER_API_USER)
	public Response GetUserList2(@PathParam("username") String username,
			@Context Request request)
	
	{
		//Esto sirve para cachear , para comprobar simplemente cojer el etaag y ponerlo en la el campo de cabeceras
		CacheControl cc = new CacheControl();
		User usuario = getUserFromDatabase(username);
		
		EntityTag eTag = new EntityTag((buildHashCode(usuario)));
		Response.ResponseBuilder rb = request.evaluatePreconditions(eTag);
		if (rb != null) {
			return rb.cacheControl(cc).tag(eTag).build();
		}

		// If rb is null then either it is first time request; or resource is
		// modified
		// Get the updated representation and return with Etag attached to it
		rb = Response.ok(usuario).cacheControl(cc).tag(eTag);

		return rb.build();
		
	}
	
	/*
	@GET
	@Produces(MediaType.BEETER_API_USER)
	public User getUserList(@PathParam("username") String username,
			@Context Request request) {
		//CacheControl cc = new CacheControl();
		User usuario = getUserFromDatabase(username);
		return usuario;
	}*/

	/*
	 * AQUIIII!!!!
	 * 
	 * @GET
	 * 
	 * @Produces(MediaType.BEETER_API_STING_COLLECTION)
	 */
	/*
	 * @GET
	 * 
	 * @Produces(MediaType.BEETER_API_USER_COLLECTION) public UserCollection
	 * getUsers(@QueryParam("perfil") String perfil) { UserCollection cusers =
	 * new UserCollection(); Connection conn = null; try {
	 * 
	 * conn = ds.getConnection(); } catch (SQLException e) { throw new
	 * ServerErrorException("Could not connect to the database",
	 * Response.Status.SERVICE_UNAVAILABLE); } PreparedStatement stmt = null;
	 * try {
	 * 
	 * stmt = conn.prepareStatement(buildGetUsersQuery()); ResultSet rs =
	 * stmt.executeQuery(); while (rs.next()) { User usuario = new User();
	 * usuario.setName(rs.getString("name")); usuario.setUsername("username");
	 * usuario.setEmail("email"); cusers.addUsers(usuario); }
	 * 
	 * } catch (SQLException e) { throw new ServerErrorException(e.getMessage(),
	 * Response.Status.INTERNAL_SERVER_ERROR); } finally { try { if (stmt !=
	 * null) stmt.close(); conn.close(); } catch (SQLException e) {
	 * System.out.println("Problema al cerrar"); } } return cusers; }
	 */
	private String buildGetUsersQuery() {
		return "select * from users   ";
	}

	@PUT
	@Consumes(MediaType.BEETER_API_USER)
	@Produces(MediaType.BEETER_API_USER)
	public User updateUsersFromDatabase(@PathParam("username") String username,
			User usuario) {
		validateUser(username);
		Connection conn = null;

		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		PreparedStatement stmt = null;

		try {
			String sql = buildUpdateUsers();
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, usuario.getEmail());
			stmt.setString(2, usuario.getName());
			stmt.setString(3, username);
			int rows = stmt.executeUpdate();
			if (rows == 1)
				usuario = getUserFromDatabase(username);
			else {
				throw new NotFoundException("There's no sting with stingid="
						+ username);
			}
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
		return usuario;

	}

	private String buildUpdateUsers() {
		return "update users set email= ?, name= ? where username=?";
	}

	private String buildGetUserByName() {
		return "select u.* from users u  where u.username = ?";
	}

	private User getUserFromDatabase(String username) {
		User usuariodb = new User();
		Connection conn = null;

		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PreparedStatement stmt = null;

		try {
			stmt = conn.prepareStatement(buildGetUserByName());
			stmt.setString(1, username);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				usuariodb.setName(rs.getString("name"));
				usuariodb.setUsername(rs.getString("username"));
				usuariodb.setEmail(rs.getString("email"));

			} else {
				throw new NotFoundException("no hay ningun usuario con "
						+ username);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
		return usuariodb;

	}

	private void validateUser(String user) {
		User currentUser = getUserFromDatabase(user);
		if (!security.getUserPrincipal().getName()
				.equals(currentUser.getUsername()))
			throw new ForbiddenException(
					"You are not allowed to modifify this user");
	}
public String buildGetStingsOfUser(long before)
{
	if(before >0 )
	{
		return "Select s.* from stings s,users u where u.username=s.username && s.username = ?  and s.last_modified > ? order by last_modified desc limit ?";
	}
	else
	return "Select s.* from stings s,users u where u.username=s.username && s.username = ? limit ?";
}
	@GET
	@Path("/stings")
	public StingCollection getStingsOfUser(
			@PathParam("username") String username,
			@QueryParam("before") long before, @QueryParam("length") int length) {
		StingCollection stings = new StingCollection();

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int longitud;
		if(length == 0)
		{
			longitud = 5;
		}else
		{
		 longitud = length;}
		
		PreparedStatement stmt = null ; 
		try {
			stmt = conn.prepareStatement(buildGetStingsOfUser(before));
			if(before > 0)
			{
				stmt.setString(1,username);
				stmt.setInt(3, longitud);
				stmt.setLong(2, before);
			}
			else
			{
				stmt.setString(1,username);
				stmt.setInt(2, longitud);
			}
			ResultSet rs = stmt.executeQuery();
			boolean first = true;
			long oldestTimestamp = 0;
			while(rs.next())
			{
				
				Sting sting = new Sting();
				sting.setId(rs.getString("stingid"));
				sting.setUsername(rs.getString("username"));
				//sting.setAuthor(rs.getString("name"));
				sting.setSubject(rs.getString("subject"));
				oldestTimestamp = rs.getTimestamp("last_modified").getTime();
				sting.setLastModified(oldestTimestamp);
				if (first) {
					first = false;
					stings.setNewestTimestamp(sting.getLastModified());
				}
				stings.addSting(sting);
			
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
		
		return stings;
	}

}
