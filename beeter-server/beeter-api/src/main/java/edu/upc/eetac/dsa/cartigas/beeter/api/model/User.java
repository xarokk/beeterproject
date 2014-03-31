package edu.upc.eetac.dsa.cartigas.beeter.api.model;

import java.util.List;

import javax.ws.rs.core.Link;

import org.glassfish.jersey.linking.Binding;
import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLinks;
import org.glassfish.jersey.linking.InjectLink.Style;

import edu.upc.eetac.dsa.cartigas.beeter.api.MediaType;
import edu.upc.eetac.dsa.cartigas.beeter.api.StingResource;
import edu.upc.eetac.dsa.cartigas.beeter.api.UserResource;

public class User {

	@InjectLinks({
		@InjectLink(resource = UserResource.class, style = Style.ABSOLUTE, rel = "collection", title = "Ultimos usuarios", type = MediaType.BEETER_API_USER_COLLECTION),
		@InjectLink(resource = UserResource.class, style = Style.ABSOLUTE, rel = "self edit", title = "Usuario", type = MediaType.BEETER_API_USER, method = "getUsers", bindings = @Binding(name = "username", value = "${instance.name}")) })
	private List<Link> links;
	private String username;
	private String name;
	private String email;
	public List<Link> getLinks() {
		return links;
	}
	public void setLinks(List<Link> links) {
		this.links = links;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	
}
