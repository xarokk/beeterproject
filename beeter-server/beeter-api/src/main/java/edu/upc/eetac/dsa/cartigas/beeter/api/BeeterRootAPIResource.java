package edu.upc.eetac.dsa.cartigas.beeter.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
 
import edu.upc.eetac.dsa.cartigas.beeter.api.model.BeeterRootAPI;
 //esta es raiz porque solo hay una barra
@Path("/")
public class BeeterRootAPIResource {
	@GET
	public BeeterRootAPI getRootAPI() {
		BeeterRootAPI api = new BeeterRootAPI();
		return api;
	}
}