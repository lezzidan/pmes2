package es.bsc.pmes.managers.infractructureHelpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.bsc.conn.exceptions.ConnException;
import es.bsc.conn.mesos.Mesos;
import es.bsc.conn.types.HardwareDescription;
import es.bsc.conn.types.SoftwareDescription;
import es.bsc.conn.types.VirtualResource;
import es.bsc.pmes.types.JobDefinition;

/**
 * MESOS connector helper.
 * 
 * Extends the Infrastructure helper class. This class is used within the
 * Infrastructure Manager class.
 */
public class MESOSHelper extends InfrastructureHelper {

	// Messos connector client
	private Mesos conn_client;

	/* Main logger */
	private static final Logger logger = LogManager.getLogger(MESOSHelper.class);

	/* Constructor */
	public MESOSHelper(String workspace, ArrayList<String> commands, ArrayList<String> auth_keys) {
		super(workspace, commands, auth_keys);
	}

	/**
	 * ************************************************************************
	 * OVERRIDING METHODS
	 * ************************************************************************
	 */

	@Override
	public HashMap<String, String> configureResource(JobDefinition jobDef, Map<String, String> configuration) {
		// TODO: Add Mesos stuff
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public VirtualResource createResource(HardwareDescription hd, SoftwareDescription sd, Map<String, String> prop,
			Map<String, String> configuration) throws ConnException {
		// TODO: Add Mesos stuff
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void destroyResource(String Id) {
		this.conn_client.destroy(Id);
		// TODO: test if destroy is done correctly
		// TODO: Add Mesos stuff
	}

	/**
	 * ************************************************************************
	 * AUXILIARY METHODS
	 * ************************************************************************
	 */

}
