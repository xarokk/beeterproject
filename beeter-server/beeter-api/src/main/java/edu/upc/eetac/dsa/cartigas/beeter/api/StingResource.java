package edu.upc.eetac.dsa.cartigas.beeter.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.sql.DataSource;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import edu.upc.eetac.dsa.cartigas.beeter.api.model.Sting;
import edu.upc.eetac.dsa.cartigas.beeter.api.model.StingCollection;

@Path("/stings")
// la clase StingResource está anotada con el "/stings"
public class StingResource {
	private DataSource ds = DataSourceSPA.getInstance().getDataSource();

	// al punto de conexiones llego por el único punto que hemos definido:
	// "getInstance()..."

	// método que devuelve - obtiene datos de dos tablas: JOIN
	// las dos tablas u y s
	private String buildGetStingsQuery() {
		return "select s.*, u.name from stings s, users u where u.username=s.username order by last_modified desc";
	}

	// PROCESA PETICIONES GET, ANOTADO CON EL @GET
	// se trata de un subrecurso
	// relativo a la clase sub-recurso
	// al parámetro stingid se lo pasa obteniendola del url
	/*
	 * @GET
	 * 
	 * @Path("/{stingid}")
	 * 
	 * @Produces(MediaType.BEETER_API_STING)
	 */
	@GET
	@Produces(MediaType.BEETER_API_STING_COLLECTION)
	public StingCollection getStings(@QueryParam("length") int length,
			@QueryParam("before") long before, @QueryParam("after") long after) {
		StingCollection stings = new StingCollection();

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			boolean updateFromLast = after > 0;
			stmt = conn.prepareStatement(buildGetStingsQuery(updateFromLast));
			if (updateFromLast) {
				stmt.setTimestamp(1, new Timestamp(after));
			} else {
				if (before > 0)
					stmt.setTimestamp(1, new Timestamp(before));
				else
					stmt.setTimestamp(1, null);
				length = (length <= 0) ? 5 : length;
				stmt.setInt(2, length);
			}
			ResultSet rs = stmt.executeQuery();
			boolean first = true;
			long oldestTimestamp = 0;
			while (rs.next()) {
				Sting sting = new Sting();
				sting.setId(rs.getString("stingid"));
				sting.setUsername(rs.getString("username"));
				sting.setAuthor(rs.getString("name"));
				sting.setSubject(rs.getString("subject"));
				oldestTimestamp = rs.getTimestamp("last_modified").getTime();
				sting.setLastModified(oldestTimestamp);
				if (first) {
					first = false;
					stings.setNewestTimestamp(sting.getLastModified());
				}
				stings.addSting(sting);
			}
			stings.setOldestTimestamp(oldestTimestamp);
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

		return stings;
	}

	private String buildGetStingsQuery(boolean updateFromLast) {
		if (updateFromLast)
			return "select s.*, u.name from stings s, users u where u.username=s.username and s.last_modified > ? order by last_modified desc";
		else
			return "select s.*, u.name from stings s, users u where u.username=s.username and s.last_modified < ifnull(?, now()) order by last_modified desc limit ?";
	}

	// dice que necesita que lo que se le envie esté en este formato, significa
	// que el cliente debe indicar en el contentType el formato de Media Type
	@POST
	@Consumes(MediaType.BEETER_API_STING)
	@Produces(MediaType.BEETER_API_STING)
	public Sting createSting(Sting sting) {

		validateSting(sting); // va al método validateSting();

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			String sql = buildInsertSting();
			stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS); // te
																				// devuelve
																				// la
																				// clave
																				// primaria
																				// que
																				// se
																				// ha
																				// genetado
			// devuelve la id de quien lo ha creado, y con esto si que puede
			// obenter las generatedKeys
			stmt.setString(1, security.getUserPrincipal().getName());
			stmt.setString(2, sting.getSubject());
			stmt.setString(3, sting.getContent());
			stmt.executeUpdate();
			// se ha insertado en la base de datos

			// si ha ido bien la inserción
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				int stingid = rs.getInt(1);

				sting = getStingFromDatabase(Integer.toString(stingid));
				// se utiliza el método sting para pasarle el stingid
				// para crear el sting -> JSON
			} else {
				// Something has failed...
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
				throw new ServerErrorException(e.getMessage(),
						Response.Status.INTERNAL_SERVER_ERROR);
			}
		}

		return sting;
	}

	@GET
	@Path("/{stingid}")
	@Produces(MediaType.BEETER_API_STING)
	public Response getSting(@PathParam("stingid") String stingid,
			@Context Request request) {
		// Create CacheControl
		CacheControl cc = new CacheControl();

		Sting sting = getStingFromDatabase(stingid);

		// Calculate the ETag on last modified date of user resource
		EntityTag eTag = new EntityTag(Long.toString(sting.getLastModified()));

		// Verify if it matched with etag available in http request
		Response.ResponseBuilder rb = request.evaluatePreconditions(eTag);

		// If ETag matches the rb will be non-null;
		// Use the rb to return the response without any further processing
		if (rb != null) {
			return rb.cacheControl(cc).tag(eTag).build();
		}

		// If rb is null then either it is first time request; or resource is
		// modified
		// Get the updated representation and return with Etag attached to it
		rb = Response.ok(sting).cacheControl(cc).tag(eTag);

		return rb.build();
	}

	// se genera la query-> inserta 3 valores (username,...) y que están
	// parametizados
	// el autor no hay que pasarlo
	private String buildInsertSting() {
		return "insert into stings (username, subject, content) value (?, ?, ?)";
	}

	// PARA ELIMINAR
	// ES COMO EL GET PERO CAMBIA POR EL MÉTODO HTTP-> DELETE
	@DELETE
	@Path("/{stingid}")
	public void deleteSting(@PathParam("stingid") String stingid) {
		validateUser(stingid);
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			String sql = buildDeleteSting();
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, Integer.valueOf(stingid));

			int rows = stmt.executeUpdate();

			if (rows == 0) {
				throw new NotFoundException("There's no sting with stingid="
						+ stingid);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {

				// Haya ido bien o haya ido mal cierra las conexiones
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
				throw new ServerErrorException(e.getMessage(),
						Response.Status.INTERNAL_SERVER_ERROR);
			}
		}
	}

	private String buildDeleteSting() {
		return "delete from stings where stingid=?";
	}

	@PUT
	@Path("/{stingid}")
	@Consumes(MediaType.BEETER_API_STING)
	@Produces(MediaType.BEETER_API_STING)
	public Sting updateStingFromDatabase(@PathParam("stingid") String stingid,
			Sting sting) {
		validateUser(stingid);
		validateUpdateSting(sting);
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			String sql = buildUpdateSting();
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, sting.getSubject());
			stmt.setString(2, sting.getContent());
			stmt.setInt(3, Integer.valueOf(stingid));

			int rows = stmt.executeUpdate();
			if (rows == 1)
				sting = getStingFromDatabase(stingid);
			else {
				throw new NotFoundException("There's no sting with stingid="
						+ stingid);
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

		return sting;
	}

	private void validateUpdateSting(Sting sting) {
		if (sting.getSubject() != null && sting.getSubject().length() > 100)
			throw new BadRequestException(
					"Subject can't be greater than 100 characters.");
		if (sting.getContent() != null && sting.getContent().length() > 500)
			throw new BadRequestException(
					"Content can't be greater than 500 characters.");
	}

	// permitiendo que sea nulo-> ifnull
	private String buildUpdateSting() {
		return "update stings set subject=ifnull(?, subject), content=ifnull(?, content) where stingid=?";
	}

	private void validateSting(Sting sting) {
		if (sting.getSubject() == null)
			throw new BadRequestException("Subject can't be null.");
		if (sting.getContent() == null)
			throw new BadRequestException("Content can't be null.");
		if (sting.getSubject().length() > 100)
			throw new BadRequestException(
					"Subject can't be greater than 100 characters.");
		if (sting.getContent().length() > 500)
			throw new BadRequestException(
					"Content can't be greater than 500 characters.");
	}

	private Sting getStingFromDatabase(String stingid) {
		Sting sting = new Sting();

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(buildGetStingByIdQuery());
			stmt.setInt(1, Integer.valueOf(stingid));
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				sting.setId(rs.getString("stingid"));
				sting.setUsername(rs.getString("username"));
				sting.setAuthor(rs.getString("name"));
				sting.setSubject(rs.getString("subject"));
				sting.setContent(rs.getString("content"));
				sting.setLastModified(rs.getTimestamp("last_modified")
						.getTime());
			} else {
				throw new NotFoundException("There's no sting with stingid="
						+ stingid);
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

		return sting;
	}

	private String buildGetStingByIdQuery() {
		return "select s.*, u.name from stings s, users u where u.username=s.username and s.stingid=?";
	}

	@Context
	private SecurityContext security;

	private void validateUser(String stingid) {
		Sting currentSting = getStingFromDatabase(stingid);
		if (!security.getUserPrincipal().getName()
				.equals(currentSting.getUsername()))
			throw new ForbiddenException(
					"You are not allowed to modify this sting.");
	}

	// @GET
	// @Path("/stings/search?subject={subject}&content={content}&length={lenght}")
	// @Produces(MediaType.BEETER_API_STING)
	// public Response getSting2(@PathParam("length") String length,
	// @Context Request request)
	// {
	//
	// }
	public static int lengthDefecto = 5;
	private String buildGetStingSubjectContent(String subject, String content,
			int length) {
		String query = null;

		if (subject != null && content != null) {
			if(length <= 0)
			{System.out.print("entrooooo1");
			query = "Select s.* , u.name from stings s, users u where u.username=s.username && subject like ? && content like ? limit "+ lengthDefecto ;
			}else
			{System.out.print("entrooooo2");
			query = "Select s.* , u.name from stings s, users u where u.username=s.username && subject like ? && content like ? limit ? ";
			}
		}
		if (subject != null && content == null) {
			if(length <= 0)
			{System.out.print("entrooooo3");
			query = "Select s.*, u.name from stings s, users u where u.username=s.username && subject like ? limit "+ lengthDefecto;
			}
			else
			{System.out.print("entrooooo4");
				query = "Select s.*, u.name from stings s, users u where u.username=s.username && subject like ? limit ?";
			}
		}
		if (content != null && subject == null) {
			if(length <= 0)
			{
				System.out.print("entrooooo5");
			query = "Select s.*,u.name from stings s , users u where u.username=s.username && content like ? limit "+ lengthDefecto;
			}else
			{System.out.print("entrooooo6");
				query = "Select s.*,u.name from stings s , users u where u.username=s.username && content like ? limit ? ";	
			}
		}
		if (content == null && subject == null) {
			if(length <= 0)
			{System.out.print("entrooooo7");
			query = "Select s.* , u.name from stings s, users u where u.username=s.username limit "+ lengthDefecto;
			}else
			{System.out.print("entrooooo8");
				query = "Select s.* , u.name from stings s, users u where u.username=s.username limit ?";
			}
		}
		return query;
	}

	@GET
	@Path("/search")
	@Produces(MediaType.BEETER_API_STING_COLLECTION)
	public StingCollection searchByContentSubject(
			@QueryParam("Content") String Contenido,
			@QueryParam("Subject") String Subject,
			@QueryParam("length") int length) {
		StingCollection sting = new StingCollection();
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		PreparedStatement stmt = null;
		try {
			// asi estoy pasandole la query que es lo que hay en
			// buildgetstingsubjectcontent
			stmt = conn.prepareStatement(buildGetStingSubjectContent(Subject,
					Contenido, length));

			if (Subject != null && Contenido != null) {
				if(length <= 0)
				{
				stmt.setString(1,"%"+ Subject+"%");
				stmt.setString(2, "%"+Contenido +"%");
				}else
				{
					stmt.setString(1,"%"+ Subject+"%");
					stmt.setString(2, "%"+Contenido +"%");
					stmt.setInt(3, length);
				}

			}

			if (Contenido != null && Subject == null) {
				if (length <=0)
				{
				stmt.setString(1, "%"+Contenido +"%");
				}else
				{
					stmt.setString(1, "%"+Contenido +"%");
					stmt.setInt(2, length);
				}
			}
			if (Subject != null && Contenido == null) {
				if(length <= 0)
				{
				stmt.setString(1,"%"+ Subject+"%");
				}
				else
				{
					stmt.setString(1, "%"+ Subject+"%");
					stmt.setInt(2, length);
				}

			}
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next()) {
				Sting s = new Sting();
				s.setId(rs.getString("stingid"));
				s.setUsername(rs.getString("username"));
				s.setAuthor(rs.getString("name"));
				s.setSubject(rs.getString("subject"));

				// lo primero es la coleccion sting, luego es el sting en si (s)
				sting.addSting(s);

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

		return sting;
	}
}
