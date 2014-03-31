package edu.upc.eetac.dsa.cartigas.beeter.api.model;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Link;

import org.glassfish.jersey.linking.Binding;
import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLinks;
import org.glassfish.jersey.linking.InjectLink.Style;

import edu.upc.eetac.dsa.cartigas.beeter.api.MediaType;
import edu.upc.eetac.dsa.cartigas.beeter.api.UserResource;

public class UserCollection {
	@InjectLinks({
		@InjectLink(resource = UserResource.class, style = Style.ABSOLUTE, rel = "create-user", title = "Create user", type = MediaType.BEETER_API_USER),
		})

	private List<Link> links;
	private List<User> users;
	public UserCollection()
	{//Wtf!!
		super();
		users = new ArrayList<>();
	}
	
	
	public List<Link> getLinks() {
		return links;
	}
	public void setLinks(List<Link> links) {
		this.links = links;
	}
	public List<User> getUsers() {
		return users;
	}
	public void setUsers(List<User> users) {
		this.users = users;
	}
	public void  addUsers(User user){
		users.add(user);
	}


}
