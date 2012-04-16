/*
APJP, A PHP/JAVA PROXY
Copyright (C) 2009-2011 Jeroen Van Steirteghem

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
*/

package APJP.HTTPS;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import APJP.APJP;
import APJP.Logger;

public class HTTPSProxyServer implements Runnable
{
	private static Logger logger;
	private Thread thread;
	private ServerSocket serverSocket;
	private List<HTTPSProxyServerWorker> httpsProxyServerWorkers;
	private List<HTTPSServer> httpsServers;
	
	static
	{
		logger = Logger.getLogger(APJP.APJP_LOCAL_HTTPS_PROXY_SERVER_LOGGER_ID);
	}
	
	public HTTPSProxyServer()
	{
		httpsProxyServerWorkers = Collections.synchronizedList(new LinkedList<HTTPSProxyServerWorker>());
		httpsServers = Collections.synchronizedList(new LinkedList<HTTPSServer>());
	}
	
	public String getLocalAddress()
	{
		return APJP.APJP_LOCAL_HTTPS_PROXY_SERVER_ADDRESS;
	}
	
	public int getLocalPort()
	{
		return APJP.APJP_LOCAL_HTTPS_PROXY_SERVER_PORT;
	}
	
	public synchronized void start() throws HTTPSProxyServerException 
	{
		logger.log(2, "HTTPS_PROXY_SERVER/START");
		
		try
		{
			startHTTPSProxyServer();
			startHTTPSProxyServerWorkers();
			startHTTPSServers();
		}
		catch(Exception e)
		{
			logger.log(2, "HTTPS_PROXY_SERVER/START: EXCEPTION", e);
			
			throw new HTTPSProxyServerException("HTTPS_PROXY_SERVER/START", e);
		}
	}
	
	public synchronized void stop() throws HTTPSProxyServerException
	{
		logger.log(2, "HTTPS_PROXY_SERVER/STOP");
		
		try
		{
			stopHTTPSProxyServer();
			stopHTTPSProxyServerWorkers();
			stopHTTPSServers();
		}
		catch(Exception e)
		{
			logger.log(2, "HTTPS_PROXY_SERVER/STOP: EXCEPTION", e);
			
			throw new HTTPSProxyServerException("HTTPS_PROXY_SERVER/STOP", e);
		}
	}
	
	protected synchronized void startHTTPSProxyServerWorker(HTTPSProxyServerWorker httpsProxyServerWorker) throws HTTPSProxyServerException
	{
		logger.log(2, "HTTPS_PROXY_SERVER/START_HTTPS_PROXY_SERVER_WORKER");
		
		try
		{
			httpsProxyServerWorker.start();
			
			httpsProxyServerWorkers.add(httpsProxyServerWorker);
		}
		catch(Exception e)
		{
			logger.log(2, "HTTPS_PROXY_SERVER/START_HTTPS_PROXY_SERVER_WORKER: EXCEPTION", e);
			
			throw new HTTPSProxyServerException("HTTPS_PROXY_SERVER/START_HTTPS_PROXY_SERVER_WORKER", e);
		}
	}
	
	protected synchronized void stopHTTPSProxyServerWorker(HTTPSProxyServerWorker httpsProxyServerWorker) throws HTTPSProxyServerException
	{
		logger.log(2, "HTTPS_PROXY_SERVER/STOP_HTTPS_PROXY_SERVER_WORKER");
		
		try
		{
			httpsProxyServerWorker.stop();
			
			httpsProxyServerWorkers.remove(httpsProxyServerWorker);
			
			HTTPSServer httpsServer = httpsProxyServerWorker.getHTTPSServer();
			
			if(httpsServer != null)
			{
				String remoteAddress = httpsServer.getRemoteAddress();
				int remotePort = httpsServer.getRemotePort();
				
				HTTPSServer httpsServer2 = null;
				
				for(HTTPSProxyServerWorker httpsProxyServerWorker2: httpsProxyServerWorkers)
				{
					HTTPSServer httpsServer3 = httpsProxyServerWorker2.getHTTPSServer();
					
					if(httpsServer3 != null)
					{
						String remoteAddress3 = httpsServer3.getRemoteAddress();
						int remotePort3 = httpsServer3.getRemotePort();
						
						if(remoteAddress.equalsIgnoreCase(remoteAddress3))
						{
							if(remotePort == remotePort3)
							{
								httpsServer2 = httpsServer3;
								
								break;
							}
						}
					}
				}
				
				if(httpsServer2 == null)
				{
					stopHTTPSServer(httpsServer);
					
					httpsServers.remove(httpsServer);
				}
			}
		}
		catch(Exception e)
		{
			logger.log(2, "HTTPS_PROXY_SERVER/STOP_HTTPS_PROXY_SERVER_WORKER: EXCEPTION", e);
			
			throw new HTTPSProxyServerException("HTTPS_PROXY_SERVER/STOP_HTTPS_PROXY_SERVER_WORKER", e);
		}
	}
	
	protected synchronized void startHTTPSServer(HTTPSServer httpsServer) throws HTTPSProxyServerException
	{
		logger.log(2, "HTTPS_PROXY_SERVER/START_HTTPS_SERVER");
		
		try
		{
			httpsServer.start();
		}
		catch(Exception e)
		{
			logger.log(2, "HTTPS_PROXY_SERVER/START_HTTPS_SERVER: EXCEPTION", e);
			
			throw new HTTPSProxyServerException("HTTPS_PROXY_SERVER/START_HTTPS_SERVER", e);
		}
	}
	
	protected synchronized void stopHTTPSServer(HTTPSServer httpsServer) throws HTTPSProxyServerException
	{
		logger.log(2, "HTTPS_PROXY_SERVER/STOP_HTTPS_SERVER");
		
		try
		{
			httpsServer.stop();
		}
		catch(Exception e)
		{
			logger.log(2, "HTTPS_PROXY_SERVER/STOP_HTTPS_SERVER: EXCEPTION", e);
			
			throw new HTTPSProxyServerException("HTTPS_PROXY_SERVER/STOP_HTTPS_SERVER", e);
		}
	}
	
	protected synchronized void startHTTPSProxyServer() throws HTTPSProxyServerException 
	{
		logger.log(2, "HTTPS_PROXY_SERVER/START_HTTPS_PROXY_SERVER");
		
		try
		{
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(APJP.APJP_LOCAL_HTTPS_PROXY_SERVER_ADDRESS, APJP.APJP_LOCAL_HTTPS_PROXY_SERVER_PORT));
			
			thread = new Thread(this);
			thread.start();
		}
		catch(Exception e)
		{
			logger.log(2, "HTTPS_PROXY_SERVER/START_HTTPS_PROXY_SERVER: EXCEPTION", e);
			
			throw new HTTPSProxyServerException("HTTPS_PROXY_SERVER/START_HTTPS_PROXY_SERVER", e);
		}
	}
	
	protected synchronized void stopHTTPSProxyServer() throws HTTPSProxyServerException
	{
		logger.log(2, "HTTPS_PROXY_SERVER/STOP_HTTPS_PROXY_SERVER");
		
		try
		{
			thread = null;
			
			try
			{
				Socket outputSocket = new Socket();
				outputSocket.connect(new InetSocketAddress(APJP.APJP_LOCAL_HTTPS_PROXY_SERVER_ADDRESS, APJP.APJP_LOCAL_HTTPS_PROXY_SERVER_PORT));
				outputSocket.close();
			}
			catch(Exception e)
			{
				
			}
			
			try
			{
				serverSocket.close();
			}
			catch(Exception e)
			{
				
			}
		}
		catch(Exception e)
		{
			logger.log(2, "HTTPS_PROXY_SERVER/STOP_HTTPS_PROXY_SERVER: EXCEPTION", e);
			
			throw new HTTPSProxyServerException("HTTPS_PROXY_SERVER/STOP_HTTPS_PROXY_SERVER", e);
		}
	}
	
	protected synchronized void startHTTPSProxyServerWorkers() throws HTTPSProxyServerException
	{
		logger.log(2, "HTTPS_PROXY_SERVER/START_HTTPS_PROXY_SERVER_WORKERS");
	}
	
	protected synchronized void stopHTTPSProxyServerWorkers() throws HTTPSProxyServerException
	{
		logger.log(2, "HTTPS_PROXY_SERVER/STOP_HTTPS_PROXY_SERVER_WORKERS");
		
		try
		{
			for(HTTPSProxyServerWorker httpsProxyServerWorker2: httpsProxyServerWorkers)
			{
				httpsProxyServerWorker2.stop();
			}
			
			httpsProxyServerWorkers.removeAll(httpsProxyServerWorkers);
		}
		catch(Exception e)
		{
			logger.log(2, "HTTPS_PROXY_SERVER/STOP_HTTPS_PROXY_SERVER_WORKERS: EXCEPTION", e);
			
			throw new HTTPSProxyServerException("HTTPS_PROXY_SERVER/STOP_HTTPS_PROXY_SERVER_WORKERS", e);
		}
	}
	
	protected synchronized void startHTTPSServers() throws HTTPSProxyServerException
	{
		logger.log(2, "HTTPS_PROXY_SERVER/START_HTTPS_SERVERS");
	}
	
	protected synchronized void stopHTTPSServers() throws HTTPSProxyServerException
	{
		logger.log(2, "HTTPS_PROXY_SERVER/STOP_HTTPS_SERVERS");
		
		try
		{
			for(HTTPSServer httpsServer2: httpsServers)
			{
				stopHTTPSServer(httpsServer2);
			}
			
			httpsServers.removeAll(httpsServers);
		}
		catch(Exception e)
		{
			logger.log(2, "HTTPS_PROXY_SERVER/STOP_HTTPS_SERVERS: EXCEPTION", e);
			
			throw new HTTPSProxyServerException("HTTPS_PROXY_SERVER/STOP_HTTPS_SERVERS", e);
		}
	}
	
	protected synchronized HTTPSServer getHTTPSServer(String remoteAddress, int remotePort) throws HTTPSProxyServerException
	{
		logger.log(2, "HTTPS_PROXY_SERVER/GET_HTTPS_SERVER");
		
		try
		{
			HTTPSServer httpsServer1 = null;
			
			for(HTTPSServer httpsServer2: httpsServers)
			{
				String remoteAddress2 = httpsServer2.getRemoteAddress();
				int remotePort2 = httpsServer2.getRemotePort();
				
				if(remoteAddress2.equalsIgnoreCase(remoteAddress))
				{
					if(remotePort2 == remotePort)
					{
						httpsServer1 = httpsServer2;
						
						break;
					}
				}
			}
			
			if(httpsServer1 == null)
			{	
				httpsServer1 = new HTTPSServer(remoteAddress, remotePort);
				
				startHTTPSServer(httpsServer1);
				
				httpsServers.add(httpsServer1);
			}
			
			return httpsServer1;
		}
		catch(Exception e)
		{
			logger.log(2, "HTTPS_PROXY_SERVER/GET_HTTPS_SERVER: EXCEPTION", e);
			
			throw new HTTPSProxyServerException("HTTPS_PROXY_SERVER/GET_HTTPS_SERVER", e);
		}
	}
	
	public void run()
	{
		while(thread != null)
		{
			try
			{
				Socket inputSocket = serverSocket.accept();
				
				if(thread != null)
				{
					HTTPSProxyServerWorker httpsProxyServerWorker = new HTTPSProxyServerWorker(this, inputSocket);
					
					startHTTPSProxyServerWorker(httpsProxyServerWorker);
				}
				else
				{
					inputSocket.close();
				}
			}
			catch(Exception e)
			{
				if(thread != null)
				{
					logger.log(2, "HTTPS_PROXY_SERVER: EXCEPTION", e);
				}
			}
		}
	}
}
